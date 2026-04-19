package com.mutation.mutation_ai_studio.adapters.out.filesystem;

import com.mutation.mutation_ai_studio.application.port.out.GeneratedTestRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class FileGeneratedTestRepositoryAdapter implements GeneratedTestRepositoryPort {

    private static final String MUTATION_DIR = ".mutation-ai";
    private static final String GENERATED_DIR = "generated";
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    @Override
    public Path save(Path projectRoot, GeneratedTestResult result, Instant createdAt) {
        Path generatedDirectory = projectRoot.resolve(MUTATION_DIR).resolve(GENERATED_DIR).normalize();
        String batchFolderName = "create-test-" + FILE_NAME_FORMATTER.format(createdAt);
        Path batchDirectory = generatedDirectory.resolve(batchFolderName);
        Path generatedFile = batchDirectory.resolve(result.generatedTestClassName() + ".java");

        try {
            Files.createDirectories(batchDirectory);
            Files.writeString(generatedFile, result.sanitizedCode());
            return generatedFile;
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao salvar teste gerado em: " + generatedFile, e);
        }
    }
}
