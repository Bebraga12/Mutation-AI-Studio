package com.mutation.mutation_ai_studio.adapters.in.cli;

import java.util.Locale;
import java.util.Optional;

enum ScanCategory {
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

    ScanCategory(String key, String label) {
        this.key = key;
        this.label = label;
    }

    String key() {
        return key;
    }

    String label() {
        return label;
    }

    static Optional<ScanCategory> fromToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String normalized = token.toLowerCase(Locale.ROOT).trim();
        for (ScanCategory category : values()) {
            if (category.key.equals(normalized)) {
                return Optional.of(category);
            }
        }

        return Optional.empty();
    }
}
