package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.application.port.out.AiTestGeneratorPort;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import com.mutation.mutation_ai_studio.domain.model.GeneratedTestCandidate;
import com.mutation.mutation_ai_studio.domain.model.TestExecutionFeedback;
import org.springframework.stereotype.Service;

@Service
public class RefineGeneratedTestService {

    private final AiTestGeneratorPort aiTestGeneratorPort;

    public RefineGeneratedTestService(AiTestGeneratorPort aiTestGeneratorPort) {
        this.aiTestGeneratorPort = aiTestGeneratorPort;
    }

    public GeneratedTestCandidate refine(ClassTestPrompt prompt,
                                         GeneratedTestCandidate previousCandidate,
                                         TestExecutionFeedback feedback) {
        boolean hasCannotFindSymbol = feedback.errors().stream()
                .anyMatch(error -> error.toLowerCase().contains("cannot find symbol"));
        boolean hasWrongTarget = feedback.errors().stream()
                .anyMatch(error -> error.toLowerCase().contains("troca indevida da classe alvo")
                        || error.toLowerCase().contains("não referencia explicitamente a classe alvo")
                        || error.toLowerCase().contains("classe de teste gerada não corresponde")
                        || error.toLowerCase().contains("@autowired")
                        || error.toLowerCase().contains("@springboottest"));
        boolean hasMissingImports = feedback.errors().stream()
                .anyMatch(error -> error.toLowerCase().contains("sem import explícito"));

        String refinementPrompt = prompt.prompt()
                + System.lineSeparator()
                + System.lineSeparator()
                + "O teste anterior falhou na execução real do Maven." + System.lineSeparator()
                + "Corrija o arquivo abaixo com base nos erros reais." + System.lineSeparator()
                + "Nome esperado da classe de teste: " + previousCandidate.testClassName() + System.lineSeparator()
                + "Erros reais:" + System.lineSeparator()
                + String.join(System.lineSeparator(), feedback.errors()) + System.lineSeparator()
                + System.lineSeparator()
                + buildSymbolGuidance(prompt, hasCannotFindSymbol, hasWrongTarget, hasMissingImports) + System.lineSeparator()
                + System.lineSeparator()
                + "Teste anterior:" + System.lineSeparator()
                + previousCandidate.sourceCode();

        ClassTestPrompt refinementRequest = new ClassTestPrompt(
                prompt.className(),
                prompt.fullyQualifiedName(),
                prompt.relativePath(),
                prompt.dependencies(),
                prompt.analysis(),
                prompt.sourceCode(),
                refinementPrompt,
                prompt.savedPath()
        );

        String refinedCode = GeneratedTestSourceNormalizer.normalize(
                aiTestGeneratorPort.generateTestCode(refinementRequest),
                refinementRequest
        );
        return new GeneratedTestCandidate(
                refinementRequest,
                previousCandidate.className(),
                previousCandidate.fullyQualifiedName(),
                previousCandidate.testClassName(),
                refinedCode,
                previousCandidate.savedPath()
        );
    }

    private String buildSymbolGuidance(ClassTestPrompt prompt, boolean hasCannotFindSymbol, boolean hasWrongTarget, boolean hasMissingImports) {
        if (hasWrongTarget) {
            return "Orientações obrigatórias para corrigir o alvo do teste:" + System.lineSeparator()
                    + "- gere teste exclusivamente para a classe alvo " + prompt.className() + System.lineSeparator()
                    + "- o nome da classe de teste deve ser exatamente " + prompt.className() + "Test" + System.lineSeparator()
                    + "- não troque service por controller, nem controller por service" + System.lineSeparator()
                    + "- não use @SpringBootTest" + System.lineSeparator()
                    + "- não use @Autowired" + System.lineSeparator()
                    + "- use teste unitário puro com Mockito e a classe alvo correta";
        }

        if (hasMissingImports) {
            return "Orientações obrigatórias para corrigir imports faltantes:" + System.lineSeparator()
                    + "- adicione import explícito para todo tipo usado no teste" + System.lineSeparator()
                    + "- use estes imports reais da classe alvo como base: " + prompt.analysis().importedTypes() + System.lineSeparator()
                    + "- se usar entidades ou repositories do projeto, importe-os explicitamente" + System.lineSeparator()
                    + "- não deixe tipos como AutorRepository, LivroRepository, Autor, Livro, Login, Usuario, Optional, AuthenticationManager ou JwtServiceGenerator sem import";
        }

        if (!hasCannotFindSymbol) {
            return "Ajuste o teste preservando nomes reais, imports corretos e compatibilidade com o código da classe alvo.";
        }

        return "Orientações obrigatórias para corrigir cannot find symbol:" + System.lineSeparator()
                + "- use somente classes e collaborators reais já presentes no código-fonte da classe alvo" + System.lineSeparator()
                + "- reutilize os imports reais da análise estrutural: " + prompt.analysis().importedTypes() + System.lineSeparator()
                + "- reutilize os fields de dependência reais: " + prompt.analysis().fieldDependencies() + System.lineSeparator()
                + "- reutilize o construtor principal identificado: " + prompt.analysis().constructorSignature() + System.lineSeparator()
                + "- não invente nomes como Repository, Service, Entity ou DTO se eles não existirem exatamente com esse nome" + System.lineSeparator()
                + "- se faltar import, importe o tipo real existente no projeto em vez de criar outro nome";
    }
}
