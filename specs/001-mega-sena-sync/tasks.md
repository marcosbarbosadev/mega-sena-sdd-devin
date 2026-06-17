---
description: "Task list — Sincronização com a API da Mega Sena (backend)"
---

# Tasks: Sincronização com a API da Mega Sena

**Input**: Design documents from `/specs/001-mega-sena-sync/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (admin-api.yaml, caixa-source.md), quickstart.md

**Tests**: INCLUÍDOS — a Constituição (Princípio III) e o plan.md tornam obrigatória a
cobertura nos caminhos críticos: mapeamento/validação da fonte, idempotência e integração
externa (sucesso/timeout/malformado).

**Organization**: Tarefas agrupadas por user story (P1 → P2 → P3) para implementação e
teste independentes. Escopo: **apenas o backend** (`backend/`).

## Format: `[ID] [P?] [Story] Descrição com caminho de arquivo`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: User story a que a tarefa pertence (US1, US2, US3)

## Path Conventions

- Backend: `backend/src/main/java/com/megasena/sync/`, recursos em
  `backend/src/main/resources/`, testes em `backend/src/test/java/com/megasena/sync/`
- Pacotes de domínio: `concurso/`, `sincronizacao/`, `fonte/`, `admin/`, `config/`

---

## Phase 1: Setup (Infraestrutura compartilhada)

**Purpose**: Inicialização do projeto e estrutura básica

- [ ] T001 Criar estrutura do monorepo: diretórios `backend/src/main/java/com/megasena/sync/{concurso,sincronizacao,fonte,admin,config}`, `backend/src/main/resources/db/migration`, `backend/src/test/java/com/megasena/sync/{concurso,sincronizacao,fonte,admin,support}` e placeholder `frontend/.gitkeep`
- [ ] T002 Criar `backend/pom.xml` com Java 21 e Spring Boot 3.x (starters: web, data-jpa, validation, security), Flyway core + flyway-mysql, MySQL Connector/J, `resilience4j-spring-boot3`; dependências de teste: spring-boot-starter-test (JUnit 5 + Mockito), `org.testcontainers:mysql`, `org.wiremock:wiremock-standalone`
- [ ] T003 Criar classe principal `backend/src/main/java/com/megasena/sync/MegaSenaSyncApplication.java` (com `@EnableScheduling`) e `backend/src/main/resources/application.yml` base (datasource MySQL, Flyway, JPA `ddl-auto: validate`)
- [ ] T004 [P] Criar `backend/src/main/java/com/megasena/sync/config/MegaSenaProperties.java` (`@ConfigurationProperties("megasena")`) mapeando `source.base-url`, `source.user-agent`, `source.timeout-ms`, `sync.cron`, `sync.retry.max-attempts`, `sync.retry.backoff-ms`, `admin.token`; preencher defaults em `application.yml` conforme quickstart.md

---

## Phase 2: Foundational (Pré-requisitos bloqueantes)

**Purpose**: Esquema, entidades, cliente da fonte e configuração que TODAS as user stories usam

**⚠️ CRITICAL**: Nenhuma user story pode começar antes desta fase concluída

- [ ] T005 [P] Criar migração Flyway `backend/src/main/resources/db/migration/V1__create_concurso.sql`: tabela `concurso` (`numero` INT PK, `data_sorteio` DATE NOT NULL, `valor_premio` DECIMAL(15,2) NOT NULL, `criado_em`/`atualizado_em` TIMESTAMP) e tabela filha `concurso_dezena` (`concurso_numero` FK ON DELETE CASCADE, `dezena` TINYINT CHECK 1..60, UNIQUE(`concurso_numero`,`dezena`), índice por `dezena`)
- [ ] T006 [P] Criar migração Flyway `backend/src/main/resources/db/migration/V2__create_sync_run.sql`: tabela `sync_run` (`id` BIGINT PK auto, `origem` ENUM, `status` ENUM, `iniciado_em`/`finalizado_em` TIMESTAMP, `concurso_inicial`/`concurso_final` INT, `concursos_importados` INT default 0, `tentativas` INT, `mensagem_erro` TEXT) com índice por `iniciado_em DESC` e índice por `status`
- [ ] T007 [P] Criar `backend/src/main/java/com/megasena/sync/concurso/Concurso.java` e `ConcursoDezena.java` (entidades JPA; `Concurso` com coleção de 6 dezenas, `@PrePersist`/`@PreUpdate` para timestamps)
- [ ] T008 [P] Criar `backend/src/main/java/com/megasena/sync/sincronizacao/SyncRun.java` com enums `OrigemSync` (AGENDADA, MANUAL) e `StatusSync` (EM_EXECUCAO, SUCESSO, FALHA, PARCIAL) em arquivos próprios no mesmo pacote
- [ ] T009 [P] Criar `backend/src/main/java/com/megasena/sync/concurso/ConcursoRepository.java` (`findTopByOrderByNumeroDesc`, `existsByNumero`, `findByNumero`)
- [ ] T010 [P] Criar `backend/src/main/java/com/megasena/sync/sincronizacao/SyncRunRepository.java` (`findTopByOrderByIniciadoEmDesc`, `existsByStatus(StatusSync)`)
- [ ] T011 [P] Criar `backend/src/main/java/com/megasena/sync/support/AbstractIntegrationTest.java` (base Testcontainers MySQL 8 + `@SpringBootTest`) em `backend/src/test/java/com/megasena/sync/support/`
- [ ] T012 [P] Criar DTO da fonte `backend/src/main/java/com/megasena/sync/fonte/CaixaConcursoResponse.java` (campos `numero`, `dataApuracao`, `listaDezenas`, `listaRateioPremio[].descricaoFaixa/valorPremio`) conforme contracts/caixa-source.md
- [ ] T013 [P] Implementar `backend/src/main/java/com/megasena/sync/fonte/ConcursoValidator.java` (FR-010: numero>0, dataApuracao presente/válida, exatamente 6 dezenas distintas 1..60, faixa "Sena" com valorPremio≥0) **com teste** `backend/src/test/java/com/megasena/sync/fonte/ConcursoValidatorTest.java` (casos válidos e cada rejeição)
- [ ] T014 [P] Implementar `backend/src/main/java/com/megasena/sync/fonte/ConcursoMapper.java` (parse `dataApuracao` dd/MM/yyyy→LocalDate, `listaDezenas` strings→int, extrair `valorPremio` da faixa "Sena") **com teste** `backend/src/test/java/com/megasena/sync/fonte/ConcursoMapperTest.java`
- [ ] T015 Criar `backend/src/main/java/com/megasena/sync/config/HttpClientConfig.java` (bean `RestClient` com base-url, header `User-Agent`, `Accept: application/json`, timeouts de conexão/leitura) e configurar instância Resilience4j (retry com max-attempts/backoff exponencial) em `application.yml`
- [ ] T016 Implementar `backend/src/main/java/com/megasena/sync/fonte/CaixaSourceClient.java` (`buscarUltimo()`, `buscarPorNumero(int)`, anotado `@Retry`/timeout) **com teste WireMock** `backend/src/test/java/com/megasena/sync/fonte/CaixaSourceClientWireMockTest.java` cobrindo sucesso, timeout, 5xx (retry esgotado) e payload malformado
- [ ] T017 [P] Criar `backend/src/main/java/com/megasena/sync/config/SecurityConfig.java` (Spring Security: `/api/admin/**` exige Bearer = `megasena.admin.token`; `/api/concursos/**` somente leitura/aberto; demais negados)
- [ ] T018 [P] Configurar logging estruturado e tratamento global de erros: `backend/src/main/java/com/megasena/sync/config/GlobalExceptionHandler.java` (`@RestControllerAdvice` mapeando 404/401/409) e padrão de log por execução

**Checkpoint**: Fundação pronta — entidades, repositórios, cliente da fonte e segurança disponíveis. User stories podem começar.

---

## Phase 3: User Story 1 - Manter o concurso mais recente atualizado (Priority: P1) 🎯 MVP

**Goal**: Sincronização agendada importa automaticamente o(s) concurso(s) novo(s) da fonte
oficial, de forma idempotente, preenchendo lacunas, registrando a execução e servindo o
último dado válido em cache durante indisponibilidade.

**Independent Test**: Disponibilizar (via WireMock) um concurso mais novo que o armazenado,
forçar a sync agendada e verificar que ele passa a existir localmente com dezenas e valor
do prêmio corretos, idêntico à fonte; reprocessar não duplica.

### Tests for User Story 1 ⚠️ (escrever ANTES e garantir que falham)

- [ ] T019 [P] [US1] Teste de integração `backend/src/test/java/com/megasena/sync/sincronizacao/SincronizacaoNovoConcursoIT.java` (WireMock + Testcontainers): fonte publica concurso novo → sync armazena número, data, 6 dezenas e valorPremio (Acceptance Scenario 1)
- [ ] T020 [P] [US1] Teste de idempotência `backend/src/test/java/com/megasena/sync/sincronizacao/SincronizacaoIdempotenciaIT.java`: rodar sync 2× → 0 duplicatas e nenhum dado alterado (Scenario 2 / SC-004)
- [ ] T021 [P] [US1] Teste de lacuna `backend/src/test/java/com/megasena/sync/sincronizacao/SincronizacaoLacunaIT.java`: gap entre armazenado e atual → importa faltantes em sequência contínua (Scenario 3 / FR-014)
- [ ] T022 [P] [US1] Teste de degradação graciosa `backend/src/test/java/com/megasena/sync/sincronizacao/DegradacaoGraciosaIT.java`: fonte indisponível → `GET /api/concursos/{numero}` já sincronizado responde 200 e `SyncRun` registra FALHA (SC-003)
- [ ] T023 [P] [US1] Teste de contrato dos endpoints de leitura `backend/src/test/java/com/megasena/sync/concurso/ConcursoControllerIT.java`: `GET /api/concursos/latest` e `GET /api/concursos/{numero}` retornam schema `Concurso` (200) e 404 quando ausente

### Implementation for User Story 1

- [ ] T024 [US1] Implementar `backend/src/main/java/com/megasena/sync/sincronizacao/SincronizacaoService.java`: calcular `max(numero)` armazenado, buscar último da fonte, importar faltantes (upsert idempotente por número via ConcursoRepository), preencher lacunas, abrir/fechar `SyncRun`, **lock de execução única** (recusar se já há `EM_EXECUCAO`) — depende de T009, T010, T013, T014, T016
- [ ] T025 [US1] Implementar `backend/src/main/java/com/megasena/sync/sincronizacao/SincronizacaoScheduler.java` (`@Scheduled(cron = "${megasena.sync.cron}")` dispara sync com origem AGENDADA)
- [ ] T026 [US1] Implementar `backend/src/main/java/com/megasena/sync/concurso/ConcursoService.java` (`buscarUltimo()`, `buscarPorNumero(int)`) lendo apenas do banco local (cache)
- [ ] T027 [US1] Implementar `backend/src/main/java/com/megasena/sync/concurso/ConcursoController.java` + DTO `ConcursoResponse.java` (`GET /api/concursos/latest` 200/404, `GET /api/concursos/{numero}` 200/404) conforme admin-api.yaml
- [ ] T028 [US1] Adicionar logging estruturado por execução de sincronização em `SincronizacaoService` (origem, faixa importada, resultado, duração — Princípio V)

**Checkpoint**: US1 funcional e testável de forma independente — MVP entregável.

---

## Phase 4: User Story 2 - Carga inicial do histórico completo (Priority: P2)

**Goal**: Importar todos os concursos passados (de 1 até o último da fonte) de forma
retomável, sem recriar nem corromper os já importados.

**Independent Test**: Rodar a carga histórica contra dublê com N concursos e verificar que
a quantidade armazenada corresponde ao total, sem lacunas nem duplicatas; interromper e
re-executar retoma sem reimportar tudo.

### Tests for User Story 2 ⚠️

- [ ] T029 [P] [US2] Teste de carga completa `backend/src/test/java/com/megasena/sync/sincronizacao/CargaHistoricaCompletaIT.java` (WireMock com N concursos): base vazia → todos importados em sequência contínua, sem duplicatas (Scenario 1 / SC-002)
- [ ] T030 [P] [US2] Teste de retomada `backend/src/test/java/com/megasena/sync/sincronizacao/CargaHistoricaRetomavelIT.java`: interromper no meio → re-executar completa sem recriar os já importados (Scenario 2 / edge case)

### Implementation for User Story 2

- [ ] T031 [US2] Estender `SincronizacaoService` com `cargaHistorica()`: importar a faixa de `1` (ou menor faltante) até o último da fonte reutilizando o upsert idempotente; retomável a partir do maior número já armazenado
- [ ] T032 [US2] Tratar falha transitória durante carga longa marcando `SyncRun` como `PARCIAL` (concursos já gravados preservados) e permitindo retomada na próxima execução

**Checkpoint**: US1 e US2 funcionam independentemente; base pode ser populada e mantida.

---

## Phase 5: User Story 3 - Visibilidade e operação da sincronização (Priority: P3)

**Goal**: Admin consulta o status da última sincronização e dispara uma sync manual de
recuperação, sem conflitar com a agendada.

**Independent Test**: Provocar syncs (manual e agendada), consultar status como admin e
verificar horário/resultado/concursos afetados; simular falha da fonte e ver a falha no
status.

### Tests for User Story 3 ⚠️

- [ ] T033 [P] [US3] Teste de status `backend/src/test/java/com/megasena/sync/admin/AdminSyncStatusIT.java`: após execuções, `GET /api/admin/sync/status` retorna 200 com iniciadoEm/finalizadoEm, status e faixa de concursos (Scenario 1 / SC-005); 404 se nenhuma
- [ ] T034 [P] [US3] Teste de disparo manual `backend/src/test/java/com/megasena/sync/admin/AdminSyncTriggerIT.java`: `POST /api/admin/sync/run` retorna 202 com `SyncRun` MANUAL; retorna 409 quando já há sync `EM_EXECUCAO` (Scenario 2 / lock)
- [ ] T035 [P] [US3] Teste de autorização `backend/src/test/java/com/megasena/sync/admin/AdminAuthIT.java`: endpoints `/api/admin/**` retornam 401 sem o Bearer token correto (FR-011)

### Implementation for User Story 3

- [ ] T036 [US3] Implementar `backend/src/main/java/com/megasena/sync/admin/AdminSyncController.java`: `GET /api/admin/sync/status` (mapeia última `SyncRun` para schema `SyncRun`) e `POST /api/admin/sync/run` (dispara sync MANUAL, 202)
- [ ] T037 [US3] Garantir, no controller/serviço, resposta `409` quando o lock de execução única recusa o disparo manual concorrente (integra com o lock da US1)

**Checkpoint**: Todas as user stories independentemente funcionais.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validação final e ajustes que atravessam as stories

- [ ] T038 [P] Validar os passos do `specs/001-mega-sena-sync/quickstart.md` (build `mvn clean verify`, run, carga histórica, verificações SC-001..SC-006)
- [ ] T039 [P] Criar `backend/README.md` documentando configuração (propriedades de `application.yml`), build e execução
- [ ] T040 Rodar `cd backend && mvn clean verify` garantindo todos os testes verdes e revisão final de logs/erros

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências — começa imediatamente
- **Foundational (Phase 2)**: depende do Setup — BLOQUEIA todas as user stories
- **User Stories (Phase 3–5)**: dependem da Foundational; depois podem seguir em paralelo
  ou em ordem de prioridade (P1 → P2 → P3)
- **Polish (Phase 6)**: depende das stories desejadas concluídas

### User Story Dependencies

- **US1 (P1)**: começa após a Foundational — sem dependência de outras stories (MVP)
- **US2 (P2)**: começa após a Foundational; **reutiliza** `SincronizacaoService` da US1
  (T024). Se trabalhada em paralelo à US1, coordenar o arquivo `SincronizacaoService.java`
- **US3 (P3)**: começa após a Foundational; integra com o **lock** implementado na US1
  (T024/T037), mas é testável de forma independente

### Within Each User Story

- Testes escritos e falhando ANTES da implementação
- Entidades/repos (Foundational) antes dos serviços
- Serviços antes dos endpoints
- Story completa antes de passar para a próxima prioridade

### Parallel Opportunities

- Setup: T004 em paralelo após T001–T003
- Foundational: T005–T014 e T017–T018 majoritariamente paralelos (arquivos distintos);
  T015→T016 sequenciais (cliente depende do RestClient/Resilience4j)
- US1: T019–T023 (testes) em paralelo; depois T024 → T025/T026 → T027 → T028
- US2: T029–T030 em paralelo; T031 → T032 (mesmo serviço, sequencial)
- US3: T033–T035 em paralelo; T036 → T037
- Com equipe: após a Foundational, US1/US2/US3 podem ser tocadas por devs diferentes
  (atenção ao arquivo compartilhado `SincronizacaoService.java` entre US1 e US2)

---

## Parallel Example: User Story 1

```bash
# Testes da US1 juntos (devem falhar primeiro):
Task: "Teste SincronizacaoNovoConcursoIT.java"
Task: "Teste SincronizacaoIdempotenciaIT.java"
Task: "Teste SincronizacaoLacunaIT.java"
Task: "Teste DegradacaoGraciosaIT.java"
Task: "Teste de contrato ConcursoControllerIT.java"
```

---

## Implementation Strategy

### MVP First (somente User Story 1)

1. Phase 1: Setup
2. Phase 2: Foundational (CRÍTICO — bloqueia tudo)
3. Phase 3: User Story 1
4. **PARAR e VALIDAR**: testar US1 isoladamente (sync agendada + leitura + idempotência)
5. Deploy/demo se pronto

### Incremental Delivery

1. Setup + Foundational → fundação pronta
2. US1 → manter resultado mais recente em dia (MVP)
3. US2 → popular histórico completo retomável
4. US3 → visibilidade/operação para admin
5. Cada story agrega valor sem quebrar a anterior

---

## Notes

- [P] = arquivos diferentes, sem dependência pendente
- [Story] mapeia a tarefa à user story para rastreabilidade
- Verificar que os testes falham antes de implementar (TDD nos caminhos críticos)
- Commit após cada tarefa ou grupo lógico
- Parar em qualquer checkpoint para validar a story isoladamente
