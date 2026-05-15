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
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(prompt.analysis().packageName()).append(";\n\n");

        for (String importName : prompt.analysis().importedTypes()) {
            if (importName != null && !importName.isBlank()) {
                builder.append("import ").append(importName).append(";\n");
            }
        }

        for (String importName : COMMON_IMPORTS) {
            builder.append("import ").append(importName).append(";\n");
        }

        builder.append("""

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class %sTest {

    @InjectMocks
    private %s subject;

    @Test
    void deveInstanciarSubjeto() {
        assertNotNull(subject);
    }
}
""".formatted(prompt.className(), prompt.className()));

        return builder.toString();
    }
}
