package com.mutation.mutation_ai_studio.application.port.in;

import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestBatch;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestResult;
import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;
import com.mutation.mutation_ai_studio.domain.model.ValidatedTestBatch;
import com.mutation.mutation_ai_studio.domain.model.ValidatedTestResult;

import java.nio.file.Path;

public interface ValidateGeneratedTestsUseCase {

    ValidatedTestBatch validate(Path projectRoot, TestPromptBatch promptBatch, GeneratedTestBatch generatedBatch);

    ValidatedTestResult validateSingle(Path projectRoot,
                                       ClassTestPrompt prompt,
                                       GeneratedTestResult generated,
                                       java.time.Instant createdAt);
}
