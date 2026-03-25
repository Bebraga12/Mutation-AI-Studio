package com.mutation.mutation_ai_studio.adapters.in.cli;

import com.mutation.mutation_ai_studio.domain.model.JavaClassCandidate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ScanOutputFormatter {

    private static final List<Category> ORDER = List.of(
            Category.SERVICE,
            Category.CORE,
            Category.CONTROLLER,
            Category.OTHER,
            Category.DTO,
            Category.ENTITY,
            Category.REPOSITORY,
            Category.CONFIG,
            Category.SECURITY
    );

    void print(Path projectRoot, List<JavaClassCandidate> classes, boolean verbose, boolean focusTestable) {
        if (classes.isEmpty()) {
            System.out.printf("Projeto: %s%n", projectRoot);
            System.out.println("0 classes encontradas");
            System.out.println("Nenhuma classe Java encontrada em src/main/java.");
            return;
        }

        Map<Category, List<JavaClassCandidate>> grouped = groupByCategory(classes);

        if (verbose) {
            printVerbose(projectRoot, classes, grouped);
            return;
        }

        if (focusTestable) {
            printFocusTestable(projectRoot, classes, grouped);
            return;
        }

        printDefault(projectRoot, classes, grouped);
    }

    private Map<Category, List<JavaClassCandidate>> groupByCategory(List<JavaClassCandidate> classes) {
        Map<Category, List<JavaClassCandidate>> grouped = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            grouped.put(category, new ArrayList<>());
        }

        classes.stream()
                .sorted(Comparator.comparing(JavaClassCandidate::fullyQualifiedName))
                .forEach(candidate -> grouped.get(classify(candidate)).add(candidate));

        return grouped;
    }

    private void printDefault(Path projectRoot, List<JavaClassCandidate> classes, Map<Category, List<JavaClassCandidate>> grouped) {
        System.out.printf("Projeto: %s%n", projectRoot);
        System.out.printf("%d classes encontradas%n%n", classes.size());

        System.out.println("Alta prioridade para testes");
        printCategoryWithNames(grouped, Category.SERVICE);
        printCategoryWithNames(grouped, Category.CORE);

        System.out.println("Média prioridade");
        printCategoryWithNames(grouped, Category.CONTROLLER);

        System.out.println("Baixa prioridade");
        printCategoryCountOnly(grouped, Category.DTO);
        printCategoryCountOnly(grouped, Category.ENTITY);
        printCategoryCountOnly(grouped, Category.REPOSITORY);
        printCombinedCount(grouped, "Config/Security", Category.CONFIG, Category.SECURITY);

        System.out.println();
        System.out.println("Use --verbose para listar todas as classes.");
        System.out.println("Use --focus testable para ver apenas classes mais testáveis.");
    }

    private void printFocusTestable(Path projectRoot, List<JavaClassCandidate> classes, Map<Category, List<JavaClassCandidate>> grouped) {
        List<JavaClassCandidate> focused = new ArrayList<>();
        focused.addAll(grouped.get(Category.SERVICE));
        focused.addAll(grouped.get(Category.CORE));

        System.out.printf("Projeto: %s%n", projectRoot);
        System.out.printf("%d classes encontradas | %d classes com foco testável%n%n", classes.size(), focused.size());

        System.out.println("Foco: classes mais relevantes para geração de testes");
        printCategoryWithNames(grouped, Category.SERVICE);
        printCategoryWithNames(grouped, Category.CORE);

        System.out.println("Dica: use --verbose para ver a listagem completa por categoria.");
    }

    private void printVerbose(Path projectRoot, List<JavaClassCandidate> classes, Map<Category, List<JavaClassCandidate>> grouped) {
        System.out.printf("Projeto: %s%n", projectRoot);
        System.out.printf("%d classes encontradas%n%n", classes.size());

        System.out.println("Resumo por categoria:");
        for (Category category : ORDER) {
            int count = grouped.get(category).size();
            System.out.printf(" - %s: %d%n", category.key, count);
        }

        System.out.println();
        for (Category category : ORDER) {
            List<JavaClassCandidate> candidates = grouped.get(category);
            if (candidates.isEmpty()) {
                continue;
            }

            System.out.printf("%s (%d)%n", category.key, candidates.size());
            candidates.forEach(candidate -> System.out.printf(" - %s | %s | %s%n",
                    candidate.className(),
                    candidate.fullyQualifiedName(),
                    candidate.relativePath()));
            System.out.println();
        }
    }

    private void printCategoryWithNames(Map<Category, List<JavaClassCandidate>> grouped, Category category) {
        List<JavaClassCandidate> candidates = grouped.get(category);
        System.out.printf(" %s (%d)%n", category.label, candidates.size());
        candidates.stream()
                .map(JavaClassCandidate::className)
                .forEach(name -> System.out.printf(" - %s%n", name));
    }

    private void printCategoryCountOnly(Map<Category, List<JavaClassCandidate>> grouped, Category category) {
        System.out.printf(" %s (%d)%n", category.label, grouped.get(category).size());
    }

    private void printCombinedCount(Map<Category, List<JavaClassCandidate>> grouped, String label, Category first, Category second) {
        int total = grouped.get(first).size() + grouped.get(second).size();
        System.out.printf(" %s (%d)%n", label, total);
    }

    private Category classify(JavaClassCandidate candidate) {
        String className = candidate.className().toLowerCase(Locale.ROOT);
        String fqcn = candidate.fullyQualifiedName().toLowerCase(Locale.ROOT);
        String path = candidate.relativePath().toLowerCase(Locale.ROOT);
        String full = String.join(" ", className, fqcn, path);

        if (containsAny(full, "service")) {
            return Category.SERVICE;
        }
        if (containsAny(full, "controller")) {
            return Category.CONTROLLER;
        }
        if (containsAny(full, "repository", "repo")) {
            return Category.REPOSITORY;
        }
        if (containsAny(full, "entity")) {
            return Category.ENTITY;
        }
        if (containsAny(full, "dto", "request", "response")) {
            return Category.DTO;
        }
        if (containsAny(full, "security", "auth", "jwt")) {
            return Category.SECURITY;
        }
        if (containsAny(full, "config", "configuration", "properties")) {
            return Category.CONFIG;
        }
        if (containsAny(full, "core", "generator", "renderer", "validator", "calculator", "facade", "usecase", "use_case", "use-case")) {
            return Category.CORE;
        }

        return Category.OTHER;
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private enum Category {
        SERVICE("service", "Services"),
        CORE("core", "Core"),
        CONTROLLER("controller", "Controllers"),
        REPOSITORY("repository", "Repositories"),
        ENTITY("entity", "Entities"),
        DTO("dto", "DTOs"),
        CONFIG("config", "Config"),
        SECURITY("security", "Security"),
        OTHER("other", "Other");

        private final String key;
        private final String label;

        Category(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }
}
