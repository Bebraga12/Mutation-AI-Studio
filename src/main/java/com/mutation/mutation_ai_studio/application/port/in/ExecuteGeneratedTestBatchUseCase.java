package com.mutation.mutation_ai_studio.application.port.in;

import com.mutation.mutation_ai_studio.domain.model.GeneratedTestBatch;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestExecutionResult;

import java.nio.file.Path;
import java.util.List;

public interface ExecuteGeneratedTestBatchUseCase {

    List<GeneratedTestExecutionResult> execute(Path projectRoot, GeneratedTestBatch batch);
}
