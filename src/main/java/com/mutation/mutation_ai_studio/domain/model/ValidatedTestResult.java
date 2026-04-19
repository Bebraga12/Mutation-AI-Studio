package com.mutation.mutation_ai_studio.domain.model;

import java.nio.file.Path;
import java.util.List;

public record ValidatedTestResult(
        String className,
        String fullyQualifiedName,
        String generatedTestClassName,
        boolean approved,
        List<String> reasons,
        String sanitizedCode,
        Path generatedPath,
        Path approvedPath
) {
}
