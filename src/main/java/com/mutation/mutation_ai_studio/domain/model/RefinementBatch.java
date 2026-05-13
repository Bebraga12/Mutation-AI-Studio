package com.mutation.mutation_ai_studio.domain.model;

import java.time.Instant;
import java.util.List;

public record RefinementBatch(
        String projectRoot,
        Instant createdAt,
        List<RefinementResult> results
) {
}
