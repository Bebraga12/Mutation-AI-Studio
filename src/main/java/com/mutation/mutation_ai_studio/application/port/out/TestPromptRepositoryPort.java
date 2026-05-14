package com.mutation.mutation_ai_studio.application.port.out;

import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;

import java.nio.file.Path;
import java.time.Instant;

public interface TestPromptRepositoryPort {

    Path save(Path projectRoot, ClassTestPrompt prompt);

    Path save(Path projectRoot, ClassTestPrompt prompt, Instant createdAt);
}
