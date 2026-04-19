package com.mutation.mutation_ai_studio.adapters.out.ollama;

import com.mutation.mutation_ai_studio.application.port.out.AiTestGeneratorPort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class OllamaAiTestGeneratorAdapter implements AiTestGeneratorPort {

    private final ChatClient chatClient;

    public OllamaAiTestGeneratorAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String generate(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
