package com.mutation.mutation_ai_studio.domain.model;

import java.time.Instant;
import java.util.List;

public record ValidatedTestBatch(
        String projectRoot,
        Instant createdAt,
        List<ValidatedTestResult> results
) {
}
