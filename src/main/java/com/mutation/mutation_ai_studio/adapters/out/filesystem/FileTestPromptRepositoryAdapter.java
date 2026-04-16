package com.mutation.mutation_ai_studio.adapters.out.filesystem;

import com.mutation.mutation_ai_studio.application.port.out.TestPromptRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class FileTestPromptRepositoryAdapter implements TestPromptRepositoryPort {

    private static final String MUTATION_DIR = ".mutation-ai";
    private static final String PROMPTS_DIR = "prompts";
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    @Override
    public Path save(Path projectRoot, ClassTestPrompt prompt) {
        return save(projectRoot, prompt, Instant.now());
    }

    public Path save(Path projectRoot, ClassTestPrompt prompt, Instant createdAt) {
        Path promptsDirectory = projectRoot.resolve(MUTATION_DIR).resolve(PROMPTS_DIR).normalize();
        String batchFolderName = "create-test-" + FILE_NAME_FORMATTER.format(createdAt);
        Path batchDirectory = promptsDirectory.resolve(batchFolderName);
        String fileName = sanitizeFileName(prompt.className()) + ".md";
        Path promptFile = batchDirectory.resolve(fileName);

        try {
            Files.createDirectories(batchDirectory);
            Files.writeString(promptFile, prompt.prompt());
            return promptFile;
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao salvar prompt em: " + promptFile, e);
        }
    }

    private String sanitizeFileName(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.replaceAll("[^a-zA-Z0-9._-]", "-");
    }
}
