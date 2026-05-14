package com.mutation.mutation_ai_studio.domain.model;

import java.util.List;

public record TestExecutionFeedback(
        boolean passed,
        int exitCode,
        List<String> errors,
        String output
) {
}
