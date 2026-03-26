package com.mutation.mutation_ai_studio.application.port.in;

import com.mutation.mutation_ai_studio.domain.model.SelectionSnapshot;

import java.nio.file.Path;
import java.util.Optional;

public interface RemoveSelectedTargetsUseCase {

    Optional<SelectionSnapshot> remove(Path projectRoot, String target);

    Optional<SelectionSnapshot> clear(Path projectRoot);
}
