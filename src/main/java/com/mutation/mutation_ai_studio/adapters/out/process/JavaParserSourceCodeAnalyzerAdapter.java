package com.mutation.mutation_ai_studio.adapters.out.process;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.mutation.mutation_ai_studio.application.port.out.SourceCodeAnalyzerPort;
import com.mutation.mutation_ai_studio.domain.model.ClassAnalysis;
import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;
import com.mutation.mutation_ai_studio.domain.model.MethodAnalysis;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JavaParserSourceCodeAnalyzerAdapter implements SourceCodeAnalyzerPort {

    @Override
    public ClassAnalysis analyze(Path projectRoot, JavaClassCandidate candidate, String sourceCode) {
        try {
            return parseAndAnalyze(candidate, sourceCode);
        } catch (RuntimeException ex) {
            return new ClassAnalysis(
                    candidate.className(),
                    candidate.packageName(),
                    "nenhum construtor explícito identificado",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    sourceCode.contains("Optional<") || sourceCode.contains("Optional."),
                    sourceCode.contains("throw ") || sourceCode.contains("throws "),
                    List.of()
            );
        }
    }

    private ClassAnalysis parseAndAnalyze(JavaClassCandidate candidate, String sourceCode) {
        CompilationUnit compilationUnit = StaticJavaParser.parse(sourceCode);
        ClassOrInterfaceDeclaration declaration = compilationUnit.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new IllegalStateException("Classe não encontrada para análise: " + candidate.fullyQualifiedName()));

        ConstructorDeclaration primaryConstructor = declaration.getConstructors().stream()
                .findFirst()
                .orElse(null);

        List<String> constructorDependencies = primaryConstructor != null
                ? extractConstructorDependencies(primaryConstructor)
                : List.of();

        String constructorSignature = primaryConstructor != null
                ? primaryConstructor.getDeclarationAsString(false, false, false)
                : "nenhum construtor explícito identificado";

        List<String> fieldDependencies = declaration.getFields().stream()
                .filter(field -> !field.isStatic())
                .filter(field -> field.getVariable(0).getInitializer().isEmpty())
                .filter(field -> isInjectableType(field.getElementType().asString()))
                .map(this::formatField)
                .toList();

        List<MethodAnalysis> publicMethods = declaration.getMethods().stream()
                .filter(m -> !m.isPrivate())
                .map(this::toMethodAnalysis)
                .toList();

        List<String> explicitImports = compilationUnit.getImports().stream()
                .map(ImportDeclaration::getNameAsString)
                .toList();

        List<String> samePackageTypes = extractSamePackageTypeImports(
                compilationUnit, candidate.packageName(), explicitImports);

        List<String> importedTypes = new ArrayList<>(explicitImports);
        importedTypes.addAll(samePackageTypes);

        List<String> nonPublicMethodNames = declaration.getMethods().stream()
                .filter(MethodDeclaration::isPrivate)
                .map(MethodDeclaration::getNameAsString)
                .distinct()
                .toList();

        boolean usesOptional = sourceCode.contains("Optional<") || sourceCode.contains("Optional.");
        boolean usesExceptions = sourceCode.contains("throw ") || sourceCode.contains("throws ");

        return new ClassAnalysis(
                candidate.className(),
                candidate.packageName(),
                constructorSignature,
                constructorDependencies,
                fieldDependencies,
                publicMethods,
                importedTypes,
                usesOptional,
                usesExceptions,
                nonPublicMethodNames
        );
    }

    private List<String> extractConstructorDependencies(ConstructorDeclaration constructor) {
        return constructor.getParameters().stream()
                .filter(parameter -> isInjectableType(parameter.getType().asString()))
                .map(parameter -> parameter.getType().asString() + " " + parameter.getNameAsString())
                .toList();
    }

    private static final Set<String> NON_INJECTABLE_RAW_TYPES = Set.of(
            "byte", "short", "int", "long", "float", "double", "boolean", "char",
            "Byte", "Short", "Integer", "Long", "Float", "Double", "Boolean", "Character",
            "String", "CharSequence", "Object", "Number", "Void",
            "BigDecimal", "BigInteger",
            "LocalDate", "LocalDateTime", "LocalTime", "Instant", "Date", "Duration", "Period", "UUID",
            "List", "ArrayList", "LinkedList", "Set", "HashSet", "LinkedHashSet", "TreeSet",
            "Map", "HashMap", "LinkedHashMap", "TreeMap", "ConcurrentHashMap",
            "Collection", "Optional", "Stream",
            "AtomicLong", "AtomicInteger", "AtomicBoolean", "AtomicReference"
    );

    private boolean isInjectableType(String typeAsString) {
        String raw = typeAsString;
        int generics = raw.indexOf('<');
        if (generics >= 0) {
            raw = raw.substring(0, generics);
        }
        raw = raw.replace("[]", "").trim();
        int dot = raw.lastIndexOf('.');
        if (dot >= 0) {
            raw = raw.substring(dot + 1);
        }
        if (raw.isEmpty()) {
            return false;
        }
        return !NON_INJECTABLE_RAW_TYPES.contains(raw);
    }

    private String formatField(FieldDeclaration field) {
        return field.getElementType().asString() + " " + field.getVariable(0).getNameAsString();
    }

    private MethodAnalysis toMethodAnalysis(MethodDeclaration method) {
        List<String> parameters = method.getParameters().stream()
                .map(parameter -> parameter.getType().asString() + " " + parameter.getNameAsString())
                .toList();

        List<String> thrownExceptions = method.getThrownExceptions().stream()
                .map(Object::toString)
                .toList();

        String methodBody = method.getBody()
                .map(body -> body.toString().trim())
                .orElse("");

        return new MethodAnalysis(
                method.getNameAsString(),
                method.getType().asString(),
                parameters,
                thrownExceptions,
                methodBody
        );
    }

    private List<String> extractSamePackageTypeImports(
            CompilationUnit compilationUnit, String packageName, List<String> explicitImports) {

        Set<String> importedSimpleNames = explicitImports.stream()
                .map(fqn -> fqn.contains(".") ? fqn.substring(fqn.lastIndexOf('.') + 1) : fqn)
                .collect(Collectors.toSet());

        Set<String> builtinNames = new HashSet<>(Set.of(
                "String", "Integer", "Long", "Double", "Float", "Boolean", "Byte", "Character", "Short",
                "Object", "Number", "Comparable", "Iterable", "Cloneable", "Enum",
                "Exception", "RuntimeException", "IllegalArgumentException", "IllegalStateException",
                "NullPointerException", "UnsupportedOperationException", "IndexOutOfBoundsException",
                "ArithmeticException", "ClassCastException", "StringBuilder", "StringBuffer",
                "Math", "System", "Thread",
                "List", "ArrayList", "LinkedList", "Map", "HashMap", "LinkedHashMap", "TreeMap",
                "Set", "HashSet", "LinkedHashSet", "TreeSet", "Optional", "Stream", "Collections",
                "Arrays", "Iterator", "Collection",
                "Test", "BeforeEach", "AfterEach", "BeforeAll", "AfterAll",
                "ExtendWith", "Mock", "InjectMocks", "Spy", "Captor", "Answers",
                "MockitoExtension", "ArgumentCaptor", "MockitoAnnotations",
                "Autowired", "Service", "Component", "Repository", "Controller", "RestController",
                "Configuration", "Bean", "Qualifier", "Value",
                "Override", "Deprecated", "SuppressWarnings", "FunctionalInterface"
        ));

        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        compilationUnit.findAll(ClassOrInterfaceType.class).forEach(type -> {
            String name = type.getNameAsString();
            if (name.length() <= 1) return;
            if (!Character.isUpperCase(name.charAt(0))) return;
            if (importedSimpleNames.contains(name)) return;
            if (builtinNames.contains(name)) return;
            if (type.getScope().isPresent()) return;

            String fqn = packageName + "." + name;
            if (seen.add(fqn)) {
                result.add(fqn);
            }
        });

        return result;
    }
}
