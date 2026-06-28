package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;

import java.util.List;

final class GeneratedTestFallbackFactory {

    private static final List<String> COMMON_IMPORTS = List.of(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.extension.ExtendWith",
            "org.mockito.InjectMocks",
            "org.mockito.junit.jupiter.MockitoExtension"
    );

    private GeneratedTestFallbackFactory() {
    }

    static String generate(ClassTestPrompt prompt) {
        if (prompt.dependencies().isEmpty()) {
            return generatePlain(prompt);
        }
        return generateMockito(prompt);
    }

    /**
     * Fallback para classes COM dependências: smoke test com Mockito (@InjectMocks).
     */
    private static String generateMockito(ClassTestPrompt prompt) {
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(prompt.analysis().packageName()).append(";\n\n");

        for (String importName : prompt.analysis().importedTypes()) {
            if (importName == null || importName.isBlank() || isProductionSideImport(importName)) {
                continue;
            }
            builder.append("import ").append(importName).append(";\n");
        }

        for (String importName : COMMON_IMPORTS) {
            builder.append("import ").append(importName).append(";\n");
        }

        builder.append("import org.mockito.Mock;\n");

        builder.append("\nimport static org.junit.jupiter.api.Assertions.assertNotNull;\n");
        builder.append("import static org.mockito.Mockito.*;\n\n");

        builder.append("@ExtendWith(MockitoExtension.class)\n");
        builder.append("public class ").append(prompt.className()).append("Test {\n\n");

        for (String dep : prompt.dependencies()) {
            builder.append("    @Mock\n");
            builder.append("    private ").append(dep).append(";\n\n");
        }

        builder.append("    @InjectMocks\n");
        builder.append("    private ").append(prompt.className()).append(" subject;\n\n");

        builder.append("    @Test\n");
        builder.append("    void deveInstanciarSubjeto() {\n");
        builder.append("        assertNotNull(subject);\n");
        builder.append("    }\n");
        builder.append("}\n");

        return builder.toString();
    }

    /**
     * Fallback para classes SEM dependências (POJOs/entities/exceptions): instancia o alvo
     * diretamente, sem Mockito. Para exceptions usa o construtor com mensagem; caso contrário
     * tenta o construtor sem argumentos.
     */
    private static String generatePlain(ClassTestPrompt prompt) {
        String className = prompt.className();
        boolean isException = className.endsWith("Exception")
                || prompt.sourceCode().contains("extends RuntimeException")
                || prompt.sourceCode().contains("extends Exception");
        boolean hasNoArgConstructor = prompt.sourceCode().contains("public " + className + "()");

        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(prompt.analysis().packageName()).append(";\n\n");

        for (String importName : prompt.analysis().importedTypes()) {
            if (importName == null || importName.isBlank() || isProductionSideImport(importName)) {
                continue;
            }
            builder.append("import ").append(importName).append(";\n");
        }

        builder.append("import org.junit.jupiter.api.Test;\n");
        builder.append("\nimport static org.junit.jupiter.api.Assertions.assertNotNull;\n\n");

        builder.append("public class ").append(className).append("Test {\n\n");
        builder.append("    @Test\n");
        builder.append("    void deveInstanciarSubjeto() {\n");
        if (isException) {
            builder.append("        ").append(className).append(" subject = new ").append(className).append("(\"mensagem de teste\");\n");
        } else if (hasNoArgConstructor || !prompt.sourceCode().contains("public " + className + "(")) {
            // Sem construtor explícito (Java fornece o padrão) ou com no-arg explícito.
            builder.append("        ").append(className).append(" subject = new ").append(className).append("();\n");
        } else {
            // Última tentativa: no-arg mesmo assim (melhor esforço para o smoke test).
            builder.append("        ").append(className).append(" subject = new ").append(className).append("();\n");
        }
        builder.append("        assertNotNull(subject);\n");
        builder.append("    }\n");
        builder.append("}\n");

        return builder.toString();
    }

    private static boolean isProductionSideImport(String qualifiedName) {
        return qualifiedName.startsWith("org.springframework.beans.factory.annotation.")
                || qualifiedName.startsWith("org.springframework.stereotype.")
                || qualifiedName.startsWith("org.springframework.web.bind.annotation.")
                || qualifiedName.startsWith("jakarta.persistence.")
                || qualifiedName.startsWith("javax.persistence.");
    }
}
