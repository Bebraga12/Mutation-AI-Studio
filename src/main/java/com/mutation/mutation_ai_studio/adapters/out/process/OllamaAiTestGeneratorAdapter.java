package com.mutation.mutation_ai_studio.adapters.out.process;

import com.mutation.mutation_ai_studio.application.port.out.AiTestGeneratorPort;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class OllamaAiTestGeneratorAdapter implements AiTestGeneratorPort {

    private final ChatClient chatClient;

    public OllamaAiTestGeneratorAdapter(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("ChatClient.Builder não está disponível. Verifique a configuração do Spring AI/Ollama.");
        }
        this.chatClient = builder.build();
    }

    @Override
    public String generateTestCode(ClassTestPrompt prompt) {
        String response = chatClient.prompt()
                .user(prompt.prompt())
                .call()
                .content();

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("Ollama retornou uma resposta vazia para o prompt de geração de teste.");
        }

        return response;
    }
}
