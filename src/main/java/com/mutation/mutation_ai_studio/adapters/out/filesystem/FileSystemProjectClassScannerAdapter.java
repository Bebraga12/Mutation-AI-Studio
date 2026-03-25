package com.mutation.mutation_ai_studio.adapters.out.filesystem;

import com.mutation.mutation_ai_studio.application.port.out.ProjectClassScannerPort;
import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class FileSystemProjectClassScannerAdapter implements ProjectClassScannerPort {

    private static final Path MAIN_JAVA_PATH = Paths.get("src", "main", "java");

    @Override
    public List<JavaClassCandidate> findClasses(Path projectRoot) {
        Path scanRoot = projectRoot.resolve(MAIN_JAVA_PATH).normalize();

        if (!Files.exists(scanRoot) || !Files.isDirectory(scanRoot)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(scanRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isJavaSource)
                    .map(path -> toCandidate(scanRoot, path))
                    .sorted(Comparator.comparing(JavaClassCandidate::fullyQualifiedName))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao escanear classes em: " + scanRoot, e);
        }
    }

    private boolean isJavaSource(Path path) {
        String fileName = path.getFileName().toString();
        if (!fileName.endsWith(".java")) {
            return false;
        }

        if (fileName.endsWith("Test.java")) {
            return false;
        }

        return !fileName.equals("package-info.java") && !fileName.equals("module-info.java");
    }

    private JavaClassCandidate toCandidate(Path scanRoot, Path absoluteClassPath) {
        Path relative = scanRoot.relativize(absoluteClassPath);

        String relativePath = relative.toString().replace('\\', '/');
        String className = removeJavaSuffix(absoluteClassPath.getFileName().toString());

        String packageName = relative.getParent() == null
                ? ""
                : relative.getParent().toString()
                .replace('/', '.')
                .replace('\\', '.');

        String fullyQualifiedName = packageName.isBlank()
                ? className
                : packageName + "." + className;

        return new JavaClassCandidate(className, packageName, fullyQualifiedName, relativePath);
    }

    private String removeJavaSuffix(String fileName) {
        return fileName.substring(0, fileName.length() - ".java".length());
    }
}
