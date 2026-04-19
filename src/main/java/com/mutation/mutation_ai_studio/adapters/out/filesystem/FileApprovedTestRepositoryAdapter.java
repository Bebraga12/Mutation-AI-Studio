package com.mutation.mutation_ai_studio.adapters.out.filesystem;

import com.mutation.mutation_ai_studio.application.port.out.ApprovedTestRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.ValidatedTestResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class FileApprovedTestRepositoryAdapter implements ApprovedTestRepositoryPort {

    private static final String MUTATION_DIR = ".mutation-ai";
    private static final String APPROVED_DIR = "approved";
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    @Override
    public Path save(Path projectRoot, ValidatedTestResult result, Instant createdAt) {
        Path approvedDirectory = projectRoot.resolve(MUTATION_DIR).resolve(APPROVED_DIR).normalize();
        String batchFolderName = "create-test-" + FILE_NAME_FORMATTER.format(createdAt);
        Path batchDirectory = approvedDirectory.resolve(batchFolderName);
        Path approvedFile = batchDirectory.resolve(result.generatedTestClassName() + ".java");

        try {
            Files.createDirectories(batchDirectory);
            Files.writeString(approvedFile, result.sanitizedCode());
            return approvedFile;
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao salvar teste aprovado em: " + approvedFile, e);
        }
    }
}
