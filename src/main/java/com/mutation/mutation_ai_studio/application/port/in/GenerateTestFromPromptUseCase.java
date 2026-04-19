package com.mutation.mutation_ai_studio.application.port.in;

import com.mutation.mutation_ai_studio.domain.model.GeneratedTestBatch;
import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;

import java.nio.file.Path;

public interface GenerateTestFromPromptUseCase {

    GeneratedTestBatch generate(Path projectRoot, TestPromptBatch promptBatch);
}
