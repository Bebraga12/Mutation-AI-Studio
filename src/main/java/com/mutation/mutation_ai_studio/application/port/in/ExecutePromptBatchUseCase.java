package com.mutation.mutation_ai_studio.application.port.in;

import com.mutation.mutation_ai_studio.domain.model.PromptExecutionResult;
import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;

import java.nio.file.Path;
import java.util.List;

public interface ExecutePromptBatchUseCase {

    List<PromptExecutionResult> execute(Path projectRoot, TestPromptBatch batch);
}
