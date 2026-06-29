package com.mutation.mutation_ai_studio.application.usecase;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class RelatedTypeApiExtractor {

    private RelatedTypeApiExtractor() {
    }

    record TypeApi(String simpleName, List<String> constructors, List<String> methods) {
    }

    static List<TypeApi> extract(Path projectRoot, String classUnderTest, List<String> candidateFqns) {
        List<TypeApi> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Path sourceRoot = projectRoot.resolve("src/main/java");

        for (String fqn : candidateFqns) {
            if (fqn == null || fqn.isBlank() || !fqn.contains(".")) {
                continue;
            }
            String simpleName = fqn.substring(fqn.lastIndexOf('.') + 1);
            if (simpleName.equals(classUnderTest) || !seen.add(fqn)) {
                continue;
            }
            Path file = sourceRoot.resolve(fqn.replace('.', '/') + ".java");
            if (!Files.isRegularFile(file)) {
                continue;
            }
            TypeApi api = parse(file, simpleName);
            if (api != null) {
                result.add(api);
            }
        }
        return result;
    }

    private static TypeApi parse(Path file, String simpleName) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(Files.readString(file));
            ClassOrInterfaceDeclaration decl = cu.findFirst(ClassOrInterfaceDeclaration.class).orElse(null);
            if (decl == null || decl.isInterface() || decl.isAbstract()) {
                return null;
            }

            List<String> constructors = decl.getConstructors().stream()
                    .filter(ConstructorDeclaration::isPublic)
                    .map(c -> simpleName + "(" + c.getParameters().stream()
                            .map(p -> p.getType().asString() + " " + p.getNameAsString())
                            .collect(Collectors.joining(", ")) + ")")
                    .collect(Collectors.toCollection(ArrayList::new));

            boolean hasExplicitCtor = !decl.getConstructors().isEmpty();
            if (!hasExplicitCtor) {
                constructors.add(simpleName + "()");
            }

            List<String> methods = decl.getMethods().stream()
                    .filter(MethodDeclaration::isPublic)
                    .map(m -> m.getNameAsString() + "(" + m.getParameters().stream()
                            .map(p -> p.getType().asString())
                            .collect(Collectors.joining(", ")) + ") -> " + m.getType().asString())
                    .distinct()
                    .limit(40)
                    .toList();

            if (constructors.isEmpty() && methods.isEmpty()) {
                return null;
            }
            return new TypeApi(simpleName, constructors, methods);
        } catch (Exception ex) {
            return null;
        }
    }
}
