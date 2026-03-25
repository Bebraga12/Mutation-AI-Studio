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
    private final ScanOutputFormatter outputFormatter;

    public ScanCliAdapter(ScanProjectUseCase scanProjectUseCase) {
        this.scanProjectUseCase = scanProjectUseCase;
        this.outputFormatter = new ScanOutputFormatter();
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> commands = args.getNonOptionArgs();
        if (commands.isEmpty() || !SCAN_COMMAND.equals(commands.getFirst())) {
            return;
        }

        String[] sourceArgs = args.getSourceArgs();
        Path projectRoot = resolveProjectRoot(sourceArgs);
        List<JavaClassCandidate> classes = scanProjectUseCase.scan(projectRoot);

        boolean verbose = hasVerbose(sourceArgs);
        boolean focusTestable = hasFocusTestable(sourceArgs);

        outputFormatter.print(projectRoot, classes, verbose, focusTestable);
    }

    private boolean hasVerbose(String[] sourceArgs) {
        for (String arg : sourceArgs) {
            if ("--verbose".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFocusTestable(String[] sourceArgs) {
        for (int i = 0; i < sourceArgs.length; i++) {
            String current = sourceArgs[i];
            if (current.startsWith("--focus=")) {
                String value = current.substring("--focus=".length());
                if ("testable".equalsIgnoreCase(value)) {
                    return true;
                }
            }

            if ("--focus".equalsIgnoreCase(current) && i + 1 < sourceArgs.length) {
                if ("testable".equalsIgnoreCase(sourceArgs[i + 1])) {
                    return true;
                }
            }
        }
        return false;
    }

    private Path resolveProjectRoot(String[] sourceArgs) {
        for (int i = 1; i < sourceArgs.length; i++) {
            String current = sourceArgs[i];

            if (current.startsWith("--")) {
                if ("--focus".equals(current) && i + 1 < sourceArgs.length) {
                    i++;
                }
                continue;
            }

            return Paths.get(current).toAbsolutePath().normalize();
        }

        return Paths.get("").toAbsolutePath().normalize();
    }
}
