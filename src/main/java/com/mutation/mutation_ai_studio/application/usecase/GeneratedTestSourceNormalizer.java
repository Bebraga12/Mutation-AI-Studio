package com.mutation.mutation_ai_studio.application.usecase;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;

import java.util.LinkedHashSet;
import java.util.List;

final class GeneratedTestSourceNormalizer {

    private static final List<String> COMMON_IMPORTS = List.of(
            "java.util.Optional",
            "java.util.List",
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.BeforeEach",
            "org.junit.jupiter.api.extension.ExtendWith",
            "org.mockito.Mock",
            "org.mockito.InjectMocks",
            "org.mockito.junit.jupiter.MockitoExtension",
            "org.springframework.beans.factory.annotation.Autowired",
            "org.springframework.stereotype.Service"
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
            return GeneratedTestFallbackFactory.generate(prompt);
        }

        sanitized = sanitized.replace("import org.mockito.MockitoExtension;", "import org.mockito.junit.jupiter.MockitoExtension;");
        sanitized = sanitized.replace("org.mockito.MockitoExtension", "org.mockito.junit.jupiter.MockitoExtension");

        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(sanitized);
            compilationUnit.setPackageDeclaration(prompt.analysis().packageName());
            normalizeTypeName(compilationUnit, prompt.className() + "Test");
            ensureImports(compilationUnit, prompt.analysis().importedTypes());
            ensureCommonImports(compilationUnit);
            return compilationUnit.toString();
        } catch (RuntimeException ex) {
            return GeneratedTestFallbackFactory.generate(prompt);
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
            if (existingImports.contains(qualifiedName)) {
                continue;
            }
            compilationUnit.addImport(qualifiedName);
        }
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
            if (existingImports.contains(qualifiedName)) {
                continue;
            }
            compilationUnit.addImport(qualifiedName, true, true);
        }
    }

}
