package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.application.port.in.ValidateGeneratedTestsUseCase;
import com.mutation.mutation_ai_studio.application.port.out.ApprovedTestRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestBatch;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestResult;
import com.mutation.mutation_ai_studio.domain.model.ValidatedTestBatch;
import com.mutation.mutation_ai_studio.domain.model.ValidatedTestResult;
import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;
import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ValidateGeneratedTestsService implements ValidateGeneratedTestsUseCase {

    private static final Pattern MARKDOWN_FENCE_PATTERN = Pattern.compile("(?s)^```(?:java)?\\s*(.*?)\\s*```$");
    private static final Pattern CODE_START_PATTERN = Pattern.compile("(?s)(package\\s+[\\w.]+\\s*;|import\\s+[\\w.*]+\\s*;|public\\s+class\\s+\\w+|class\\s+\\w+).*?");
    private static final Pattern TEST_CLASS_PATTERN = Pattern.compile("\\bclass\\s+(\\w+Test)\\b");
    private static final Pattern TEST_METHOD_PATTERN = Pattern.compile("@Test");
    private static final Pattern METHOD_DECLARATION_PATTERN = Pattern.compile("(?m)\\b(?:public|protected|private)?\\s*(?:static\\s+)?[\\w$.<>\\[\\]]+\\s+(\\w+)\\s*\\(([^)]*)\\)");
    private static final Pattern RETURN_TYPE_PATTERN = Pattern.compile("(?m)\\b(?:public|protected|private)?\\s*(?:static\\s+)?([\\w$.<>\\[\\]]+)\\s+(\\w+)\\s*\\(([^)]*)\\)");
    private static final Pattern COMMENT_LINE_PATTERN = Pattern.compile("(?m)^\\s*//\\s*(arrange|act|assert|given|when|then|test|scenario).*$");
    private static final Pattern EMPTY_SETUP_PATTERN = Pattern.compile("(?s)@BeforeEach\\s+void\\s+setUp\\s*\\(\\s*\\)\\s*\\{\\s*}");
    private static final Pattern USELESS_SETUP_PATTERN = Pattern.compile("(?s)@BeforeEach\\s+void\\s+setUp\\s*\\(\\s*\\)\\s*\\{\\s*(?:super\\.setUp\\(\\);\\s*)?}");

    private final ApprovedTestRepositoryPort approvedTestRepositoryPort;

    public ValidateGeneratedTestsService(ApprovedTestRepositoryPort approvedTestRepositoryPort) {
        this.approvedTestRepositoryPort = approvedTestRepositoryPort;
    }

    @Override
    public ValidatedTestBatch validate(Path projectRoot, TestPromptBatch promptBatch, GeneratedTestBatch generatedBatch) {
        Instant createdAt = generatedBatch.createdAt();
        List<ValidatedTestResult> results = new ArrayList<>();

        for (GeneratedTestResult generated : generatedBatch.results()) {
            ClassTestPrompt prompt = findPrompt(promptBatch, generated.className())
                    .orElseThrow(() -> new IllegalStateException("Prompt não encontrado para: " + generated.className()));
            results.add(validateSingle(projectRoot, prompt, generated, createdAt));
        }

        return new ValidatedTestBatch(projectRoot.toString(), createdAt, results);
    }

    private ValidatedTestResult validateSingle(Path projectRoot,
                                               ClassTestPrompt prompt,
                                               GeneratedTestResult generated,
                                               Instant createdAt) {
        String sanitizedCode = sanitize(generated.sanitizedCode());
        List<String> reasons = new ArrayList<>();

        validateStructure(sanitizedCode, generated.generatedTestClassName(), reasons);
        if (reasons.isEmpty()) {
            validateSemanticConsistency(prompt, sanitizedCode, reasons);
        }
        if (reasons.isEmpty()) {
            validateCompilation(projectRoot, sanitizedCode, generated.generatedTestClassName(), reasons);
        }

        boolean approved = reasons.isEmpty();
        Path approvedPath = null;
        ValidatedTestResult result = new ValidatedTestResult(
                generated.className(),
                generated.fullyQualifiedName(),
                generated.generatedTestClassName(),
                approved,
                List.copyOf(reasons),
                sanitizedCode,
                generated.savedPath(),
                null
        );

        if (approved) {
            approvedPath = approvedTestRepositoryPort.save(projectRoot, result, createdAt);
        }

        return new ValidatedTestResult(
                result.className(),
                result.fullyQualifiedName(),
                result.generatedTestClassName(),
                result.approved(),
                result.reasons(),
                result.sanitizedCode(),
                result.generatedPath(),
                approvedPath
        );
    }

    private Optional<ClassTestPrompt> findPrompt(TestPromptBatch promptBatch, String className) {
        return promptBatch.prompts().stream()
                .filter(prompt -> prompt.className().equals(className))
                .findFirst();
    }

    private String sanitize(String code) {
        if (code == null) {
            return "";
        }

        String sanitized = code.trim();
        Matcher markdownMatcher = MARKDOWN_FENCE_PATTERN.matcher(sanitized);
        if (markdownMatcher.matches()) {
            sanitized = markdownMatcher.group(1).trim();
        }

        Matcher codeStartMatcher = CODE_START_PATTERN.matcher(sanitized);
        if (codeStartMatcher.find()) {
            sanitized = sanitized.substring(codeStartMatcher.start()).trim();
        }

        int lastBrace = sanitized.lastIndexOf('}');
        if (lastBrace >= 0) {
            sanitized = sanitized.substring(0, lastBrace + 1);
        }

        sanitized = COMMENT_LINE_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = EMPTY_SETUP_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = USELESS_SETUP_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = sanitized.replaceAll("(?m)^\\s*$\\R{2,}", System.lineSeparator());

        return sanitized.trim();
    }

    private void validateStructure(String code, String expectedTestClassName, List<String> reasons) {
        if (code.isBlank()) {
            reasons.add("arquivo vazio após sanitização");
            return;
        }
        if (code.contains("```")) {
            reasons.add("delimitador markdown ainda presente");
        }

        Matcher classMatcher = TEST_CLASS_PATTERN.matcher(code);
        if (!classMatcher.find()) {
            reasons.add("classe *Test não encontrada");
        } else if (!expectedTestClassName.equals(classMatcher.group(1))) {
            reasons.add("nome da classe de teste incompatível");
        }

        if (!TEST_METHOD_PATTERN.matcher(code).find()) {
            reasons.add("sem @Test");
        }

        if (!hasBalancedBraces(code)) {
            reasons.add("estrutura de chaves inválida");
        }
    }

    private void validateSemanticConsistency(ClassTestPrompt prompt, String code, List<String> reasons) {
        List<MethodSignature> sourceMethods = extractMethodSignatures(prompt.sourceCode());
        if (sourceMethods.isEmpty()) {
            return;
        }

        boolean referencesExistingMethod = sourceMethods.stream()
                .map(MethodSignature::name)
                .anyMatch(code::contains);
        if (!referencesExistingMethod) {
            reasons.add("teste não referencia método existente da classe alvo");
            return;
        }

        boolean optionalSource = sourceMethods.stream().anyMatch(signature -> signature.returnType().contains("Optional"));
        if (optionalSource && code.contains("assertNull(")) {
            reasons.add("tipo de retorno incoerente com Optional da classe alvo");
        }

        boolean voidSource = sourceMethods.stream().anyMatch(signature -> "void".equals(signature.returnType()));
        if (voidSource && (code.contains("assertEquals(") || code.contains("assertNull(") || code.contains("assertNotNull("))) {
            reasons.add("teste assume retorno incompatível com método void");
        }
    }

    private void validateCompilation(Path projectRoot, String code, String testClassName, List<String> reasons) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return;
        }

        try {
            Path tempDir = Files.createTempDirectory("mutation-ai-validate-");
            Path javaFile = tempDir.resolve(testClassName + ".java");
            Files.writeString(javaFile, code);
            int result = compiler.run(null, null, null, javaFile.toString());
            if (result != 0) {
                reasons.add("falha na compilação básica");
            }
            Files.deleteIfExists(javaFile);
            Files.deleteIfExists(tempDir);
        } catch (IOException e) {
            reasons.add("erro ao validar compilação");
        }
    }

    private boolean hasBalancedBraces(String code) {
        int depth = 0;
        for (char c : code.toCharArray()) {
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth < 0) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    private List<MethodSignature> extractMethodSignatures(String sourceCode) {
        List<MethodSignature> methods = new ArrayList<>();
        Matcher matcher = RETURN_TYPE_PATTERN.matcher(sourceCode);
        while (matcher.find()) {
            String returnType = matcher.group(1).trim();
            String methodName = matcher.group(2).trim();
            if ("if".equals(methodName) || "for".equals(methodName) || "while".equals(methodName) || "switch".equals(methodName) || methodName.equals("class")) {
                continue;
            }
            methods.add(new MethodSignature(methodName, returnType));
        }
        return methods;
    }

    private record MethodSignature(String name, String returnType) {
    }
}
