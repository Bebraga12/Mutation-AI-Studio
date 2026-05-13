package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.application.port.in.CreateTestPromptUseCase;
import com.mutation.mutation_ai_studio.application.port.out.SelectionRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;
import com.mutation.mutation_ai_studio.domain.model.SelectionSnapshot;
import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CreateTestPromptService implements CreateTestPromptUseCase {

    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile("(?s)\\b%s\\s*\\((.*?)\\)\\s*\\{");
    private static final Pattern FIELD_PATTERN = Pattern.compile("(?m)^\\s*private\\s+(?!static\\b)(?!final\\s+[A-Z0-9_]+\\b)(?:final\\s+)?([\\w$.<>?,\\[\\]]+)\\s+(\\w+)\\s*(?:=.*)?;");
    private static final Pattern BLOCK_COMMENT_PATTERN = Pattern.compile("(?s)/\\*.*?\\*/");
    private static final Pattern LINE_COMMENT_PATTERN = Pattern.compile("(?m)^\\s*//.*$");

    private final SelectionRepositoryPort selectionRepositoryPort;

    public CreateTestPromptService(SelectionRepositoryPort selectionRepositoryPort) {
        this.selectionRepositoryPort = selectionRepositoryPort;
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
        List<String> dependencies = extractDependencies(candidate.className(), sourceCode);
        String prompt = buildPrompt(candidate, sourceCode, dependencies);

        return new ClassTestPrompt(
                candidate.className(),
                candidate.fullyQualifiedName(),
                candidate.relativePath(),
                dependencies,
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

    private List<String> extractDependencies(String className, String sourceCode) {
        LinkedHashSet<String> dependencies = new LinkedHashSet<>();
        dependencies.addAll(extractConstructorDependencies(className, sourceCode));
        dependencies.addAll(extractFieldDependencies(sourceCode));
        return List.copyOf(dependencies);
    }

    private List<String> extractConstructorDependencies(String className, String sourceCode) {
        Pattern pattern = Pattern.compile(CONSTRUCTOR_PATTERN.pattern().formatted(Pattern.quote(className)));
        Matcher matcher = pattern.matcher(sourceCode);
        if (!matcher.find()) {
            return List.of();
        }

        String rawParameters = matcher.group(1).trim();
        if (rawParameters.isBlank()) {
            return List.of();
        }

        List<String> dependencies = new ArrayList<>();
        for (String parameter : splitParameters(rawParameters)) {
            String cleaned = parameter.replaceAll("@[\\w$.]+(?:\\([^)]*\\))?\\s*", "").trim();
            if (cleaned.isBlank()) {
                continue;
            }
            dependencies.add(cleaned.replaceAll("\\s+", " "));
        }
        return dependencies;
    }

    private List<String> extractFieldDependencies(String sourceCode) {
        Matcher matcher = FIELD_PATTERN.matcher(sourceCode);
        List<String> dependencies = new ArrayList<>();
        while (matcher.find()) {
            String type = matcher.group(1).trim();
            String name = matcher.group(2).trim();
            if (looksLikeConstant(name)) {
                continue;
            }
            dependencies.add(type + " " + name);
        }
        return dependencies;
    }

    private boolean looksLikeConstant(String fieldName) {
        return fieldName.equals(fieldName.toUpperCase());
    }

    private List<String> splitParameters(String rawParameters) {
        List<String> parameters = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int genericDepth = 0;

        for (int i = 0; i < rawParameters.length(); i++) {
            char currentChar = rawParameters.charAt(i);
            if (currentChar == '<') {
                genericDepth++;
            } else if (currentChar == '>') {
                genericDepth--;
            }

            if (currentChar == ',' && genericDepth == 0) {
                parameters.add(current.toString().trim());
                current.setLength(0);
                continue;
            }

            current.append(currentChar);
        }

        if (!current.isEmpty()) {
            parameters.add(current.toString().trim());
        }

        return parameters;
    }

    private String sanitizeSourceCode(String sourceCode) {
        String withoutBlockComments = BLOCK_COMMENT_PATTERN.matcher(sourceCode).replaceAll("");
        String withoutLineComments = LINE_COMMENT_PATTERN.matcher(withoutBlockComments).replaceAll("");

        return Arrays.stream(withoutLineComments.split("\\R", -1))
                .map(String::stripTrailing)
                .dropWhile(String::isBlank)
                .reduce(new StringBuilder(), (builder, line) -> {
                    if (!builder.isEmpty()) {
                        builder.append(System.lineSeparator());
                    }
                    builder.append(line);
                    return builder;
                }, StringBuilder::append)
                .toString()
                .replaceAll("(" + Pattern.quote(System.lineSeparator()) + "){3,}", System.lineSeparator() + System.lineSeparator())
                .trim();
    }

    private String buildPrompt(JavaClassCandidate candidate, String sourceCode, List<String> dependencies) {
        String dependencyBlock = dependencies.isEmpty()
                ? "- nenhuma dependência identificada"
                : dependencies.stream()
                .map(dependency -> "- " + dependency)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("- nenhuma dependência identificada");

        return "Você é um gerador de testes unitários Java." + System.lineSeparator()
                + System.lineSeparator()
                + "Tarefa:" + System.lineSeparator()
                + "Gere um único arquivo de teste unitário completo para a classe informada abaixo." + System.lineSeparator()
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
                + "- inclua todos os imports necessários para o arquivo compilar no contexto do projeto" + System.lineSeparator()
                + "- inclua todos os static imports necessários para assertions do JUnit e utilitários do Mockito quando usados" + System.lineSeparator()
                + "- não use classes sem import correspondente, exceto classes do mesmo package ou de java.lang" + System.lineSeparator()
                + System.lineSeparator()
                + "Restrições de implementação:" + System.lineSeparator()
                + "- use JUnit 5" + System.lineSeparator()
                + "- use Mockito" + System.lineSeparator()
                + "- se usar assertEquals, assertThrows, assertNotNull ou assertNull, inclua os static imports apropriados" + System.lineSeparator()
                + "- se usar verify, when, doThrow, any, eq ou outros utilitários do Mockito, inclua os static imports apropriados" + System.lineSeparator()
                + "- use explicitamente @Mock e @InjectMocks quando fizer sentido" + System.lineSeparator()
                + "- use preferencialmente @ExtendWith(MockitoExtension.class) quando fizer sentido" + System.lineSeparator()
                + "- prefira testes unitários puros" + System.lineSeparator()
                + "- não suba contexto Spring sem necessidade estrita" + System.lineSeparator()
                + "- o nome da classe de teste deve ser " + candidate.className() + "Test" + System.lineSeparator()
                + "- mantenha no teste o mesmo package da classe alvo" + System.lineSeparator()
                + "- gere código compatível com src/test/java" + System.lineSeparator()
                + "- se houver dependências colaboradoras, use @Mock nelas quando fizer sentido" + System.lineSeparator()
                + "- use @InjectMocks na classe sob teste quando fizer sentido" + System.lineSeparator()
                + System.lineSeparator()
                + "Objetivo dos testes:" + System.lineSeparator()
                + "- cobrir comportamento observável" + System.lineSeparator()
                + "- cobrir caminho feliz" + System.lineSeparator()
                + "- cobrir falhas relevantes" + System.lineSeparator()
                + "- cobrir bordas importantes" + System.lineSeparator()
                + "- use assertNull apenas quando o cenário de null estiver explicitamente refletido no fluxo da implementação real" + System.lineSeparator()
                + "- nunca use assertNull em fluxos com Optional.get(), orElseThrow() ou acesso posterior a objeto ausente" + System.lineSeparator()
                + "- cubra cenários onde dependências retornam null somente quando isso estiver explicitamente refletido no fluxo real" + System.lineSeparator()
                + "- cubra Optional vazio quando aplicável" + System.lineSeparator()
                + "- se o fluxo real indicar exceção em cenário not found, use assertThrows" + System.lineSeparator()
                + "- cubra exceções relevantes" + System.lineSeparator()
                + "- para métodos void, não capture retorno; valide comportamento com verify(...) e, quando fizer sentido, assertThrows(...)" + System.lineSeparator()
                + "- não use assertEquals, assertNull ou assertNotNull para validar retorno de métodos void" + System.lineSeparator()
                + "- verifique interações com mocks quando isso fizer parte do comportamento observável" + System.lineSeparator()
                + "- quando o método delegar para repository, gateway, client ou outros serviços, valide as interações relevantes com verify(...)" + System.lineSeparator()
                + "- evite assertNotNull isolado quando houver interações relevantes ou resultado determinístico que possa ser validado de forma mais forte" + System.lineSeparator()
                + "- quando houver valor determinístico no resultado, prefira assertEquals com o valor esperado" + System.lineSeparator()
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
                + "Código fonte da classe alvo:" + System.lineSeparator()
                + sourceCode;
    }
}
