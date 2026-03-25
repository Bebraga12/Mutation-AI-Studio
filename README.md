# Mutation AI Studio

Plataforma local para geração automatizada de testes unitários em projetos Java, com foco em qualidade (cobertura e mutation testing) e arquitetura hexagonal.

> Status atual: o comando CLI implementado até agora é o **`scan`**.

---

## Visão Geral

O Mutation AI Studio foi projetado para:

- escanear classes Java de um projeto alvo
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

Executar aplicação Spring Boot (modo padrão):

```bash
./mvnw spring-boot:run
```

Gerar JAR:

```bash
./mvnw -DskipTests package
```

---

## Comandos CLI disponíveis (até agora)

## 1) `scan` (modo padrão)

Escaneia classes em `src/main/java` do projeto alvo e mostra saída amigável para humanos, com prioridades.

### Usando com JAR

Dentro de qualquer projeto Java alvo:

```bash
java -jar /caminho/Mutation-AI-Studio/target/mutation-ai-studio-0.0.1-SNAPSHOT.jar scan .
```

Também pode passar caminho explícito:

```bash
java -jar /caminho/Mutation-AI-Studio/target/mutation-ai-studio-0.0.1-SNAPSHOT.jar scan /caminho/projeto-alvo
```

### Se você tiver um alias/comando global `mutation-ai`

```bash
mutation-ai scan
mutation-ai scan /caminho/projeto-alvo
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
# ou
java -jar /caminho/target/mutation-ai-studio-0.0.1-SNAPSHOT.jar scan . --focus testable
```

---

## 3) `scan --verbose`

Mostra a listagem detalhada completa (classe + FQCN + path), agrupada por categoria.

```bash
mutation-ai scan --verbose
# ou
java -jar /caminho/target/mutation-ai-studio-0.0.1-SNAPSHOT.jar scan . --verbose
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

Com detalhes completos (verbose) no filtro:

```bash
mutation-ai scan dto --verbose
# ou
java -jar /caminho/target/mutation-ai-studio-0.0.1-SNAPSHOT.jar scan . dto --verbose
```

---

## Regras atuais do scan

A descoberta de classes (lógica funcional) permanece:

- busca em `src/main/java`
- inclui `.java`
- ignora:
  - `*Test.java`
  - `package-info.java`
  - `module-info.java`

A nova parte adicionada foi apenas de **formatação de saída no terminal** e **filtro de exibição por categoria**.

---

## Estrutura arquitetural (resumo)

```text
adapters/in/cli          -> entrada CLI (scan)
application/usecase      -> caso de uso ScanProjectService
application/port/in      -> ScanProjectUseCase
application/port/out     -> ProjectClassScannerPort
adapters/out/filesystem  -> scanner de classes no filesystem
domain/model             -> JavaClassCandidate
```

---

## Próximos comandos previstos

Ainda não implementados nesta branch:

- `generate <ClassName>`
- integração com IA local (Ollama)
- execução automatizada de JaCoCo e Pitest no fluxo de geração

---

## Observações

- O modo `scan` roda em perfil CLI (sem web, sem DataSource/JPA autoconfig) e encerra sozinho.
- Em ambiente de desenvolvimento, prefira sempre usar `./mvnw`.
