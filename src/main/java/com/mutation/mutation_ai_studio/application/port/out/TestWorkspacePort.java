package com.mutation.mutation_ai_studio.application.port.out;

import com.mutation.mutation_ai_studio.domain.model.GeneratedTestCandidate;

import java.nio.file.Path;

public interface TestWorkspacePort {

    Path writeCandidate(Path projectRoot, GeneratedTestCandidate candidate);

    void cleanup(Path candidatePath);
}
