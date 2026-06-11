package com.mutation.mutation_ai_studio.application.port.in;

import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;

import java.nio.file.Path;

public interface CreateTestPromptUseCase {

    TestPromptBatch create(Path projectRoot);
}
