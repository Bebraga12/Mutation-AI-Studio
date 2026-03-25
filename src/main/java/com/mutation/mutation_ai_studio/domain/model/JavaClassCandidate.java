package com.mutation.mutation_ai_studio.domain.model;

public record JavaClassCandidate(
        String className,
        String packageName,
        String fullyQualifiedName,
        String relativePath
) {
}
