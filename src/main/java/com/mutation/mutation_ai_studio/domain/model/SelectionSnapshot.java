package com.mutation.mutation_ai_studio.domain.model;

import java.time.Instant;
import java.util.List;

public record SelectionSnapshot(
        String projectRoot,
        Instant selectedAt,
        int totalSelected,
        List<JavaClassCandidate> classes
) {
}
