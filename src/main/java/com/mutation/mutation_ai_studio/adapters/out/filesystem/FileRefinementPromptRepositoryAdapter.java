package com.mutation.mutation_ai_studio.adapters.out.filesystem;

import com.mutation.mutation_ai_studio.application.port.out.RefinementPromptRepositoryPort;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class FileRefinementPromptRepositoryAdapter implements RefinementPromptRepositoryPort {

    private static final String MUTATION_DIR = ".mutation-ai";
    private static final String REFINEMENTS_DIR = "refinements";
    private static final String PROMPTS_DIR = "prompts";
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    @Override
    public Path save(Path projectRoot, String className, int attemptNumber, String prompt, Instant createdAt) {
        Path file = batchDirectory(projectRoot, createdAt)
                .resolve(className)
                .resolve("attempt" + attemptNumber + "-correction.md");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, prompt);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao salvar prompt de refinamento em: " + file, e);
        }
    }

    private Path batchDirectory(Path projectRoot, Instant createdAt) {
        String batchFolderName = "create-test-" + FILE_NAME_FORMATTER.format(createdAt);
        return projectRoot.resolve(MUTATION_DIR)
                .resolve(REFINEMENTS_DIR)
                .resolve(batchFolderName)
                .resolve(PROMPTS_DIR)
                .normalize();
    }
}
