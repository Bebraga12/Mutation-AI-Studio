package com.mutation.mutation_ai_studio.adapters.out.filesystem;

import com.mutation.mutation_ai_studio.application.port.out.GeneratedTestRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestCandidate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
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
    public Path save(Path projectRoot, GeneratedTestCandidate candidate, Instant createdAt) {
        return save(projectRoot, candidate, createdAt, GENERATED_DIR);
    }

    @Override
    public Path saveFailed(Path projectRoot, GeneratedTestCandidate candidate, Instant createdAt) {
        return save(projectRoot, candidate, createdAt, GENERATED_DIR + "/failed");
    }

    private Path save(Path projectRoot, GeneratedTestCandidate candidate, Instant createdAt, String directoryName) {
        Path generatedDirectory = projectRoot.resolve(MUTATION_DIR).resolve(directoryName).normalize();
        String batchFolderName = "create-test-" + FILE_NAME_FORMATTER.format(createdAt);
        Path batchDirectory = generatedDirectory.resolve(batchFolderName);
        String fileName = sanitizeFileName(candidate.testClassName()) + ".java";
        Path generatedFile = batchDirectory.resolve(fileName);

        try {
            Files.createDirectories(batchDirectory);
            Files.writeString(generatedFile, candidate.sourceCode());
            return generatedFile;
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao salvar teste gerado em: " + generatedFile, e);
        }
    }

    private String sanitizeFileName(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.replaceAll("[^a-zA-Z0-9._-]", "-");
    }
}
