package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.application.port.in.ReadSelectionStatusUseCase;
import com.mutation.mutation_ai_studio.application.port.out.SelectionRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.SelectionSnapshot;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class ReadSelectionStatusService implements ReadSelectionStatusUseCase {

    private final SelectionRepositoryPort selectionRepositoryPort;

    public ReadSelectionStatusService(SelectionRepositoryPort selectionRepositoryPort) {
        this.selectionRepositoryPort = selectionRepositoryPort;
    }

    @Override
    public Optional<SelectionSnapshot> read(Path projectRoot) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("projectRoot não pode ser nulo");
        }

        if (!Files.exists(projectRoot) || !Files.isDirectory(projectRoot)) {
            throw new IllegalArgumentException("Caminho de projeto inválido: " + projectRoot);
        }

        return selectionRepositoryPort.read(projectRoot);
    }
}
