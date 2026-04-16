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
import java.util.Set;

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
    public SelectionSnapshot selectByCategory(Path projectRoot, String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Categoria alvo não pode ser vazia.");
        }

        List<JavaClassCandidate> classes = scanProjectUseCase.scan(projectRoot);
        String normalizedCategory = category.trim().toLowerCase(Locale.ROOT);

        Set<String> matchedSuffixes = switch (normalizedCategory) {
            case "service" -> Set.of("service");
            case "controller" -> Set.of("controller");
            case "repository" -> Set.of("repository");
            case "entity" -> Set.of("entity");
            case "dto" -> Set.of("dto");
            case "config" -> Set.of("config");
            case "security" -> Set.of("security");
            case "core" -> Set.of("usecase", "port", "domain", "core");
            case "other" -> Set.of();
            default -> throw new IllegalArgumentException("Categoria inválida: " + category);
        };

        List<JavaClassCandidate> selected = classes.stream()
                .filter(candidate -> matchesCategory(candidate, normalizedCategory, matchedSuffixes))
                .toList();

        return saveSelection(projectRoot, selected);
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

    private boolean matchesCategory(JavaClassCandidate candidate, String normalizedCategory, Set<String> matchedSuffixes) {
        String className = candidate.className().toLowerCase(Locale.ROOT);
        String packageName = candidate.packageName() == null ? "" : candidate.packageName().toLowerCase(Locale.ROOT);

        if ("other".equals(normalizedCategory)) {
            return matchedKnownCategory(packageName, className).isEmpty();
        }

        return matchedSuffixes.stream().anyMatch(token -> className.endsWith(token) || packageName.contains("." + token) || packageName.endsWith(token));
    }

    private Set<String> matchedKnownCategory(String packageName, String className) {
        if (className.endsWith("service") || packageName.contains(".service") || packageName.endsWith("service")) {
            return Set.of("service");
        }
        if (className.endsWith("controller") || packageName.contains(".controller") || packageName.endsWith("controller")) {
            return Set.of("controller");
        }
        if (className.endsWith("repository") || packageName.contains(".repository") || packageName.endsWith("repository")) {
            return Set.of("repository");
        }
        if (className.endsWith("entity") || packageName.contains(".entity") || packageName.endsWith("entity")) {
            return Set.of("entity");
        }
        if (className.endsWith("dto") || packageName.contains(".dto") || packageName.endsWith("dto")) {
            return Set.of("dto");
        }
        if (className.endsWith("config") || packageName.contains(".config") || packageName.endsWith("config")) {
            return Set.of("config");
        }
        if (className.endsWith("security") || packageName.contains(".security") || packageName.endsWith("security")) {
            return Set.of("security");
        }
        if (packageName.contains(".domain") || packageName.contains(".usecase") || packageName.contains(".port") || packageName.contains(".core") || packageName.endsWith("domain") || packageName.endsWith("usecase") || packageName.endsWith("port") || packageName.endsWith("core")) {
            return Set.of("core");
        }
        return Set.of();
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
