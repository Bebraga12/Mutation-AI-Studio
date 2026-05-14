package com.mutation.mutation_ai_studio.application.port.out;

import com.mutation.mutation_ai_studio.domain.model.ClassAnalysis;
import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;

import java.nio.file.Path;

public interface SourceCodeAnalyzerPort {

    ClassAnalysis analyze(Path projectRoot, JavaClassCandidate candidate, String sourceCode);
}
