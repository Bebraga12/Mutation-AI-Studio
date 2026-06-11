package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.application.port.in.ExecutePromptBatchUseCase;
import com.mutation.mutation_ai_studio.application.port.out.TestExecutorPort;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import com.mutation.mutation_ai_studio.domain.model.PromptExecutionResult;
import com.mutation.mutation_ai_studio.domain.model.TestExecutionFeedback;
import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExecutePromptBatchService implements ExecutePromptBatchUseCase {

    private final TestExecutorPort testExecutorPort;

    public ExecutePromptBatchService(TestExecutorPort testExecutorPort) {
        this.testExecutorPort = testExecutorPort;
    }

    @Override
    public List<PromptExecutionResult> execute(Path projectRoot, TestPromptBatch batch) {
        List<PromptExecutionResult> results = new ArrayList<>();
        for (ClassTestPrompt prompt : batch.prompts()) {
            String testClassName = prompt.className() + "Test";
            TestExecutionFeedback feedback = testExecutorPort.execute(projectRoot, testClassName);
            results.add(new PromptExecutionResult(prompt, prompt.savedPath(), feedback));
        }
        return results;
    }
}
