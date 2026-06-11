package com.mutation.mutation_ai_studio.application.port.in;

import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;

import java.nio.file.Path;
import java.util.List;

public interface ScanProjectUseCase {

    List<JavaClassCandidate> scan(Path projectRoot);
}
