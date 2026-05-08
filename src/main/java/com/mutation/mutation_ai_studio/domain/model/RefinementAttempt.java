package com.mutation.mutation_ai_studio.domain.model;

import java.nio.file.Path;
import java.util.List;

public record RefinementAttempt(
        int attemptNumber,
        List<String> rejectionReasons,
        Path correctionPromptPath,
        Path generatedPath,
        boolean approved,
        Path approvedPath,
        List<String> finalReasons
) {
}
