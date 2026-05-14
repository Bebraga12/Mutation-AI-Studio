package com.mutation.mutation_ai_studio.application.port.out;

import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;

public interface AiTestGeneratorPort {

    String generateTestCode(ClassTestPrompt prompt);
}
