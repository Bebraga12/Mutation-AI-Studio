package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.application.port.in.CreateTestPromptUseCase;
import com.mutation.mutation_ai_studio.application.port.out.SelectionRepositoryPort;
import com.mutation.mutation_ai_studio.application.port.out.SourceCodeAnalyzerPort;
import com.mutation.mutation_ai_studio.domain.model.ClassAnalysis;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;
import com.mutation.mutation_ai_studio.domain.model.SelectionSnapshot;
import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class CreateTestPromptService implements CreateTestPromptUseCase {

    private final SelectionRepositoryPort selectionRepositoryPort;
    private final SourceCodeAnalyzerPort sourceCodeAnalyzerPort;

    public CreateTestPromptService(SelectionRepositoryPort selectionRepositoryPort,
                                   SourceCodeAnalyzerPort sourceCodeAnalyzerPort) {
        this.selectionRepositoryPort = selectionRepositoryPort;
        this.sourceCodeAnalyzerPort = sourceCodeAnalyzerPort;
    }

    @Override
    public TestPromptBatch create(Path projectRoot) {
        SelectionSnapshot selection = selectionRepositoryPort.read(projectRoot)
                .orElseThrow(() -> new IllegalStateException("Nenhuma seleção encontrada para o projeto. Use `mutation-ai select .` antes de criar o prompt."));

        List<ClassTestPrompt> prompts = selection.classes().stream()
                .map(candidate -> toPrompt(projectRoot, candidate))
                .toList();

        return new TestPromptBatch(
                projectRoot.toString(),
                Instant.now(),
                selection.totalSelected(),
                prompts
        );
    }

    private ClassTestPrompt toPrompt(Path projectRoot, JavaClassCandidate candidate) {
        Path sourceFile = projectRoot.resolve("src/main/java").resolve(candidate.relativePath()).normalize();
        String sourceCode = sanitizeSourceCode(readSourceCode(sourceFile));
        ClassAnalysis analysis = sourceCodeAnalyzerPort.analyze(projectRoot, candidate, sourceCode);
        List<String> dependencies = combineDependencies(analysis);
        String prompt = buildPrompt(candidate, sourceCode, dependencies, analysis);

        return new ClassTestPrompt(
                candidate.className(),
                candidate.fullyQualifiedName(),
                candidate.relativePath(),
                dependencies,
                analysis,
                sourceCode,
                prompt,
                null
        );
    }

    private String readSourceCode(Path sourceFile) {
        try {
            return Files.readString(sourceFile);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler código fonte da classe alvo: " + sourceFile, e);
        }
    }

    private List<String> combineDependencies(ClassAnalysis analysis) {
        LinkedHashSet<String> dependencies = new LinkedHashSet<>();
        dependencies.addAll(analysis.constructorDependencies());
        dependencies.addAll(analysis.fieldDependencies());
        return List.copyOf(dependencies);
    }

    private String sanitizeSourceCode(String sourceCode) {
        String normalized = sourceCode.replace("\r\n", "\n").trim();
        StringBuilder builder = new StringBuilder();
        boolean inBlockComment = false;

        for (String line : normalized.split("\n", -1)) {
            String current = line;
            if (inBlockComment) {
                int endIndex = current.indexOf("*/");
                if (endIndex < 0) {
                    continue;
                }
                current = current.substring(endIndex + 2);
                inBlockComment = false;
            }

            while (true) {
                int blockStart = current.indexOf("/*");
                int lineCommentStart = current.indexOf("//");

                if (blockStart >= 0 && (lineCommentStart < 0 || blockStart < lineCommentStart)) {
                    int blockEnd = current.indexOf("*/", blockStart + 2);
                    if (blockEnd >= 0) {
                        current = current.substring(0, blockStart) + current.substring(blockEnd + 2);
                        continue;
                    }

                    current = current.substring(0, blockStart);
                    inBlockComment = true;
                } else if (lineCommentStart >= 0) {
                    current = current.substring(0, lineCommentStart);
                }
                break;
            }

            String trimmed = current.stripTrailing();
            if (trimmed.isBlank()) {
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
                    builder.append('\n');
                }
                continue;
            }

            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(trimmed);
        }

        return builder.toString().trim();
    }

    private String buildPrompt(JavaClassCandidate candidate, String sourceCode, List<String> dependencies, ClassAnalysis analysis) {
        String dependencyBlock = dependencies.isEmpty()
                ? "- nenhuma dependência identificada"
                : dependencies.stream()
                .map(dependency -> "- " + dependency)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("- nenhuma dependência identificada");

        String publicMethodsBlock = analysis.publicMethods().isEmpty()
                ? "- nenhum método público identificado"
                : analysis.publicMethods().stream()
                .map(method -> "- " + method.methodName() + " : " + method.returnType() + "(" + String.join(", ", method.parameters()) + ")")
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("- nenhum método público identificado");

        String importedTypesBlock = analysis.importedTypes().isEmpty()
                ? "- nenhum import explícito identificado"
                : analysis.importedTypes().stream()
                .map(importType -> "- " + importType)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("- nenhum import explícito identificado");

        String fieldDependenciesBlock = analysis.fieldDependencies().isEmpty()
                ? "- nenhum field de dependência identificado"
                : analysis.fieldDependencies().stream()
                .map(field -> "- " + field)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("- nenhum field de dependência identificado");

        String analysisPlanBlock = buildAnalysisPlanBlock(candidate, analysis, dependencies);

        return "Você é um gerador de testes unitários Java." + System.lineSeparator()
                + System.lineSeparator()
                + "Tarefa:" + System.lineSeparator()
                + "Gere um único arquivo de teste unitário completo para a classe informada abaixo." + System.lineSeparator()
                + System.lineSeparator()
                + "Plano explícito antes da geração:" + System.lineSeparator()
                + analysisPlanBlock + System.lineSeparator()
                + System.lineSeparator()
                + "Regras obrigatórias de saída:" + System.lineSeparator()
                + "- responda com apenas código Java puro" + System.lineSeparator()
                + "- não inclua texto fora do código" + System.lineSeparator()
                + "- não inclua markdown" + System.lineSeparator()
                + "- não inclua blocos de código" + System.lineSeparator()
                + "- não inclua ```java, ``` ou qualquer outro delimitador de código" + System.lineSeparator()
                + "- não inclua explicações" + System.lineSeparator()
                + "- não inclua comentários explicando o código gerado" + System.lineSeparator()
                + "- retorne um único arquivo Java completo e compilável" + System.lineSeparator()
                + "- o arquivo deve incluir package, imports, declaração da classe de teste e métodos de teste completos" + System.lineSeparator()
                + System.lineSeparator()
                + "Restrições de implementação:" + System.lineSeparator()
                + "- use JUnit 5" + System.lineSeparator()
                + "- use Mockito" + System.lineSeparator()
                + "- use explicitamente @Mock e @InjectMocks quando fizer sentido" + System.lineSeparator()
                + "- use preferencialmente @ExtendWith(MockitoExtension.class) quando fizer sentido" + System.lineSeparator()
                + "- prefira testes unitários puros" + System.lineSeparator()
                + "- não suba contexto Spring sem necessidade estrita" + System.lineSeparator()
                + "- o nome da classe de teste deve ser " + candidate.className() + "Test" + System.lineSeparator()
                + "- mantenha no teste o mesmo package da classe alvo" + System.lineSeparator()
                + "- gere código compatível com src/test/java" + System.lineSeparator()
                + "- se houver dependências colaboradoras, use @Mock nelas quando fizer sentido" + System.lineSeparator()
                + "- use @InjectMocks na classe sob teste quando fizer sentido" + System.lineSeparator()
                + "- use apenas tipos, imports e dependências que realmente existam no código fornecido" + System.lineSeparator()
                + "- todo tipo usado no teste deve ter import explícito quando não estiver no mesmo package ou em java.lang" + System.lineSeparator()
                + "- se usar AutorRepository, LivroRepository, Autor, Livro, Login, Usuario, AuthenticationManager, Optional, JwtServiceGenerator ou qualquer outro tipo, inclua o import correto" + System.lineSeparator()
                + "- não invente nomes de repositório, serviço, DTO, entidade ou collaborator" + System.lineSeparator()
                + "- se uma dependência não estiver evidente, prefira usar exatamente os fields e imports listados na análise estrutural" + System.lineSeparator()
                + "- package obrigatório no arquivo de teste: " + candidate.packageName() + System.lineSeparator()
                + "- quando a classe alvo depender de um colaborador com retorno Optional, use Optional.of(...) ou Optional.empty() no mock, nunca o valor cru" + System.lineSeparator()
                + System.lineSeparator()
                + "Objetivo dos testes:" + System.lineSeparator()
                + "- cobrir comportamento observável" + System.lineSeparator()
                + "- cobrir caminho feliz" + System.lineSeparator()
                + "- cobrir falhas relevantes" + System.lineSeparator()
                + "- cobrir bordas importantes" + System.lineSeparator()
                + "- cobrir retornos nulos quando houver" + System.lineSeparator()
                + "- cobrir cenários onde dependências retornam null" + System.lineSeparator()
                + "- cobrir Optional vazio quando aplicável" + System.lineSeparator()
                + "- cobrir exceções relevantes" + System.lineSeparator()
                + "- verificar interações com mocks quando isso fizer parte do comportamento observável" + System.lineSeparator()
                + "- usar nomes de testes descritivos e legíveis" + System.lineSeparator()
                + "- evitar testes frágeis" + System.lineSeparator()
                + "- evitar mocks desnecessários" + System.lineSeparator()
                + "- não testar detalhes internos irrelevantes" + System.lineSeparator()
                + System.lineSeparator()
                + "Contexto do alvo:" + System.lineSeparator()
                + "- fullyQualifiedName: " + candidate.fullyQualifiedName() + System.lineSeparator()
                + "- sourceFile: src/main/java/" + candidate.relativePath() + System.lineSeparator()
                + System.lineSeparator()
                + "Dependências identificadas (prioridade para dependências de construtor):" + System.lineSeparator()
                + dependencyBlock + System.lineSeparator()
                + System.lineSeparator()
                + "Análise estrutural da classe:" + System.lineSeparator()
                + "- package: " + analysis.packageName() + System.lineSeparator()
                + "- construtor principal: " + analysis.constructorSignature() + System.lineSeparator()
                + "- usa Optional: " + analysis.usesOptional() + System.lineSeparator()
                + "- usa exceções: " + analysis.usesExceptions() + System.lineSeparator()
                + "- imports reais:" + System.lineSeparator()
                + importedTypesBlock + System.lineSeparator()
                + "- fields de dependência reais:" + System.lineSeparator()
                + fieldDependenciesBlock + System.lineSeparator()
                + "- métodos públicos:" + System.lineSeparator()
                + publicMethodsBlock + System.lineSeparator()
                + System.lineSeparator()
                + "Use rigorosamente os nomes acima para mocks, imports e collaborators." + System.lineSeparator()
                + "Antes de finalizar, confira se todos os tipos usados no teste foram importados explicitamente." + System.lineSeparator()
                + System.lineSeparator()
                + "Código fonte da classe alvo:" + System.lineSeparator()
                + sourceCode;
    }

    private String buildAnalysisPlanBlock(JavaClassCandidate candidate, ClassAnalysis analysis, List<String> dependencies) {
        StringBuilder builder = new StringBuilder();
        builder.append("- classe alvo: ").append(candidate.fullyQualifiedName()).append(System.lineSeparator());
        builder.append("- package alvo: ").append(analysis.packageName()).append(System.lineSeparator());
        builder.append("- construtor principal: ").append(analysis.constructorSignature()).append(System.lineSeparator());
        builder.append("- dependências de construtor: ").append(analysis.constructorDependencies()).append(System.lineSeparator());
        builder.append("- dependências de field: ").append(analysis.fieldDependencies()).append(System.lineSeparator());
        builder.append("- dependências consolidadas: ").append(dependencies).append(System.lineSeparator());
        builder.append("- métodos públicos a cobrir: ").append(analysis.publicMethods().stream()
                .map(method -> method.methodName())
                .toList()).append(System.lineSeparator());
        builder.append("- usa Optional: ").append(analysis.usesOptional()).append(System.lineSeparator());
        builder.append("- usa exceções: ").append(analysis.usesExceptions()).append(System.lineSeparator());
        builder.append("- imports reais disponíveis: ").append(analysis.importedTypes());
        return builder.toString();
    }
}
