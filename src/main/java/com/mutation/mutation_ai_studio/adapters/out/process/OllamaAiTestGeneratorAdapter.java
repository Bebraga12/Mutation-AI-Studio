package com.mutation.mutation_ai_studio.adapters.out.process;

import com.mutation.mutation_ai_studio.application.port.out.AiTestGeneratorPort;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class OllamaAiTestGeneratorAdapter implements AiTestGeneratorPort {

    private static final String SYSTEM_PROMPT = String.join("\n",
            "You are a senior Java test engineer. You write JUnit 5 + Mockito unit tests that COMPILE and PASS on the first run.",
            "Hard rules you NEVER break:",
            "- Output ONLY raw Java source. No markdown, no code fences, no prose, no explanation before or after.",
            "- Use ONLY classes, methods, fields and constructors that literally appear in the SOURCE CLASS or its listed imports. Never invent symbols.",
            "- Call ONLY the public methods listed under METHODS TO TEST, with their exact parameter types.",
            "- Read each method body and assert only what the code can actually produce.",
            "- Prefer self-contained @Test methods; avoid shared @BeforeEach stubs unless every test uses them."
    );

    private final ChatClient chatClient;
    private final OllamaModelConfig modelConfig;

    private final int numCtx;

    private final int numPredict;

    private final double temperature;
    private final double topP;
    private final double repeatPenalty;

    private final int seed;

    public OllamaAiTestGeneratorAdapter(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            OllamaModelConfig modelConfig,
            @Value("${mutation-ai.ollama.num-ctx:16384}") int numCtx,
            @Value("${mutation-ai.ollama.num-predict:4096}") int numPredict,
            @Value("${mutation-ai.ollama.temperature:0.0}") double temperature,
            @Value("${mutation-ai.ollama.top-p:0.9}") double topP,
            @Value("${mutation-ai.ollama.repeat-penalty:1.05}") double repeatPenalty,
            @Value("${mutation-ai.ollama.seed:42}") int seed) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("ChatClient.Builder não está disponível. Verifique a configuração do Spring AI/Ollama.");
        }
        this.chatClient = builder.build();
        this.modelConfig = modelConfig;
        this.numCtx = numCtx;
        this.numPredict = numPredict;
        this.temperature = temperature;
        this.topP = topP;
        this.repeatPenalty = repeatPenalty;
        this.seed = seed;
    }

    @Override
    public String generateTestCode(ClassTestPrompt prompt) {
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(modelConfig.getModel())
                .numCtx(numCtx)
                .numPredict(numPredict)
                .temperature(temperature)
                .topP(topP)
                .repeatPenalty(repeatPenalty)
                .seed(seed)
                .build();

        String response = chatClient.prompt()
                .options(options)
                .system(SYSTEM_PROMPT)
                .user(prompt.prompt())
                .call()
                .content();

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("Ollama retornou uma resposta vazia para o prompt de geração de teste.");
        }

        return response;
    }
}
