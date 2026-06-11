package com.mutation.mutation_ai_studio.application.port.in;

import com.mutation.mutation_ai_studio.domain.model.SelectionSnapshot;

import java.nio.file.Path;
import java.util.Optional;

public interface ReadSelectionStatusUseCase {

    Optional<SelectionSnapshot> read(Path projectRoot);
}
