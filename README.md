# Mutation AI Studio

Plataforma local para geração automatizada de testes unitários em projetos Java, com foco em qualidade (cobertura e mutation testing) e arquitetura hexagonal.

> Status atual (CLI): `scan`, `select` (alias `s`), `status` e a base do fluxo `create test`.

---

## Visão Geral

O Mutation AI Studio foi projetado para:

- escanear classes Java de um projeto alvo
- selecionar e persistir alvos para futura geração de testes
- visualizar o estado atual da seleção
- (próximas etapas) gerar testes JUnit 5 + Mockito com IA local

Tudo com execução local, sem dependência de cloud.

---

## Stack

- Java 21
- Spring Boot 3.5.x
- Maven Wrapper (`./mvnw`)
- Arquitetura Hexagonal (Ports and Adapters)

---

## Instalação e uso

O uso é pelo fluxo manual abaixo. Ele funciona igual em qualquer distro Linux,
macOS e Windows.

### 1. Pré-requisitos

Você precisa de três coisas: **Git**, **JDK 21** e **Ollama** (com um modelo baixado).
O Maven já vem embutido no projeto via wrapper (`./mvnw`), não precisa instalar.

| Sistema | JDK 21 | Ollama |
| --- | --- | --- |
| Arch / Manjaro | `sudo pacman -S jdk21-openjdk` | `sudo pacman -S ollama` (ou script oficial abaixo) |
| Debian / Ubuntu | `sudo apt install openjdk-21-jdk` | `curl -fsSL https://ollama.com/install.sh \| sh` |
| Fedora | `sudo dnf install java-21-openjdk-devel` | `curl -fsSL https://ollama.com/install.sh \| sh` |
| macOS (Homebrew) | `brew install openjdk@21` | `brew install ollama` |
| Windows (winget) | `winget install EclipseAdoptium.Temurin.21.JDK` | `winget install Ollama.Ollama` |

Confirme o Java (precisa ser 21+):

```bash
java -version
```

### 2. Baixar um modelo no Ollama

Deixe o servidor do Ollama rodando e baixe um modelo de código:

```bash
ollama serve        # deixe rodando em um terminal (no macOS/Windows o app já sobe sozinho)
ollama pull qwen2.5-coder:7b
```

Alternativa mais leve/rápida: `ollama pull deepseek-coder:6.7b`.

### 3. Clonar e compilar

```bash
git clone <url-do-repositorio> Mutation-AI-Studio
cd Mutation-AI-Studio
./mvnw clean package -DskipTests      # Windows: mvnw.cmd clean package -DskipTests
```

Isso gera `target/mutation-ai-studio-0.0.1-SNAPSHOT.jar`.

### 4. Rodar a CLI

**Linux / macOS** — use o launcher da raiz do projeto (ele localiza o jar e, se
faltar, compila automaticamente):

```bash
./mutation-ai scan .
./mutation-ai select .
./mutation-ai status .
```

**Windows (PowerShell ou CMD)** — rode o jar direto:

```powershell
java -jar target\mutation-ai-studio-0.0.1-SNAPSHOT.jar scan .
```

### 5. (Opcional) Chamar de qualquer diretório

Para não precisar do `./` nem estar dentro da pasta do projeto:

**Linux / macOS** — carregue o ambiente no terminal atual:

```bash
source scripts/env.sh
mutation-ai scan .
```

Para deixar permanente, adicione ao seu `~/.zshrc` ou `~/.bashrc`:

```bash
source /caminho/para/Mutation-AI-Studio/scripts/env.sh
```

**Windows** — crie um alias/função de PowerShell no seu `$PROFILE`:

```powershell
function mutation-ai { java -jar "C:\caminho\para\Mutation-AI-Studio\target\mutation-ai-studio-0.0.1-SNAPSHOT.jar" @args }
```

### Configurar o modelo da IA

A CLI lê o modelo do arquivo de configuração (opcional):

```bash
# Linux/macOS
~/.config/mutation-ai/config.env
```

Conteúdo esperado:

```bash
MUTATION_AI_OLLAMA_BASE_URL=http://localhost:11434
MUTATION_AI_OLLAMA_MODEL=qwen2.5-coder:7b
```

---

## CLI disponível

## 1) `scan`

Escaneia classes elegíveis em `src/main/java` do projeto alvo.

```bash
mutation-ai scan
mutation-ai scan .
mutation-ai scan /caminho/projeto-alvo
```

### Variações do scan

```bash
mutation-ai scan --focus testable
mutation-ai scan --verbose
mutation-ai scan service
mutation-ai scan controller
mutation-ai scan dto --verbose
mutation-ai scan /caminho/projeto-alvo service
```

Categorias suportadas em `scan <categoria>`:

- `service`
- `core`
- `controller`
- `repository`
- `entity`
- `dto`
- `config`
- `security`
- `other`

---

## 2) `select` e alias `s`

Seleciona alvos e persiste no projeto.

### Selecionar tudo

```bash
mutation-ai select .
mutation-ai s .
mutation-ai select /caminho/projeto-alvo
```

### Selecionar apenas uma classe

Por nome simples ou FQCN no projeto atual:

```bash
mutation-ai select UserService
mutation-ai select br.com.exemplo.service.UserService
```

Com projeto explícito:

```bash
mutation-ai select /caminho/projeto-alvo UserService
```

