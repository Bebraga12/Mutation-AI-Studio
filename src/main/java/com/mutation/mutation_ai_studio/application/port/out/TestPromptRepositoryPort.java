package com.mutation.mutation_ai_studio.application.port.out;

import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;

import java.nio.file.Path;

public interface TestPromptRepositoryPort {

    Path save(Path projectRoot, ClassTestPrompt prompt);
}
