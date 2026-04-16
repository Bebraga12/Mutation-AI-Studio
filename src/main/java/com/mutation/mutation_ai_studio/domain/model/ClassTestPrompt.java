package com.mutation.mutation_ai_studio.domain.model;

import java.nio.file.Path;
import java.util.List;

public record ClassTestPrompt(
        String className,
        String fullyQualifiedName,
        String relativePath,
        List<String> dependencies,
        String sourceCode,
        String prompt,
        Path savedPath
) {
}
