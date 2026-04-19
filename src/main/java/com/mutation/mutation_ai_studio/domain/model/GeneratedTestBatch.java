package com.mutation.mutation_ai_studio.domain.model;

import java.time.Instant;
import java.util.List;

public record GeneratedTestBatch(
        String projectRoot,
        Instant createdAt,
        List<GeneratedTestResult> results
) {
}
