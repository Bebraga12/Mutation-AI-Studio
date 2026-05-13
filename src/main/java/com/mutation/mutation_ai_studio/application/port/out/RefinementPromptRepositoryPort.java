package com.mutation.mutation_ai_studio.application.port.out;

import java.nio.file.Path;
import java.time.Instant;

public interface RefinementPromptRepositoryPort {

    Path save(Path projectRoot, String className, int attemptNumber, String prompt, Instant createdAt);
}
