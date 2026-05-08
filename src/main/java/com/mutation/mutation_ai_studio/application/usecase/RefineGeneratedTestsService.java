package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.application.port.in.GenerateTestFromPromptUseCase;
import com.mutation.mutation_ai_studio.application.port.in.RefineGeneratedTestsUseCase;
import com.mutation.mutation_ai_studio.application.port.in.ValidateGeneratedTestsUseCase;
import com.mutation.mutation_ai_studio.application.port.out.RefinementPromptRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestResult;
import com.mutation.mutation_ai_studio.domain.model.RefinementAttempt;
import com.mutation.mutation_ai_studio.domain.model.RefinementBatch;
import com.mutation.mutation_ai_studio.domain.model.RefinementResult;
import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;
import com.mutation.mutation_ai_studio.domain.model.ValidatedTestBatch;
import com.mutation.mutation_ai_studio.domain.model.ValidatedTestResult;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RefineGeneratedTestsService implements RefineGeneratedTestsUseCase {

    private static final int MAX_ATTEMPTS = 3;

    private final GenerateTestFromPromptUseCase generateTestFromPromptUseCase;
    private final ValidateGeneratedTestsUseCase validateGeneratedTestsUseCase;
    private final RefinementPromptRepositoryPort refinementPromptRepositoryPort;

    public RefineGeneratedTestsService(GenerateTestFromPromptUseCase generateTestFromPromptUseCase,
                                       ValidateGeneratedTestsUseCase validateGeneratedTestsUseCase,
                                       RefinementPromptRepositoryPort refinementPromptRepositoryPort) {
        this.generateTestFromPromptUseCase = generateTestFromPromptUseCase;
        this.validateGeneratedTestsUseCase = validateGeneratedTestsUseCase;
        this.refinementPromptRepositoryPort = refinementPromptRepositoryPort;
    }

    @Override
    public RefinementBatch refine(Path projectRoot, TestPromptBatch promptBatch, ValidatedTestBatch validatedBatch) {
        Instant createdAt = validatedBatch.createdAt();
        List<RefinementResult> results = validatedBatch.results().stream()
                .map(result -> refineSingle(projectRoot, promptBatch, result, createdAt))
                .toList();

        return new RefinementBatch(projectRoot.toString(), createdAt, results);
    }

    private RefinementResult refineSingle(Path projectRoot,
                                          TestPromptBatch promptBatch,
                                          ValidatedTestResult initialResult,
                                          Instant createdAt) {
        if (initialResult.approved()) {
            return new RefinementResult(initialResult, 0, List.of(), false);
        }

        ClassTestPrompt originalPrompt = findPrompt(promptBatch, initialResult.className())
                .orElseThrow(() -> new IllegalStateException("Prompt não encontrado para refinamento: " + initialResult.className()));

        List<RefinementAttempt> attempts = new ArrayList<>();
        ValidatedTestResult current = initialResult;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS && !current.approved(); attempt++) {
            String correctionPrompt = buildCorrectionPrompt(originalPrompt, current, attempt);
            Path correctionPromptPath = refinementPromptRepositoryPort.save(projectRoot, originalPrompt.className(), attempt, correctionPrompt, createdAt);

            ClassTestPrompt refinementPrompt = new ClassTestPrompt(
                    originalPrompt.className(),
                    originalPrompt.fullyQualifiedName(),
                    originalPrompt.relativePath(),
                    originalPrompt.dependencies(),
                    originalPrompt.sourceCode(),
                    correctionPrompt,
                    correctionPromptPath
            );

            GeneratedTestResult regenerated = generateTestFromPromptUseCase.generateSingle(
                    projectRoot,
                    refinementPrompt,
                    createdAt,
                    "refinements/" + originalPrompt.className(),
                    "attempt" + attempt
            );

            ValidatedTestResult validated = validateGeneratedTestsUseCase.validateSingle(projectRoot, refinementPrompt, regenerated, createdAt);
            attempts.add(new RefinementAttempt(
                    attempt,
                    current.reasons(),
                    correctionPromptPath,
                    regenerated.savedPath(),
                    validated.approved(),
                    validated.approvedPath(),
                    validated.reasons()
            ));
            current = validated;
        }

        return new RefinementResult(current, attempts.size(), List.copyOf(attempts), true);
    }

    private Optional<ClassTestPrompt> findPrompt(TestPromptBatch promptBatch, String className) {
        return promptBatch.prompts().stream()
                .filter(prompt -> prompt.className().equals(className))
                .findFirst();
    }

    private String buildCorrectionPrompt(ClassTestPrompt prompt, ValidatedTestResult rejectedResult, int attemptNumber) {
        List<String> limitedReasons = rejectedResult.reasons().stream().limit(5).toList();

        return "Você está corrigindo um teste unitário Java que falhou na validação." + System.lineSeparator()
                + System.lineSeparator()
                + "Objetivo:" + System.lineSeparator()
                + "Corrigir o teste abaixo para que ele fique coerente com o código-fonte real, estruturalmente completo e compilável no contexto do projeto." + System.lineSeparator()
                + System.lineSeparator()
                + "Tentativa atual: " + attemptNumber + " de " + MAX_ATTEMPTS + System.lineSeparator()
                + System.lineSeparator()
                + "Regras obrigatórias de saída:" + System.lineSeparator()
                + "- responda com apenas código Java puro" + System.lineSeparator()
                + "- não inclua markdown" + System.lineSeparator()
                + "- não inclua explicações" + System.lineSeparator()
                + "- retorne um único arquivo Java completo e compilável" + System.lineSeparator()
                + "- preserve package, imports e nome da classe de teste" + System.lineSeparator()
                + System.lineSeparator()
                + "Motivos da rejeição:" + System.lineSeparator()
                + limitedReasons.stream()
                        .map(reason -> "- " + reason)
                        .reduce((left, right) -> left + System.lineSeparator() + right)
                        .orElse("- nenhum motivo informado")
                + System.lineSeparator()
                + System.lineSeparator()
                + "Instruções de correção:" + System.lineSeparator()
                + "- corrija comportamentos incompatíveis com a implementação real" + System.lineSeparator()
                + "- corrija imports ausentes e static imports ausentes" + System.lineSeparator()
                + "- não use símbolos sem import correspondente, exceto classes do mesmo package ou de java.lang" + System.lineSeparator()
                + "- se o teste não compilou, priorize corrigir estrutura do arquivo, imports e símbolos não resolvidos" + System.lineSeparator()
                + "- se o fluxo real indicar exceção, use assertThrows(...)" + System.lineSeparator()
                + "- para métodos void, não capture retorno; valide com verify(...)" + System.lineSeparator()
                + "- quando houver delegação para dependências, valide interações relevantes com verify(...)" + System.lineSeparator()
                + "- não use assertNull quando o fluxo real não retornar null explicitamente" + System.lineSeparator()
                + System.lineSeparator()
                + "Classe alvo:" + System.lineSeparator()
                + "- fullyQualifiedName: " + prompt.fullyQualifiedName() + System.lineSeparator()
                + "- sourceFile: src/main/java/" + prompt.relativePath() + System.lineSeparator()
                + System.lineSeparator()
                + "Código fonte da classe alvo:" + System.lineSeparator()
                + prompt.sourceCode() + System.lineSeparator()
                + System.lineSeparator()
                + "Teste anterior rejeitado:" + System.lineSeparator()
                + rejectedResult.sanitizedCode();
    }
}
