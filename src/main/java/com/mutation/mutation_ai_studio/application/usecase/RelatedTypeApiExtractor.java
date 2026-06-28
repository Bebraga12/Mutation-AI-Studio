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

/**
 * Extrai a API pública (construtores + métodos) de tipos do PRÓPRIO projeto que a classe
 * sob teste referencia (entities, value objects, DTOs).
 *
 * <p>Motivação: o modelo só recebe o código fonte da classe sob teste. Quando o teste precisa
 * construir um colaborador do projeto (ex.: {@code new Carro(...)} dentro de um teste de
 * {@code CarroRepository}), o modelo NÃO conhece os construtores reais de {@code Carro} e os
 * inventa — gerando {@code new Carro(1L, "Ford", "Mustang")} quando o construtor real tem 5
 * argumentos. Isso causa "constructor cannot be applied to given types" e derruba todas as
 * tentativas. Fornecer a API real desses tipos elimina o chute.
 */
final class RelatedTypeApiExtractor {

    private RelatedTypeApiExtractor() {
    }

    record TypeApi(String simpleName, List<String> constructors, List<String> methods) {
    }

    /**
     * @param projectRoot      raiz do projeto alvo
     * @param classUnderTest   nome simples da classe sob teste (excluída do resultado)
     * @param candidateFqns    FQNs candidatos (vindos dos imports / tipos do mesmo pacote)
     */
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
                continue; // não é um tipo do projeto (lib externa) — pulamos
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

            // Se não há construtor explícito, Java fornece o no-arg padrão.
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
            return null; // arquivo não parseável — melhor omitir do que travar
        }
    }
}
