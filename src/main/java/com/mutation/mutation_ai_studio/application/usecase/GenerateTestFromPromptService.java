package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.application.port.in.GenerateTestFromPromptUseCase;
import com.mutation.mutation_ai_studio.application.port.out.AiTestGeneratorPort;
import com.mutation.mutation_ai_studio.application.port.out.GeneratedTestRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestBatch;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestCandidate;
import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class GenerateTestFromPromptService implements GenerateTestFromPromptUseCase {

    private final AiTestGeneratorPort aiTestGeneratorPort;
    private final GeneratedTestRepositoryPort generatedTestRepositoryPort;

    public GenerateTestFromPromptService(AiTestGeneratorPort aiTestGeneratorPort,
                                         GeneratedTestRepositoryPort generatedTestRepositoryPort) {
        this.aiTestGeneratorPort = aiTestGeneratorPort;
        this.generatedTestRepositoryPort = generatedTestRepositoryPort;
    }

    @Override
    public GeneratedTestBatch generate(Path projectRoot, TestPromptBatch batch) {
        List<GeneratedTestCandidate> candidates = new ArrayList<>();
        for (ClassTestPrompt prompt : batch.prompts()) {
            String code = aiTestGeneratorPort.generateTestCode(prompt);
            String sanitizedCode = GeneratedTestSourceNormalizer.normalize(code, prompt);
            GeneratedTestCandidate unsavedCandidate = new GeneratedTestCandidate(
                    prompt,
                    prompt.className(),
                    prompt.fullyQualifiedName(),
                    prompt.className() + "Test",
                    sanitizedCode,
                    null
            );
            Path savedPath = generatedTestRepositoryPort.save(projectRoot, unsavedCandidate, batch.createdAt());
            candidates.add(new GeneratedTestCandidate(
                    unsavedCandidate.prompt(),
                    unsavedCandidate.className(),
                    unsavedCandidate.fullyQualifiedName(),
                    unsavedCandidate.testClassName(),
                    unsavedCandidate.sourceCode(),
                    savedPath
            ));
        }

        return new GeneratedTestBatch(batch.projectRoot(), batch.createdAt(), candidates);
    }

}