### Remover da seleção

Remover classe específica:

```bash
mutation-ai select --remove UserService
mutation-ai select --remove br.com.exemplo.service.UserService
```

Limpar toda a seleção:

```bash
mutation-ai select --remove .
```

---

## 3) `status`

Mostra o estado atual da seleção persistida.

```bash
mutation-ai status
mutation-ai status .
mutation-ai status /caminho/projeto-alvo
```

Saída:
- caminho do projeto
- total de classes selecionadas
- lista de classes selecionadas (verde via ANSI quando suportado)
- caminho do arquivo de seleção

Se não existir seleção, mostra mensagem amigável para executar `select`.

---

## 4) `create test` e alias `c t`

Lê a seleção atual, analisa a classe alvo, gera um teste candidato por classe selecionada e prepara o fluxo de validação real via Maven.

```bash
mutation-ai create test
mutation-ai c t
mutation-ai create test .
mutation-ai create test /caminho/projeto-alvo
```

Saída do comando:
- caminho do projeto
- total de classes selecionadas
- total de prompts gerados
- total de testes candidatos gerados
- total de execuções aprovadas e com falha no Maven
- caminho do lote gerado
- resultado por classe (APROVADO ou FALHOU, com erros e caminho do teste falho se houver)

Exemplo de saída:

```text
Projeto: /caminho/projeto
Classes selecionadas: 3
Prompts gerados: 3
Testes candidatos gerados: 3
Execuções aprovadas no Maven: 2
Execuções com falha no Maven: 1
Lote salvo em: /caminho/projeto/.mutation-ai/prompts/create-test-20260415-235655
Resultados por classe:
 - com.exemplo.service.UserService: APROVADO
 - com.exemplo.service.AuthService: APROVADO
 - com.exemplo.service.BillingService: FALHOU
   erro: [ERROR] cannot find symbol
   teste falho salvo em: /caminho/projeto/.mutation-ai/failed/BillingServiceTest.java
```

### O que cada prompt e execução devem considerar

Cada classe selecionada deve gerar um fluxo próprio com:
- fully qualified name da classe alvo
- caminho relativo do arquivo fonte
- dependências colaboradoras identificadas, priorizando construtor
- código fonte completo da classe alvo com limpeza de ruídos simples
- análise estrutural da classe para reduzir alucinação
- instruções estritas para geração de um único arquivo `*Test.java`
- espaço para refinamento orientado por erro real de compilação e execução

### Regras do prompt gerado

O template foi preparado para automação com IA local e pede explicitamente:
- retorno com apenas código Java
- sem markdown
- sem blocos como ` ```java ` ou ` ``` `
- sem explicações fora do código
- sem comentários explicando o código gerado
- uso de JUnit 5 e Mockito
- uso de `@Mock`, `@InjectMocks` e preferencialmente `@ExtendWith(MockitoExtension.class)` quando fizer sentido
- classe de teste no formato `NomeDaClasseTest`
- package compatível com a classe alvo
- foco em comportamento observável
- cobertura de caminho feliz, falhas relevantes, bordas, `null`, `Optional.empty()` e exceções quando aplicável
- nomes de testes descritivos e legíveis

### Validação real e aprovação

A direção arquitetural do projeto é:
- gerar um teste candidato
- salvar temporariamente o teste no projeto alvo
- executar `./mvnw -Dtest=<NomeDoTeste> test`
- capturar feedback real do Maven/JUnit
- refinar o teste quando houver falha
- considerar aprovado apenas o teste que passar no projeto real
- medir cobertura e mutation score após a aprovação funcional

### Organização dos arquivos gerados

Cada execução cria uma pasta própria para evitar mistura entre lotes diferentes.

Exemplo:

```text
.mutation-ai/prompts/
  create-test-20260415-235655/
    UserService.md
    AuthService.md
    BillingService.md
```

Se não existir seleção, o comando orienta executar `mutation-ai select .` antes.

---

## Persistência

Arquivos salvos no projeto alvo:

```text
<projectRoot>/.mutation-ai/selection.json
<projectRoot>/.mutation-ai/prompts/create-test-<timestamp>/<Classe>.md
```

Se a pasta/arquivo não existir, é criado automaticamente.

Exemplo simplificado:

```json
{
  "projectRoot": "/caminho/projeto",
  "selectedAt": "2026-03-26T00:00:00Z",
  "totalSelected": 2,
  "classes": [
    {
      "className": "UserService",
      "fullyQualifiedName": "com.exemplo.UserService",
      "relativePath": "com/exemplo/UserService.java"
    }
  ]
}
```

---

## Regras do scan

A lógica de descoberta permanece:

- busca em `src/main/java`
- inclui `.java`
- ignora:
  - `*Test.java`
  - `package-info.java`
  - `module-info.java`

---

## Estrutura arquitetural (resumo)

```text
adapters/in/cli          -> comandos scan/select/status/create test
application/usecase      -> casos de uso
application/port/in      -> contratos de entrada
application/port/out     -> contratos de saída
adapters/out/filesystem  -> scan e persistência/leituras locais
domain/model             -> modelos de domínio
```

---

## Observações

- Modos CLI (`scan`, `select`, `s`, `status`, `create`, `c`) rodam sem web e encerram ao final.
- Prefira sempre `./mvnw` para build/execução local.
# Mutation-AI-Studio
