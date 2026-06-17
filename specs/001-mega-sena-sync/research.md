# Research: Sincronização com a API da Mega Sena

**Feature**: 001-mega-sena-sync | **Date**: 2026-05-31

Consolidação das decisões de pesquisa. A stack base (Java 21, Spring Boot, Maven,
Flyway, MySQL) já é fixada pela constituição; a pesquisa foca nos pontos com incerteza
real: a fonte oficial de dados e os mecanismos de resiliência, agendamento e teste.

## 1. Fonte oficial dos dados de concurso

- **Decision**: Usar o endpoint oficial do Portal de Loterias da Caixa como fonte da
  verdade:
  - Último concurso: `GET https://servicebus2.caixa.gov.br/portaldeloterias/api/megasena`
  - Concurso específico: `GET https://servicebus2.caixa.gov.br/portaldeloterias/api/megasena/{numero}`
  - Resposta JSON inclui (entre outros) `numero`, `dataApuracao` (dd/MM/yyyy),
    `listaDezenas` (strings "01".."60"), `acumulado`, `valorEstimadoProximoConcurso` e
    `listaRateioPremio` (faixas; faixa "Sena" traz `valorPremio`).
  - Base URL configurável (permite apontar para dublê/proxy em teste).
- **Rationale**: É a fonte oficial exigida pela spec (FR-001) e pelo Princípio II. Expõe
  por concurso exatamente o que precisamos (número, data, dezenas, valor do prêmio).
- **Alternatives considered**:
  - **BrasilAPI** (`/api/loterias/v1/megasena/{numero}`) e wrappers comunitários
    (`loteriascaixa-api`): documentados (OpenAPI) e mais estáveis para consumir, porém
    são intermediários. Decisão: mantê-los como **fallback opcional configurável**; a
    fonte primária permanece a oficial. A escolha não muda o modelo de dados.

## 2. Quirks de acesso à fonte oficial

- **Decision**: Cliente HTTP com: header `User-Agent` de navegador, `Accept:
  application/json`, timeout explícito de conexão/leitura, e confiança na cadeia TLS do
  domínio `servicebus2.caixa.gov.br` (incluir cadeia completa; não desabilitar
  verificação TLS).
- **Rationale**: O endpoint costuma rejeitar requisições sem `User-Agent` e pode
  responder lentamente; desligar TLS violaria boas práticas de segurança.
- **Alternatives considered**: Desabilitar verificação TLS — **rejeitado** (risco de
  segurança; preferir configurar truststore/cadeia correta).

## 3. Mecanismo de resiliência (retry/timeout/degradação)

- **Decision**: Resilience4j (`@Retry` + `TimeLimiter`/timeout no `RestClient`) com
  backoff exponencial e número máximo de tentativas configuráveis. A persistência local
  (MySQL) é o cache: leituras de concurso nunca dependem da fonte estar no ar.
- **Rationale**: Atende FR-007/FR-009 e o Princípio IV sem reinventar política de retry.
- **Alternatives considered**: Retry manual com `Thread.sleep` — **rejeitado**
  (frágil, sem backoff/observabilidade padronizada). Spring Retry — equivalente; escolha
  por Resilience4j pela combinação retry+timeout integrada.

## 4. Agendamento (cron diário)

- **Decision**: `@Scheduled(cron = "${megasena.sync.cron}")` do Spring, com expressão
  configurável (default uma vez ao dia, em horário após o sorteio). Um **lock** garante
  execução única (ver item 5).
- **Rationale**: Decisão de clarificação (cron 1x/dia). Spring Scheduling é nativo, sem
  dependência extra.
- **Alternatives considered**: Quartz — **rejeitado** por excesso de complexidade para
  um job diário (Princípio VI). Cron do SO — descartado para manter o agendamento
  versionado no app e testável.

## 5. Idempotência, lacunas e concorrência

- **Decision**:
  - **Idempotência**: upsert por `numero` do concurso; reprocessar não duplica nem
    altera dados de um concurso já confirmado (apenas insere o que falta).
  - **Lacunas**: a sincronização compara o maior `numero` armazenado com o `numero` do
    último concurso da fonte e importa os faltantes em sequência (também usado pela
    carga histórica retomável).
  - **Concorrência (agendada × manual)**: lock de execução única — uma linha/registro
    de `SyncRun` em estado `EM_EXECUCAO` (ou lock de aplicação) impede uma segunda sync
    simultânea; a segunda é recusada/ignorada.
- **Rationale**: Atende FR-006, FR-014 e o edge case de concorrência; mantém a sequência
  contínua exigida por SC-002.
- **Alternatives considered**: Reimportar tudo a cada execução — **rejeitado** (custo e
  risco à imutabilidade). Lock distribuído externo (Redis) — desnecessário para
  instância única; reavaliar se houver múltiplas instâncias.

## 6. Estratégia de testes

- **Decision**:
  - **WireMock** para simular a fonte oficial: respostas de sucesso, timeout/erro 5xx e
    payload malformado/incompleto (cobre FR-007/FR-009/FR-010 e os edge cases).
  - **Testcontainers (MySQL)** para validar migrações Flyway, idempotência e persistência
    reais.
  - **JUnit 5 + Mockito** para unidade (mapeamento e validação de dezenas).
- **Rationale**: Princípio III exige cobertura nos caminhos críticos; dublês evitam
  depender da fonte real em CI.
- **Alternatives considered**: Testar contra a API real — **rejeitado** (instável,
  não determinístico, dependente de rede).

## 7. Proteção das operações administrativas

- **Decision**: Spring Security restringe os endpoints `/api/admin/**` ao perfil
  **admin**. Os endpoints de leitura de concursos (`/api/concursos/**`) são somente
  leitura. O modelo de autenticação de usuários finais é de outra feature; aqui o acesso
  admin é protegido por mecanismo próprio (credencial/segredo de serviço configurável),
  desacoplado do login de usuários.
- **Rationale**: Decisão de clarificação (status para admin) e Princípio I (não expor
  operações sensíveis a usuários comuns) sem acoplar à feature de auth ainda inexistente.
- **Alternatives considered**: Deixar endpoints abertos — **rejeitado** (risco).
  Depender da feature de auth de usuários — **rejeitado** por acoplamento prematuro.

## Itens NEEDS CLARIFICATION remanescentes

Nenhum. Todos os desconhecidos técnicos foram resolvidos acima.
