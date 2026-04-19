package com.mutation.mutation_ai_studio.application.port.out;

import com.mutation.mutation_ai_studio.domain.model.GeneratedTestResult;

import java.nio.file.Path;
import java.time.Instant;

public interface GeneratedTestRepositoryPort {

    Path save(Path projectRoot, GeneratedTestResult result, Instant createdAt);
}
