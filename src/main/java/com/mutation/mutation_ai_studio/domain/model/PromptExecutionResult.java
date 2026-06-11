package com.mutation.mutation_ai_studio.domain.model;

import java.nio.file.Path;

public record PromptExecutionResult(
        ClassTestPrompt prompt,
        Path promptPath,
        TestExecutionFeedback feedback
) {
}
