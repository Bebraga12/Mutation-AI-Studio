package com.mutation.mutation_ai_studio.domain.model;

import java.util.List;

public record ClassAnalysis(
        String className,
        String packageName,
        String constructorSignature,
        List<String> constructorDependencies,
        List<String> fieldDependencies,
        List<MethodAnalysis> publicMethods,
        List<String> importedTypes,
        boolean usesOptional,
        boolean usesExceptions,
        List<String> nonPublicMethodNames
) {
}
