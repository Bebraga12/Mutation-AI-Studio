package com.mutation.mutation_ai_studio.application.usecase;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.type.ReferenceType;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

final class GeneratedTestSourceNormalizer {

    private static final List<String> COMMON_IMPORTS = List.of(
            "java.util.Optional",
            "java.util.List",
            "java.util.ArrayList",
            "java.util.Set",
            "java.util.Map",
            "java.util.HashMap",
            "java.util.Collections",
            "java.util.Arrays",
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.BeforeEach",
            "org.junit.jupiter.api.extension.ExtendWith",
            "org.mockito.Mock",
            "org.mockito.InjectMocks",
            "org.mockito.Answers",
            "org.mockito.junit.jupiter.MockitoExtension"
    );

    private static final List<String> COMMON_STATIC_IMPORTS = List.of(
            "org.junit.jupiter.api.Assertions.*",
            "org.mockito.Mockito.*",
            "org.mockito.ArgumentMatchers.*"
    );

    private GeneratedTestSourceNormalizer() {
    }

    static String normalize(String generatedCode, ClassTestPrompt prompt) {
        String sanitized = GeneratedTestSanitizer.sanitize(generatedCode);
        if (sanitized.isBlank()) {
            return "";
        }

        sanitized = sanitized.replace("import org.mockito.MockitoExtension;", "import org.mockito.junit.jupiter.MockitoExtension;");
        sanitized = sanitized.replace("org.mockito.MockitoExtension", "org.mockito.junit.jupiter.MockitoExtension");

        sanitized = replaceCheckedExceptionInStubs(sanitized);

        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(sanitized);
            compilationUnit.setPackageDeclaration(prompt.analysis().packageName());
            normalizeTypeName(compilationUnit, prompt.className() + "Test");
            ensureImports(compilationUnit, prompt.analysis().importedTypes());
            ensureSpringValidationImports(compilationUnit);
            ensureCommonImports(compilationUnit);
            removeProductionAnnotationsFromTestClass(compilationUnit);
            ensureMockitoExtension(compilationUnit);
            ensureTestMethodsThrowException(compilationUnit);
            return compilationUnit.toString();
        } catch (RuntimeException ex) {
            return sanitized;
        }
    }

    private static void normalizeTypeName(CompilationUnit compilationUnit, String expectedTestClassName) {
        compilationUnit.findFirst(ClassOrInterfaceDeclaration.class)
                .ifPresent(declaration -> {
                    if (!expectedTestClassName.equals(declaration.getNameAsString())) {
                        declaration.setName(expectedTestClassName);
                    }
                });
    }

    private static void ensureImports(CompilationUnit compilationUnit, List<String> requiredImports) {
        LinkedHashSet<String> existingImports = new LinkedHashSet<>();
        compilationUnit.getImports().forEach(importDeclaration -> existingImports.add(importDeclaration.getNameAsString()));

        for (String qualifiedName : requiredImports) {
            if (qualifiedName == null || qualifiedName.isBlank()) {
                continue;
            }
            if (isProductionSideImport(qualifiedName)) {
                continue;
            }
            if (existingImports.contains(qualifiedName)) {
                continue;
            }
            compilationUnit.addImport(qualifiedName);
        }
    }

    private static boolean isProductionSideImport(String qualifiedName) {
        return qualifiedName.startsWith("org.springframework.beans.factory.annotation.")
                || qualifiedName.startsWith("org.springframework.stereotype.")
                || qualifiedName.startsWith("org.springframework.web.bind.annotation.")
                || qualifiedName.startsWith("jakarta.persistence.")
                || qualifiedName.startsWith("javax.persistence.");
    }

    private static final Pattern ONLY_OPEN_MOCKS_BODY = Pattern.compile(
            "\\{\\s*MockitoAnnotations\\.(?:openMocks|initMocks)\\(this\\);\\s*\\}"
    );

    private static void ensureMockitoExtension(CompilationUnit compilationUnit) {
        ClassOrInterfaceDeclaration testClass = compilationUnit
                .findFirst(ClassOrInterfaceDeclaration.class)
                .orElse(null);
        if (testClass == null) {
            return;
        }

        List<MethodDeclaration> openMocksMethods = testClass.getMethods().stream()
                .filter(m -> m.isAnnotationPresent("BeforeEach"))
                .filter(m -> {
                    String body = m.getBody().map(b -> b.toString().trim()).orElse("");
                    return body.contains("MockitoAnnotations.openMocks")
                            || body.contains("MockitoAnnotations.initMocks");
                })
                .toList();

        if (!openMocksMethods.isEmpty()) {
            openMocksMethods.stream()
                    .filter(m -> {
                        String body = m.getBody().map(b -> b.toString().trim()).orElse("");
                        return ONLY_OPEN_MOCKS_BODY.matcher(body).matches();
                    })
                    .forEach(MethodDeclaration::remove);

            boolean stillHasOpenMocks = testClass.getMethods().stream()
                    .anyMatch(m -> m.toString().contains("MockitoAnnotations.openMocks")
                            || m.toString().contains("MockitoAnnotations.initMocks"));
            if (stillHasOpenMocks) {
                compilationUnit.addImport("org.mockito.MockitoAnnotations");
            }
        }

        boolean usesMockito = testClass.getFields().stream()
                .anyMatch(f -> f.isAnnotationPresent("Mock") || f.isAnnotationPresent("InjectMocks"));
        boolean hasExtendWith = testClass.getAnnotations().stream()
                .anyMatch(a -> a.getNameAsString().equals("ExtendWith"));
        if (usesMockito && !hasExtendWith) {
            addExtendWithAnnotation(testClass);
        }
    }

    private static void addExtendWithAnnotation(ClassOrInterfaceDeclaration testClass) {
        NormalAnnotationExpr extendWith = new NormalAnnotationExpr(
                new com.github.javaparser.ast.expr.Name("ExtendWith"),
                new NodeList<>(new MemberValuePair("value",
                        new ClassExpr(StaticJavaParser.parseType("MockitoExtension"))))
        );
        testClass.addAnnotation(extendWith);
    }

    private static String replaceCheckedExceptionInStubs(String code) {
        code = code.replace("thenThrow(new Exception()", "thenThrow(new RuntimeException())")
                   .replace("thenThrow(new Exception(\"", "thenThrow(new RuntimeException(\"")
                   .replace("doThrow(new Exception()", "doThrow(new RuntimeException())")
                   .replace("doThrow(new Exception(\"", "doThrow(new RuntimeException(\"");
        return code;
    }

    private static void ensureSpringValidationImports(CompilationUnit compilationUnit) {
        String sourceText = compilationUnit.toString();

        boolean importsManve = compilationUnit.getImports().stream()
                .anyMatch(i -> i.getNameAsString().contains("MethodArgumentNotValidException"));
        if (importsManve && sourceText.contains("BindingResult")) {
            boolean alreadyImported = compilationUnit.getImports().stream()
                    .anyMatch(i -> i.getNameAsString().equals("org.springframework.validation.BindingResult"));
            if (!alreadyImported) {
                compilationUnit.addImport("org.springframework.validation.BindingResult");
            }
        }

        boolean importsUserDetailsService = compilationUnit.getImports().stream()
                .anyMatch(i -> i.getNameAsString().contains("UserDetailsService"));
        if (importsUserDetailsService && sourceText.contains("UserDetails")) {
            boolean alreadyImported = compilationUnit.getImports().stream()
                    .anyMatch(i -> i.getNameAsString().equals("org.springframework.security.core.userdetails.UserDetails"));
            if (!alreadyImported) {
                compilationUnit.addImport("org.springframework.security.core.userdetails.UserDetails");
            }
        }
    }

    private static final java.util.Set<String> PRODUCTION_CLASS_ANNOTATIONS = java.util.Set.of(
            "ControllerAdvice", "RestControllerAdvice",
            "RestController", "Controller",
            "Service", "Component", "Repository",
            "Configuration", "SpringBootApplication"
    );

    private static void removeProductionAnnotationsFromTestClass(CompilationUnit compilationUnit) {
        compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(cls ->
            cls.getAnnotations().removeIf(ann ->
                PRODUCTION_CLASS_ANNOTATIONS.contains(ann.getNameAsString()))
        );
    }

    private static void ensureTestMethodsThrowException(CompilationUnit compilationUnit) {
        compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(cls ->
            cls.getMethods().stream()
                .filter(m -> m.isAnnotationPresent("Test"))
                .filter(m -> m.getThrownExceptions().isEmpty())
                .forEach(m -> {
                    ReferenceType exceptionType = StaticJavaParser.parseClassOrInterfaceType("Exception");
                    m.addThrownException(exceptionType);
                })
        );
    }

    private static void ensureCommonImports(CompilationUnit compilationUnit) {
        LinkedHashSet<String> existingImports = new LinkedHashSet<>();
        compilationUnit.getImports().forEach(importDeclaration -> existingImports.add(importDeclaration.getNameAsString()));

        for (String qualifiedName : COMMON_IMPORTS) {
            if (existingImports.contains(qualifiedName)) {
                continue;
            }
            compilationUnit.addImport(qualifiedName);
        }

        for (String qualifiedName : COMMON_STATIC_IMPORTS) {
            String name = qualifiedName.endsWith(".*")
                    ? qualifiedName.substring(0, qualifiedName.length() - 2)
                    : qualifiedName;
            if (existingImports.contains(name)) {
                continue;
            }
            compilationUnit.addImport(name, true, true);
        }
    }

}
