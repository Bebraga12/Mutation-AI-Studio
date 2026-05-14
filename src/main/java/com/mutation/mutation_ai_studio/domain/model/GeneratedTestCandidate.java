package com.mutation.mutation_ai_studio.domain.model;

import java.nio.file.Path;

public record GeneratedTestCandidate(
        ClassTestPrompt prompt,
        String className,
        String fullyQualifiedName,
        String testClassName,
        String sourceCode,
        Path savedPath
) {
}
