package com.mutation.mutation_ai_studio.adapters.in.cli;

import com.mutation.mutation_ai_studio.application.port.in.ScanProjectUseCase;
import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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

        classes.forEach(candidate -> System.out.printf(
                "%s | %s | %s%n",
                candidate.className(),
                candidate.fullyQualifiedName(),
                candidate.relativePath()
        ));
    }

    private Path resolveProjectRoot(List<String> commands) {
        if (commands.size() < 2) {
            return Paths.get("").toAbsolutePath().normalize();
        }

        return Paths.get(commands.get(1)).toAbsolutePath().normalize();
    }
}
