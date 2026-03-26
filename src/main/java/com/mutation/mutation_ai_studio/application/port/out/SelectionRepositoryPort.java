package com.mutation.mutation_ai_studio.application.port.out;

import com.mutation.mutation_ai_studio.domain.model.SelectionSnapshot;

import java.nio.file.Path;
import java.util.Optional;

public interface SelectionRepositoryPort {

    void save(Path projectRoot, SelectionSnapshot selectionSnapshot);

    Optional<SelectionSnapshot> read(Path projectRoot);

    Path selectionFilePath(Path projectRoot);
}
