package com.mutation.mutation_ai_studio.application.port.in;

import com.mutation.mutation_ai_studio.domain.model.RefinementBatch;
import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;
import com.mutation.mutation_ai_studio.domain.model.ValidatedTestBatch;

import java.nio.file.Path;

public interface RefineGeneratedTestsUseCase {

    RefinementBatch refine(Path projectRoot, TestPromptBatch promptBatch, ValidatedTestBatch validatedBatch);
}
