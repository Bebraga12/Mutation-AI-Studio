package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.application.port.in.ScanProjectUseCase;
import com.mutation.mutation_ai_studio.application.port.out.ProjectClassScannerPort;
import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class ScanProjectService implements ScanProjectUseCase {

    private final ProjectClassScannerPort scannerPort;

    public ScanProjectService(ProjectClassScannerPort scannerPort) {
        this.scannerPort = scannerPort;
    }

    @Override
    public List<JavaClassCandidate> scan(Path projectRoot) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("projectRoot não pode ser nulo");
        }

        if (!Files.exists(projectRoot) || !Files.isDirectory(projectRoot)) {
            throw new IllegalArgumentException("Caminho de projeto inválido: " + projectRoot);
        }

        return scannerPort.findClasses(projectRoot);
    }
}
