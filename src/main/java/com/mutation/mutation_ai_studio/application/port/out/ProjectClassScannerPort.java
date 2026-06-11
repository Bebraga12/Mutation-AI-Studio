package com.mutation.mutation_ai_studio.application.port.out;

import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;

import java.nio.file.Path;
import java.util.List;

public interface ProjectClassScannerPort {

    List<JavaClassCandidate> findClasses(Path projectRoot);
}
