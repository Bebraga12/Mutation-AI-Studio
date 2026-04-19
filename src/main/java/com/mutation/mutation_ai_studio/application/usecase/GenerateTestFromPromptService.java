package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.application.port.in.GenerateTestFromPromptUseCase;
import com.mutation.mutation_ai_studio.application.port.out.AiTestGeneratorPort;
import com.mutation.mutation_ai_studio.application.port.out.GeneratedTestRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestBatch;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestResult;
import com.mutation.mutation_ai_studio.domain.model.TestPromptBatch;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GenerateTestFromPromptService implements GenerateTestFromPromptUseCase {

    private static final Pattern MARKDOWN_FENCE_PATTERN = Pattern.compile("(?s)^```(?:java)?\\s*(.*?)\\s*```$");
    private static final Pattern CODE_START_PATTERN = Pattern.compile("(?s)(package\\s+[\\w.]+\\s*;|import\\s+[\\w.*]+\\s*;|public\\s+class\\s+\\w+).*?");

    private final AiTestGeneratorPort aiTestGeneratorPort;
    private final GeneratedTestRepositoryPort generatedTestRepositoryPort;

    public GenerateTestFromPromptService(AiTestGeneratorPort aiTestGeneratorPort,
                                         GeneratedTestRepositoryPort generatedTestRepositoryPort) {
        this.aiTestGeneratorPort = aiTestGeneratorPort;
        this.generatedTestRepositoryPort = generatedTestRepositoryPort;
    }

    @Override
    public GeneratedTestBatch generate(Path projectRoot, TestPromptBatch promptBatch) {
        Instant createdAt = promptBatch.createdAt();
        List<GeneratedTestResult> results = promptBatch.prompts().stream()
                .map(prompt -> generateForPrompt(projectRoot, prompt, createdAt))
                .toList();

        return new GeneratedTestBatch(projectRoot.toString(), createdAt, results);
    }

    private GeneratedTestResult generateForPrompt(Path projectRoot, ClassTestPrompt prompt, Instant createdAt) {
        String rawResponse = aiTestGeneratorPort.generate(prompt.prompt());
        String sanitizedCode = sanitize(rawResponse);
        String generatedTestClassName = prompt.className() + "Test";

        GeneratedTestResult result = new GeneratedTestResult(
                prompt.className(),
                prompt.fullyQualifiedName(),
                generatedTestClassName,
                rawResponse,
                sanitizedCode,
                null
        );

        Path savedPath = generatedTestRepositoryPort.save(projectRoot, result, createdAt);
        return new GeneratedTestResult(
                result.className(),
                result.fullyQualifiedName(),
                result.generatedTestClassName(),
                result.rawResponse(),
                result.sanitizedCode(),
                savedPath
        );
    }

    private String sanitize(String rawResponse) {
        if (rawResponse == null) {
            return "";
        }

        String sanitized = rawResponse.trim();
        Matcher markdownMatcher = MARKDOWN_FENCE_PATTERN.matcher(sanitized);
        if (markdownMatcher.matches()) {
            sanitized = markdownMatcher.group(1).trim();
        }

        Matcher codeStartMatcher = CODE_START_PATTERN.matcher(sanitized);
        if (codeStartMatcher.find()) {
            sanitized = sanitized.substring(codeStartMatcher.start()).trim();
        }

        return sanitized.trim();
    }
}
