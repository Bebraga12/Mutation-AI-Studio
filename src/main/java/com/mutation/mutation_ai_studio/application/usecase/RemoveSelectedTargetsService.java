package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.application.port.in.RemoveSelectedTargetsUseCase;
import com.mutation.mutation_ai_studio.application.port.out.SelectionRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;
import com.mutation.mutation_ai_studio.domain.model.SelectionSnapshot;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class RemoveSelectedTargetsService implements RemoveSelectedTargetsUseCase {

    private final SelectionRepositoryPort selectionRepositoryPort;

    public RemoveSelectedTargetsService(SelectionRepositoryPort selectionRepositoryPort) {
        this.selectionRepositoryPort = selectionRepositoryPort;
    }

    @Override
    public Optional<SelectionSnapshot> remove(Path projectRoot, String target) {
        validateProjectRoot(projectRoot);

        Optional<SelectionSnapshot> maybeSelection = selectionRepositoryPort.read(projectRoot);
        if (maybeSelection.isEmpty()) {
            return Optional.empty();
        }

        SelectionSnapshot current = maybeSelection.get();
        String normalizedTarget = target.toLowerCase(Locale.ROOT);

        List<JavaClassCandidate> remaining = current.classes().stream()
                .filter(candidate -> !matches(candidate, normalizedTarget))
                .toList();

        SelectionSnapshot updated = new SelectionSnapshot(
                current.projectRoot(),
                Instant.now(),
                remaining.size(),
                remaining
        );

        selectionRepositoryPort.save(projectRoot, updated);
        return Optional.of(updated);
    }

    @Override
    public Optional<SelectionSnapshot> clear(Path projectRoot) {
        validateProjectRoot(projectRoot);

        Optional<SelectionSnapshot> maybeSelection = selectionRepositoryPort.read(projectRoot);
        if (maybeSelection.isEmpty()) {
            return Optional.empty();
        }

        SelectionSnapshot current = maybeSelection.get();
        SelectionSnapshot cleared = new SelectionSnapshot(
                current.projectRoot(),
                Instant.now(),
                0,
                List.of()
        );

        selectionRepositoryPort.save(projectRoot, cleared);
        return Optional.of(cleared);
    }

    private boolean matches(JavaClassCandidate candidate, String normalizedTarget) {
        return candidate.className().toLowerCase(Locale.ROOT).equals(normalizedTarget)
                || candidate.fullyQualifiedName().toLowerCase(Locale.ROOT).equals(normalizedTarget);
    }

    private void validateProjectRoot(Path projectRoot) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("projectRoot não pode ser nulo");
        }

        if (!Files.exists(projectRoot) || !Files.isDirectory(projectRoot)) {
            throw new IllegalArgumentException("Caminho de projeto inválido: " + projectRoot);
        }
    }
}
