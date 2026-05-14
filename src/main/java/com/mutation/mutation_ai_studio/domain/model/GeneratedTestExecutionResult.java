package com.mutation.mutation_ai_studio.domain.model;

import java.nio.file.Path;

public record GeneratedTestExecutionResult(
        GeneratedTestCandidate candidate,
        Path workspacePath,
        Path preservedPath,
        TestExecutionFeedback feedback
) {
}
