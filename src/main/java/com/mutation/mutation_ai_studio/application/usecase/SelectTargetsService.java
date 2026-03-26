package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.application.port.in.SelectTargetsUseCase;
import com.mutation.mutation_ai_studio.application.port.in.ScanProjectUseCase;
import com.mutation.mutation_ai_studio.application.port.out.SelectionRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;
import com.mutation.mutation_ai_studio.domain.model.SelectionSnapshot;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class SelectTargetsService implements SelectTargetsUseCase {

    private final ScanProjectUseCase scanProjectUseCase;
    private final SelectionRepositoryPort selectionRepositoryPort;

    public SelectTargetsService(ScanProjectUseCase scanProjectUseCase,
                                SelectionRepositoryPort selectionRepositoryPort) {
        this.scanProjectUseCase = scanProjectUseCase;
        this.selectionRepositoryPort = selectionRepositoryPort;
    }

    @Override
    public SelectionSnapshot selectAll(Path projectRoot) {
        List<JavaClassCandidate> classes = scanProjectUseCase.scan(projectRoot);
        return saveSelection(projectRoot, classes);
    }

    @Override
    public SelectionSnapshot selectSingle(Path projectRoot, String classNameOrFqcn) {
        if (classNameOrFqcn == null || classNameOrFqcn.isBlank()) {
            throw new IllegalArgumentException("Classe alvo não pode ser vazia.");
        }

        List<JavaClassCandidate> classes = scanProjectUseCase.scan(projectRoot);
        String target = classNameOrFqcn.trim().toLowerCase(Locale.ROOT);

        List<JavaClassCandidate> selected = classes.stream()
                .filter(candidate -> candidate.className().toLowerCase(Locale.ROOT).equals(target)
                        || candidate.fullyQualifiedName().toLowerCase(Locale.ROOT).equals(target))
                .toList();

        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Classe não encontrada no projeto: " + classNameOrFqcn);
        }

        return saveSelection(projectRoot, selected);
    }

    private SelectionSnapshot saveSelection(Path projectRoot, List<JavaClassCandidate> classes) {
        SelectionSnapshot snapshot = new SelectionSnapshot(
                projectRoot.toString(),
                Instant.now(),
                classes.size(),
                classes
        );

        selectionRepositoryPort.save(projectRoot, snapshot);
        return snapshot;
    }
}
