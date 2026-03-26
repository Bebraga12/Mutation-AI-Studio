# Mutation AI Studio

Plataforma local para geração automatizada de testes unitários em projetos Java, com foco em qualidade (cobertura e mutation testing) e arquitetura hexagonal.

> Status atual: comandos CLI implementados até agora: `scan`, `select` (alias `s`) e `status`.

---

## Visão Geral

O Mutation AI Studio foi projetado para:

- escanear classes Java de um projeto alvo
- selecionar e persistir alvos para futura geração de testes
- (próximas etapas) gerar testes JUnit 5 + Mockito com IA local
- avaliar qualidade com JaCoCo e Pitest

Tudo com execução local, sem dependência de cloud.

---

## Stack

- Java 21
- Spring Boot 3.5.x
- Maven Wrapper (`./mvnw`)
- Arquitetura Hexagonal (Ports and Adapters)

---

## Como rodar o projeto

Na raiz deste repositório:

```bash
./mvnw clean install
```

Gerar JAR:

```bash
./mvnw -DskipTests package
```

---

## Comandos CLI disponíveis

## 1) `scan` (modo padrão)

Escaneia classes em `src/main/java` do projeto alvo e mostra saída amigável com prioridades.

```bash
mutation-ai scan
mutation-ai scan /caminho/projeto-alvo
```

Ou com JAR:

```bash
java -jar /caminho/Mutation-AI-Studio/target/mutation-ai-studio-0.0.1-SNAPSHOT.jar scan .
```

### O que a saída padrão mostra

- caminho do projeto
- total de classes encontradas
- categorias por prioridade
  - **Alta prioridade:** `service`, `core`
  - **Média prioridade:** `controller`
  - **Baixa prioridade:** `dto`, `entity`, `repository`, `config/security`

---

## 2) `scan --focus testable`

Mostra apenas classes mais relevantes para geração de testes (principalmente `service` e `core`).

```bash
mutation-ai scan --focus testable
```

---

## 3) `scan --verbose`

Mostra listagem detalhada completa (classe + FQCN + path), agrupada por categoria.

```bash
mutation-ai scan --verbose
```

---

## 4) `scan <categoria>`

Filtra a saída para retornar **somente uma categoria**.

Categorias suportadas:

- `service`
- `core`
- `controller`
- `repository`
- `entity`
- `dto`
- `config`
- `security`
- `other`

Exemplos:

```bash
mutation-ai scan service
mutation-ai scan controller
mutation-ai scan dto
```

Com caminho explícito:

```bash
mutation-ai scan /caminho/projeto-alvo service
```

Com detalhes completos no filtro:

```bash
mutation-ai scan dto --verbose
```

---

## 5) `select` e alias `s`

Seleciona alvos elegíveis para testes (baseado no scan atual) e persiste no projeto alvo.

Comandos equivalentes:

```bash
mutation-ai select .
mutation-ai s .
```

Selecionar apenas uma classe (por nome simples ou FQCN) no projeto atual:

```bash
mutation-ai select UserService
mutation-ai select br.com.exemplo.service.UserService
```

Também com caminho explícito:

```bash
mutation-ai select /caminho/projeto-alvo
mutation-ai s /caminho/projeto-alvo
```

Remover um alvo específico da seleção atual:

```bash
mutation-ai select --remove UserService
```

Limpar toda a seleção atual:

```bash
mutation-ai select --remove .
```

### Persistência da seleção

A seleção é salva em:

```text
<projectRoot>/.mutation-ai/selection.json
```

Se a pasta/arquivo não existir, é criado automaticamente.

### O que o comando exibe

- caminho do projeto
- quantidade total de classes selecionadas
- alguns exemplos de classes selecionadas
- caminho onde a seleção foi salva

---

## 6) `status`

Mostra o estado atual da seleção persistida no projeto, em formato amigável de terminal.

```bash
mutation-ai status
mutation-ai status .
```

Com caminho explícito:

```bash
mutation-ai status /caminho/projeto-alvo
```

### O que o comando exibe

- caminho do projeto
- total de classes selecionadas
- lista das classes selecionadas (em verde)
- caminho do arquivo de seleção

Se não existir seleção, exibe uma mensagem amigável orientando a usar `select`.

---

## Regras atuais do scan

A descoberta de classes (lógica funcional) permanece:

- busca em `src/main/java`
- inclui `.java`
- ignora:
  - `*Test.java`
  - `package-info.java`
  - `module-info.java`

As mudanças recentes adicionaram apenas melhoria de apresentação no terminal e filtros de exibição/seleção.

---

## Estrutura arquitetural (resumo)

```text
adapters/in/cli          -> entrada CLI (scan/select/status)
application/usecase      -> casos de uso (scan/select/read status)
application/port/in      -> interfaces de entrada (use cases)
application/port/out     -> portas de saída (scanner/selection repo)
adapters/out/filesystem  -> filesystem para scan, persistência e leitura
domain/model             -> modelos de domínio
```

---

## Próximos comandos previstos

Ainda não implementados nesta branch:

- `generate <ClassName>`
- integração com IA local (Ollama)
- execução automatizada de JaCoCo e Pitest no fluxo de geração

---

## Observações

- Os modos `scan`, `select`, `s` e `status` rodam em perfil CLI (sem web, sem DataSource/JPA autoconfig) e encerram sozinhos.
- Em ambiente de desenvolvimento, prefira sempre usar `./mvnw`.
