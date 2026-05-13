package com.mutation.mutation_ai_studio.domain.model;

import java.util.List;

public record RefinementResult(
        ValidatedTestResult finalResult,
        int attemptsUsed,
        List<RefinementAttempt> attempts,
        boolean refined
) {
}
