package com.mutation.mutation_ai_studio.domain.model;

import java.nio.file.Path;

public record GeneratedTestResult(
        String className,
        String fullyQualifiedName,
        String generatedTestClassName,
        String rawResponse,
        String sanitizedCode,
        Path savedPath
) {
}
