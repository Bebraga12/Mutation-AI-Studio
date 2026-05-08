package com.mutation.mutation_ai_studio.adapters.in.cli;

import com.mutation.mutation_ai_studio.adapters.out.filesystem.FileApprovedTestRepositoryAdapter;
import com.mutation.mutation_ai_studio.adapters.out.filesystem.FileTestPromptRepositoryAdapter;
import com.mutation.mutation_ai_studio.application.port.in.CreateTestPromptUseCase;
import com.mutation.mutation_ai_studio.application.port.in.GenerateTestFromPromptUseCase;
import com.mutation.mutation_ai_studio.application.port.in.RefineGeneratedTestsUseCase;
import com.mutation.mutation_ai_studio.application.port.in.ValidateGeneratedTestsUseCase;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestBatch;
import com.mutation.mutation_ai_studio.domain.model.RefinementAttempt;
import com.mutation.mutation_ai_studio.domain.model.RefinementBatch;
import com.mutation.mutation_ai_studio.domain.model.RefinementResult;
import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;
import com.mutation.mutation_ai_studio.domain.model.ValidatedTestBatch;
import com.mutation.mutation_ai_studio.domain.model.ValidatedTestResult;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class CreateTestCliAdapter implements ApplicationRunner {

    private static final String CREATE_COMMAND = "create";
    private static final String CREATE_ALIAS = "c";
    private static final String TEST_COMMAND = "test";
    private static final String TEST_ALIAS = "t";

    private final CreateTestPromptUseCase createTestPromptUseCase;
    private final GenerateTestFromPromptUseCase generateTestFromPromptUseCase;
    private final ValidateGeneratedTestsUseCase validateGeneratedTestsUseCase;
    private final RefineGeneratedTestsUseCase refineGeneratedTestsUseCase;
    private final FileTestPromptRepositoryAdapter testPromptRepository;
    private final FileApprovedTestRepositoryAdapter approvedTestRepository;

    public CreateTestCliAdapter(CreateTestPromptUseCase createTestPromptUseCase,
                                GenerateTestFromPromptUseCase generateTestFromPromptUseCase,
                                ValidateGeneratedTestsUseCase validateGeneratedTestsUseCase,
                                RefineGeneratedTestsUseCase refineGeneratedTestsUseCase,
                                FileTestPromptRepositoryAdapter testPromptRepository,
                                FileApprovedTestRepositoryAdapter approvedTestRepository) {
        this.createTestPromptUseCase = createTestPromptUseCase;
        this.generateTestFromPromptUseCase = generateTestFromPromptUseCase;
        this.validateGeneratedTestsUseCase = validateGeneratedTestsUseCase;
        this.refineGeneratedTestsUseCase = refineGeneratedTestsUseCase;
        this.testPromptRepository = testPromptRepository;
        this.approvedTestRepository = approvedTestRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] sourceArgs = args.getSourceArgs();
        if (!isCreateTestCommand(sourceArgs)) {
            return;
        }

        Path projectRoot = resolveProjectRoot(sourceArgs);
        TestPromptBatch batch = createTestPromptUseCase.create(projectRoot);
        List<ClassTestPrompt> savedPrompts = savePrompts(projectRoot, batch);
        GeneratedTestBatch generatedBatch = generateTestFromPromptUseCase.generate(projectRoot, batch);
        ValidatedTestBatch validatedBatch = validateGeneratedTestsUseCase.validate(projectRoot, batch, generatedBatch);
        RefinementBatch refinementBatch = refineGeneratedTestsUseCase.refine(projectRoot, batch, validatedBatch);

        printSummary(projectRoot, batch, savedPrompts, generatedBatch, refinementBatch);
    }

    private boolean isCreateTestCommand(String[] sourceArgs) {
        if (sourceArgs.length < 2) {
            return false;
        }

        return isCreateToken(sourceArgs[0]) && isTestToken(sourceArgs[1]);
    }

    private boolean isCreateToken(String value) {
        return CREATE_COMMAND.equals(value) || CREATE_ALIAS.equals(value);
    }

    private boolean isTestToken(String value) {
        return TEST_COMMAND.equals(value) || TEST_ALIAS.equals(value);
    }

    private Path resolveProjectRoot(String[] sourceArgs) {
        List<String> positionals = extractPositionalArgs(sourceArgs);
        if (positionals.isEmpty()) {
            return Paths.get("").toAbsolutePath().normalize();
        }

        String candidate = positionals.getFirst();
        if (".".equals(candidate)) {
            return Paths.get("").toAbsolutePath().normalize();
        }

        Path maybePath = Paths.get(candidate).toAbsolutePath().normalize();
        if (Files.exists(maybePath) && Files.isDirectory(maybePath)) {
            return maybePath;
        }

        return Paths.get("").toAbsolutePath().normalize();
    }

    private List<String> extractPositionalArgs(String[] sourceArgs) {
        List<String> positional = new ArrayList<>();
        for (int i = 2; i < sourceArgs.length; i++) {
            String current = sourceArgs[i];
            if (current.startsWith("--")) {
                continue;
            }
            positional.add(current);
        }
        return positional;
    }

    private List<ClassTestPrompt> savePrompts(Path projectRoot, TestPromptBatch batch) {
        List<ClassTestPrompt> savedPrompts = new ArrayList<>();
        for (ClassTestPrompt prompt : batch.prompts()) {
            Path savedPath = testPromptRepository.save(projectRoot, prompt, batch.createdAt());
            savedPrompts.add(new ClassTestPrompt(
                    prompt.className(),
                    prompt.fullyQualifiedName(),
                    prompt.relativePath(),
                    prompt.dependencies(),
                    prompt.sourceCode(),
                    prompt.prompt(),
                    savedPath
            ));
        }
        return savedPrompts;
    }

    private void printSummary(Path projectRoot,
                              TestPromptBatch batch,
                              List<ClassTestPrompt> savedPrompts,
                              GeneratedTestBatch generatedBatch,
                              RefinementBatch refinementBatch) {
        System.out.printf("Projeto: %s%n", projectRoot);
        System.out.printf("Classes selecionadas: %d%n", batch.totalSelected());
        System.out.printf("Prompts gerados: %d%n", savedPrompts.size());
        System.out.printf("Respostas geradas: %d%n", generatedBatch.results().size());
        long approvedCount = refinementBatch.results().stream().map(RefinementResult::finalResult).filter(ValidatedTestResult::approved).count();
        long rejectedCount = refinementBatch.results().size() - approvedCount;
        System.out.printf("Aprovados: %d%n", approvedCount);
        System.out.printf("Rejeitados: %d%n", rejectedCount);

        Path promptBatchDirectory = savedPrompts.isEmpty()
                ? projectRoot.resolve(".mutation-ai/prompts")
                : savedPrompts.getFirst().savedPath().getParent();

        Path generatedBatchDirectory = generatedBatch.results().isEmpty()
                ? projectRoot.resolve(".mutation-ai/generated")
                : generatedBatch.results().getFirst().savedPath().getParent();

        Path approvedBatchDirectory = approvedTestRepository.ensureBatchDirectory(projectRoot, refinementBatch.createdAt());

        System.out.printf("Lote de prompts: %s%n", promptBatchDirectory);
        System.out.printf("Lote de respostas: %s%n", generatedBatchDirectory);
        System.out.printf("Lote de aprovados: %s%n", approvedBatchDirectory);

        if (approvedCount == 0) {
            System.out.println("Nenhum teste foi aprovado neste lote; nada foi salvo em approved");
        } else {
            System.out.println("Aprovados:");
            refinementBatch.results().stream()
                    .map(RefinementResult::finalResult)
                    .filter(ValidatedTestResult::approved)
                    .forEach(result -> System.out.printf(" - %s -> %s%n", result.fullyQualifiedName(), result.approvedPath()));
        }

        System.out.println("Evolução por classe:");
        refinementBatch.results().forEach(this::printRefinementResult);
    }

    private void printRefinementResult(RefinementResult result) {
        ValidatedTestResult finalResult = result.finalResult();
        String status = finalResult.approved() ? "APROVADO" : "REJEITADO";
        System.out.printf(" - %s: %s após %d tentativa(s)%n", finalResult.fullyQualifiedName(), status, result.attemptsUsed());

        for (RefinementAttempt attempt : result.attempts()) {
            System.out.printf("   tentativa %d, motivos: %s%n", attempt.attemptNumber(), String.join("; ", attempt.rejectionReasons()));
            System.out.printf("   gerado: %s%n", attempt.generatedPath());
            if (attempt.approved()) {
                System.out.printf("   aprovado em: %s%n", attempt.approvedPath());
            } else {
                System.out.printf("   ainda rejeitado: %s%n", String.join("; ", attempt.finalReasons()));
            }
        }

        if (!finalResult.approved() && result.attempts().isEmpty()) {
            System.out.printf("   rejeitado sem refinamento: %s%n", String.join("; ", finalResult.reasons()));
        } else if (!finalResult.approved()) {
            System.out.printf("   rejeição final: %s%n", String.join("; ", finalResult.reasons()));
        }
    }
}
