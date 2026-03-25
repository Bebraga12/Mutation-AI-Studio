package com.mutation.mutation_ai_studio.adapters.in.cli;

import com.mutation.mutation_ai_studio.application.port.in.ScanProjectUseCase;
import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScanCliAdapter implements ApplicationRunner {

    private static final String SCAN_COMMAND = "scan";

    private final ScanProjectUseCase scanProjectUseCase;

    public ScanCliAdapter(ScanProjectUseCase scanProjectUseCase) {
        this.scanProjectUseCase = scanProjectUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> commands = args.getNonOptionArgs();
        if (commands.isEmpty() || !SCAN_COMMAND.equals(commands.getFirst())) {
            return;
        }

        Path projectRoot = resolveProjectRoot(commands);
        List<JavaClassCandidate> classes = scanProjectUseCase.scan(projectRoot);
        printScanResult(projectRoot, classes);
    }

    private void printScanResult(Path projectRoot, List<JavaClassCandidate> classes) {
        System.out.printf("🔎 Scan: %s%n", projectRoot);

        if (classes.isEmpty()) {
            System.out.println("📦 Nenhuma classe Java encontrada em src/main/java.");
            return;
        }

        System.out.printf("📦 %d classes encontradas%n%n", classes.size());

        Map<String, List<JavaClassCandidate>> byDirectory = classes.stream()
                .sorted(Comparator.comparing(JavaClassCandidate::relativePath))
                .collect(LinkedHashMap::new,
                        (map, candidate) -> map.computeIfAbsent(parentDirectory(candidate.relativePath()), k -> new java.util.ArrayList<>()).add(candidate),
                        LinkedHashMap::putAll);

        byDirectory.forEach((directory, candidates) -> {
            System.out.println(directory);
            candidates.forEach(candidate -> System.out.printf("  - %-45s (%s)%n",
                    fileName(candidate.relativePath()),
                    candidate.className()));
            System.out.println();
        });
    }

    private String parentDirectory(String relativePath) {
        int lastSlash = relativePath.lastIndexOf('/');
        if (lastSlash < 0) {
            return ".";
        }

        return relativePath.substring(0, lastSlash);
    }

    private String fileName(String relativePath) {
        int lastSlash = relativePath.lastIndexOf('/');
        if (lastSlash < 0) {
            return relativePath;
        }

        return relativePath.substring(lastSlash + 1);
    }

    private Path resolveProjectRoot(List<String> commands) {
        if (commands.size() < 2) {
            return Paths.get("").toAbsolutePath().normalize();
        }

        return Paths.get(commands.get(1)).toAbsolutePath().normalize();
    }
}
