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
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class FileSystemProjectClassScannerAdapter implements ProjectClassScannerPort {

    private static final Path MAIN_JAVA_PATH = Paths.get("src", "main", "java");

    private static final Pattern INTERFACE_OR_ANNOTATION_TYPE = Pattern.compile(
            "(?m)^\\s*(?:public\\s+)?(?:abstract\\s+|sealed\\s+|non-sealed\\s+)*@?interface\\s+\\w"
    );

    private static final Pattern ENUM_DECLARATION = Pattern.compile(
            "(?m)^\\s*(?:public\\s+)?enum\\s+\\w"
    );

    private static final Pattern HAS_DECLARED_METHOD = Pattern.compile(
            "(?m)^[ \\t]*(?:@\\w+(?:\\([^)\\n]*\\))?[ \\t]*)?(?:public|private|protected)[ \\t]+" +
            "(?:(?:static|final|synchronized|abstract|native)[ \\t]+)*" +
            "(?!class[ \\t]|interface[ \\t]|enum[ \\t])[\\w$<>\\[\\]]+[^(;\\n]*\\("
    );

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
                    .filter(this::isUnitTestable)
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

    private boolean isUnitTestable(Path sourcePath) {
        String content;
        try {
            content = Files.readString(sourcePath);
        } catch (IOException e) {
            return true;
        }

        if (INTERFACE_OR_ANNOTATION_TYPE.matcher(content).find()) {
            return false;
        }

        if (ENUM_DECLARATION.matcher(content).find()) {
            return false;
        }

        if (content.contains("@Entity")) {
            return false;
        }

        if (content.contains("@SpringBootApplication")) {
            return false;
        }

        if (content.contains("extends JpaRepository")
                || content.contains("extends CrudRepository")
                || content.contains("extends PagingAndSortingRepository")
                || content.contains("extends MongoRepository")
                || content.contains("extends ReactiveCrudRepository")
                || content.contains("extends ReactiveMongoRepository")) {
            return false;
        }

        if (isPureLombokDataClass(content)) {
            return false;
        }

        if (content.contains("@EnableWebSecurity")) {
            return false;
        }

        return true;
    }

    private boolean isPureLombokDataClass(String content) {
        boolean hasLombokDataAnnotation = content.contains("@Getter")
                || content.contains("@Setter")
                || content.contains("@Data");
        if (!hasLombokDataAnnotation) {
            return false;
        }

        boolean hasSpringStereotype = content.contains("@Service")
                || content.contains("@Component")
                || content.contains("@Controller")
                || content.contains("@RestController")
                || content.contains("@Configuration")
                || content.contains("@Repository");
        if (hasSpringStereotype) {
            return false;
        }

        return !HAS_DECLARED_METHOD.matcher(content).find();
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
