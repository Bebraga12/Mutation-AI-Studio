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

    /**
     * System message que ancora o comportamento do modelo de código local.
     * Mantido curto e imperativo: modelos como qwen2.5-coder seguem melhor
     * instruções no system role do que diluídas no meio de um prompt enorme.
     */
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

    /**
     * Tamanho da janela de contexto (num_ctx) enviada ao Ollama.
     *
     * <p>CRÍTICO: o Ollama usa num_ctx=2048/4096 por padrão. O prompt gerado por
     * {@code CreateTestPromptService} (regras + análise + código fonte) já passa de
     * 2000 tokens para classes triviais, e os prompts de refinamento (prompt original +
     * erros do Maven + teste anterior) facilmente ultrapassam 6000 tokens. Sem fixar
     * num_ctx, o Ollama TRUNCA silenciosamente o prompt — descartando justamente o
     * SOURCE CLASS (que fica no fim) e as regras de refinamento. O modelo então
     * "alucina" a classe alvo, gerando testes que não compilam. Fixar um num_ctx amplo
     * é a maior alavanca de qualidade. qwen2.5-coder suporta até 32k.
     */
    private final int numCtx;

    /**
     * Limite de tokens de saída (num_predict). Um arquivo de teste completo com vários
     * métodos @Test pode passar de 2048 tokens; se cortado no meio, não compila.
     */
    private final int numPredict;

    private final double temperature;
    private final double topP;
    private final double repeatPenalty;

    /**
     * Decodificação determinística: temperatura 0 (greedy) + seed fixo fazem a MESMA classe gerar
     * o MESMO teste a cada execução, eliminando a variância "às vezes bom, às vezes lixo". O loop de
     * refinamento ainda explora soluções diferentes porque cada tentativa recebe um prompt distinto
     * (com os erros reais do Maven), então o determinismo não reduz a capacidade de recuperação.
     */
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
