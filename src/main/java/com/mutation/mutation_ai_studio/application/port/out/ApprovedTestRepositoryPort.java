package com.mutation.mutation_ai_studio.application.port.out;

import com.mutation.mutation_ai_studio.domain.model.ValidatedTestResult;

import java.nio.file.Path;
import java.time.Instant;

public interface ApprovedTestRepositoryPort {

    Path save(Path projectRoot, ValidatedTestResult result, Instant createdAt);
}
