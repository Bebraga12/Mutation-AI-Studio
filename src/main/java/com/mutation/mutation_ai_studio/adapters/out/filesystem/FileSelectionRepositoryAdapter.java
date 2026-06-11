package com.mutation.mutation_ai_studio.adapters.out.filesystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mutation.mutation_ai_studio.application.port.out.SelectionRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.SelectionSnapshot;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class FileSelectionRepositoryAdapter implements SelectionRepositoryPort {

    private static final String MUTATION_DIR = ".mutation-ai";
    private static final String SELECTION_FILE = "selection.json";

    private final ObjectMapper objectMapper;

    public FileSelectionRepositoryAdapter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void save(Path projectRoot, SelectionSnapshot selectionSnapshot) {
        Path selectionFilePath = selectionFilePath(projectRoot);

        try {
            Files.createDirectories(selectionFilePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(selectionFilePath.toFile(), selectionSnapshot);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao salvar seleção em: " + selectionFilePath, e);
        }
    }

    @Override
    public Optional<SelectionSnapshot> read(Path projectRoot) {
        Path selectionFilePath = selectionFilePath(projectRoot);
        if (!Files.exists(selectionFilePath)) {
            return Optional.empty();
        }

        try {
            SelectionSnapshot snapshot = objectMapper.readValue(selectionFilePath.toFile(), SelectionSnapshot.class);
            return Optional.of(snapshot);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler seleção em: " + selectionFilePath, e);
        }
    }

    @Override
    public Path selectionFilePath(Path projectRoot) {
        return projectRoot.resolve(MUTATION_DIR).resolve(SELECTION_FILE).normalize();
    }
}
