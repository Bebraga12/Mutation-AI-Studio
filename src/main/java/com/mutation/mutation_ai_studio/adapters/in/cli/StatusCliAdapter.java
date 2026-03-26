package com.mutation.mutation_ai_studio.adapters.in.cli;

import com.mutation.mutation_ai_studio.application.port.in.ReadSelectionStatusUseCase;
import com.mutation.mutation_ai_studio.application.port.out.SelectionRepositoryPort;
import com.mutation.mutation_ai_studio.domain.model.SelectionSnapshot;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Component
public class StatusCliAdapter implements ApplicationRunner {

    private static final String STATUS_COMMAND = "status";
    private static final String GREEN = "[32m";
    private static final String RESET = "[0m";

    private final ReadSelectionStatusUseCase readSelectionStatusUseCase;
    private final SelectionRepositoryPort selectionRepositoryPort;

    public StatusCliAdapter(ReadSelectionStatusUseCase readSelectionStatusUseCase,
                            SelectionRepositoryPort selectionRepositoryPort) {
        this.readSelectionStatusUseCase = readSelectionStatusUseCase;
        this.selectionRepositoryPort = selectionRepositoryPort;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> commands = args.getNonOptionArgs();
        if (commands.isEmpty() || !STATUS_COMMAND.equals(commands.getFirst())) {
            return;
        }

        Path projectRoot = resolveProjectRoot(commands);
        Optional<SelectionSnapshot> maybeSelection = readSelectionStatusUseCase.read(projectRoot);

        System.out.printf("Projeto: %s%n%n", projectRoot);

        if (maybeSelection.isEmpty()) {
            System.out.println("Nenhuma seleção encontrada.");
            System.out.println("Use `mutation-ai select .` para selecionar classes primeiro.");
            return;
        }

        SelectionSnapshot selection = maybeSelection.get();
        System.out.printf("Classes selecionadas (%d):%n%n", selection.totalSelected());

        selection.classes().forEach(candidate ->
                System.out.printf("%s+ %s%s%n", GREEN, candidate.className(), RESET));

        System.out.printf("%nArquivo de seleção: %s%n", selectionRepositoryPort.selectionFilePath(projectRoot));
    }

    private Path resolveProjectRoot(List<String> commands) {
        if (commands.size() < 2 || ".".equals(commands.get(1))) {
            return Paths.get("").toAbsolutePath().normalize();
        }

        return Paths.get(commands.get(1)).toAbsolutePath().normalize();
    }
}
