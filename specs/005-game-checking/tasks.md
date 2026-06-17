---
description: "Task list — Conferência Automática de Jogos (backend)"
---

# Tasks: Conferência Automática de Jogos

**Input**: Design documents from `/specs/005-game-checking/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (conferencia-api.yaml), quickstart.md. **Depende da 004** (jogos isolados por `usuario_id`), da **001** (concursos + dezenas sorteadas) e da **002** (gate `ROLE_USUARIO`).

**Tests**: INCLUÍDOS — a Constituição torna **obrigatória** a cobertura do cálculo de acertos (quadra/quina/sena) e exige conferência **determinística e idempotente** (Princípio II); somam-se isolamento (FR-007), estado aguardando-sorteio (FR-005) e gate de conta ATIVA (FR-008). Reutilizam a base Testcontainers MySQL e o **verificador de identidade dublê** da 002.

**Organization**: Tarefas agrupadas por user story (US1 conferir um jogo → US2 listar conferências). Escopo: **apenas o backend** (`backend/`).

## Format: `[ID] [P?] [Story] Descrição com caminho de arquivo`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: User story a que a tarefa pertence (US1, US2)

## Path Conventions

- Backend: `backend/src/main/java/com/megasena/sync/`, recursos em `backend/src/main/resources/`, testes em `backend/src/test/java/com/megasena/sync/`
- Pacote desta feature: `conferencia/`. Arquivos compartilhados (coordenar): `config/SecurityConfig.java` (002/004), repositório de `concurso/` (001), `jogo/JogoRepository.java` (004)

---

## Phase 1: Setup (Infraestrutura compartilhada)

**Purpose**: Estrutura do contexto de conferência (sem novas dependências)

- [ ] T001 [P] Criar o pacote `conferencia` e os enums `Faixa.java` (SENA, QUINA, QUADRA, NENHUMA) e `StatusConferencia.java` (CONFERIDO, AGUARDANDO_SORTEIO) em `backend/src/main/java/com/megasena/sync/conferencia/`

---

## Phase 2: Foundational (Pré-requisitos bloqueantes)

**Purpose**: Migração de auditoria, leitura do concurso, cálculo puro, DTOs, segurança e base de teste que ambas as user stories usam

**⚠️ CRITICAL**: Nenhuma user story pode começar antes desta fase concluída

- [ ] T002 [P] Criar migração `backend/src/main/resources/db/migration/V8__create_evento_conferencia.sql`: tabela `evento_conferencia` (`id` BIGINT PK auto, `usuario_id` BINARY(16) NOT NULL FK→usuario(id), `jogo_id` BINARY(16) NOT NULL **sem FK**, `concurso_numero` INT NOT NULL, `acertos` TINYINT NOT NULL, `faixa` ENUM('SENA','QUINA','QUADRA','NENHUMA') NOT NULL, `premiado` BOOLEAN NOT NULL, `criado_em` DATETIME(6) NOT NULL; `UNIQUE(jogo_id,concurso_numero)`, `idx_conf_usuario`, `CHECK(acertos BETWEEN 0 AND 6)`) conforme data-model.md — não altera tabelas das 001/002/003/004
- [ ] T003 [P] Criar entidade `backend/src/main/java/com/megasena/sync/conferencia/EventoConferencia.java` + `EventoConferenciaRepository.java` com `boolean existsByJogoIdAndConcursoNumero(UUID, int)` (suporte à gravação idempotente)
- [ ] T004 Garantir a leitura das dezenas do concurso na 001 (arquivo de `concurso/` — coordenar): método `Set<Integer> findDezenasByConcursoNumero(int)` (a partir de `concurso_dezena`) e `boolean existsByNumero(int)` (reaproveitar o já previsto na 004); expor via um pequeno serviço/porta consumível pelo contexto `conferencia`
- [ ] T005 [P] Criar `backend/src/main/java/com/megasena/sync/conferencia/Conferencia.java` (record: jogoId, concursoNumero, status, acertos, faixa, premiado) e `ConferenciaResponse.java` (DTO de saída, conforme conferencia-api.yaml)
- [ ] T006 [P] Criar `backend/src/main/java/com/megasena/sync/conferencia/CalculadoraConferencia.java` (**puro/sem I/O**): dado o conjunto de dezenas do jogo e as 6 sorteadas, calcula `acertos` (interseção), `faixa` (6→SENA,5→QUINA,4→QUADRA,≤3→NENHUMA) e `premiado` (≥4) — núcleo testável (R1, Princípio II)
- [ ] T007 Atualizar `backend/src/main/java/com/megasena/sync/config/SecurityConfig.java` (arquivo da 002/004 — coordenar): mapear `/api/conferencias/**` para exigir `ROLE_USUARIO` (somente conta ATIVA); `/api/jogos/**` já é gated pela 004
- [ ] T008 [P] Criar suporte de teste `backend/src/test/java/com/megasena/sync/conferencia/ConferenciaIntegrationTest.java`: base reutilizando o `AbstractIntegrationTest` (Testcontainers MySQL) e o `VerificadorDeIdentidadeFake` da 002, com helpers para semear um `concurso` com 6 dezenas, semear `jogo` (004) para um usuário e autenticar contas **ATIVA**/**PENDENTE**

**Checkpoint**: Fundação pronta — auditoria, leitura do concurso, cálculo puro, DTOs, gate e base de teste disponíveis.

---

## Phase 3: User Story 1 - Conferir um jogo cujo concurso foi sorteado (Priority: P1) 🎯 MVP

**Goal**: O usuário confere um jogo seu contra o concurso vinculado — acertos, faixa e premiado quando sorteado; AGUARDANDO_SORTEIO caso contrário — de forma determinística, idempotente, isolada e auditada uma vez.

**Independent Test**: Com um concurso sorteado e um jogo do usuário, `GET /api/jogos/{id}/conferencia` retorna acertos/faixa/premiado corretos e grava 1 linha de auditoria; repetir não muda o resultado nem duplica a auditoria; jogo de outro usuário → 404; concurso não sorteado → AGUARDANDO_SORTEIO.

### Tests for User Story 1 ⚠️ (escrever ANTES e garantir que falham)

- [ ] T009 [P] [US1] Teste **puro** do cálculo `backend/src/test/java/com/megasena/sync/conferencia/CalculadoraConferenciaTest.java`: para jogos de **6, 7, 8 e 9** dezenas e várias interseções, verifica `acertos` (máx. 6), `faixa` (SENA/QUINA/QUADRA/NENHUMA) e `premiado` (⇔ acertos≥4) — **cobertura obrigatória** (Princípio II / SC-001, SC-003)
- [ ] T010 [P] [US1] Teste de conferência `backend/src/test/java/com/megasena/sync/conferencia/ConferirJogoIT.java`: concurso sorteado → `GET /api/jogos/{id}/conferencia` 200 `CONFERIDO` com acertos/faixa/premiado corretos e grava 1 `evento_conferencia` (Acceptance Scenarios 1–4)
- [ ] T011 [P] [US1] Teste aguardando-sorteio `backend/src/test/java/com/megasena/sync/conferencia/AguardandoSorteioIT.java`: jogo cujo concurso não existe na 001 → 200 `AGUARDANDO_SORTEIO`, sem acertos/faixa/premiado e **sem** linha de auditoria (FR-005 / SC-004)
- [ ] T012 [P] [US1] Teste de idempotência `backend/src/test/java/com/megasena/sync/conferencia/IdempotenciaConferenciaIT.java`: conferir o mesmo jogo 2× → resultado idêntico e **uma única** linha em `evento_conferencia`; **e** confirmar que o `jogo` (dezenas/vínculo) e o `concurso` permanecem **inalterados** após conferir (FR-006/FR-010/FR-011 / SC-002)
- [ ] T013 [P] [US1] Teste de isolamento `backend/src/test/java/com/megasena/sync/conferencia/ConferenciaIsolamentoIT.java`: conferir jogo de outro usuário → 404 `NAO_ENCONTRADO` (FR-007 / SC-005)
- [ ] T014 [P] [US1] Teste de gate `backend/src/test/java/com/megasena/sync/conferencia/GateContaAtivaConferenciaIT.java`: conta PENDENTE/REPROVADO em `/api/jogos/{id}/conferencia` → 403 `ACESSO_NEGADO`; sem token → 401 (FR-008 / Princípio IX)

### Implementation for User Story 1

- [ ] T015 [US1] Criar `backend/src/main/java/com/megasena/sync/conferencia/ConferenciaService.java` com `Conferencia conferir(UUID jogoId, UUID usuarioId)` (`@Transactional`): resolve o jogo via `JogoRepository.findByIdAndUsuarioId` (não-dono → 404); se o concurso vinculado **não existe** (T004) → `AGUARDANDO_SORTEIO`; senão lê as dezenas (T004), calcula via `CalculadoraConferencia` (T006), **grava a auditoria idempotente** (insere em `evento_conferencia` apenas se ainda não existir — T003) e retorna `CONFERIDO` — arquivo compartilhado com US2 (coordenar)
- [ ] T016 [US1] Criar `backend/src/main/java/com/megasena/sync/conferencia/ConferenciaController.java` com `GET /api/jogos/{id}/conferencia`: obtém `usuarioId` do `UsuarioAutenticado` (nunca do cliente — Princípio I), chama `conferir`, responde 200 + `ConferenciaResponse` — arquivo compartilhado (coordenar); depende de T015

**Checkpoint**: US1 funcional e testável de forma independente — MVP entregável (conferir um jogo).

---

## Phase 4: User Story 2 - Consultar a conferência dos meus jogos (Priority: P2)

**Goal**: O usuário lista a conferência de **todos** os seus jogos (conferidos ou aguardando), vendo apenas os próprios.

**Independent Test**: Com jogos do usuário em concursos sorteados e não sorteados, `GET /api/conferencias` retorna cada jogo com resultado ou AGUARDANDO_SORTEIO; jogos de outro usuário não aparecem.

### Tests for User Story 2 ⚠️

- [ ] T017 [P] [US2] Teste de listagem `backend/src/test/java/com/megasena/sync/conferencia/ListarConferenciasIT.java`: `GET /api/conferencias` retorna a conferência de todos os jogos do usuário (mistura CONFERIDO/AGUARDANDO_SORTEIO); jogos de outro usuário não aparecem (Acceptance Scenarios 1,2 / SC-005)

### Implementation for User Story 2

- [ ] T018 [US2] Adicionar `List<Conferencia> listar(UUID usuarioId)` ao `ConferenciaService` (T015): para cada jogo de `JogoRepository.findByUsuarioId`, reusa `conferir` — arquivo compartilhado (coordenar)
- [ ] T019 [US2] Adicionar `GET /api/conferencias` ao `ConferenciaController` (T016), derivando `usuarioId` do token — arquivo compartilhado (coordenar); depende de T018

**Checkpoint**: US1 e US2 funcionais — conferência de um jogo e visão consolidada, ambas isoladas e idempotentes.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Validação final e ajustes que atravessam as stories

- [ ] T020 [P] Validar os passos do `specs/005-game-checking/quickstart.md` (migração V8, conferir, aguardando, listar, isolamento, idempotência da auditoria)
- [ ] T021 [P] Atualizar `backend/README.md` documentando os endpoints de conferência (`/api/jogos/{id}/conferencia`, `/api/conferencias`) e a tabela `evento_conferencia`
- [ ] T022 Rodar `cd backend && ./mvnw clean verify` garantindo todos os testes verdes e revisão final de logs (sem PII/credenciais em claro — Princípio V)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências
- **Foundational (Phase 2)**: depende do Setup — BLOQUEIA as user stories
- **User Stories (Phase 3–4)**: dependem da Foundational; depois US1 → US2 (US2 reusa o `conferir` da US1)
- **Polish (Phase 5)**: depende das stories desejadas concluídas

### User Story Dependencies

- **US1 (P1)**: cria `ConferenciaService.conferir` + `ConferenciaController.GET /jogos/{id}/conferencia`. MVP, sem dependência de outras stories
- **US2 (P2)**: **estende** `ConferenciaService`/`ConferenciaController` (arquivos compartilhados com US1) reutilizando `conferir` para listar

### Within Each User Story

- Testes escritos e falhando ANTES da implementação (TDD nos caminhos críticos — Princípios II/III)
- Cálculo puro (`CalculadoraConferencia`) e leitura/auditoria (Foundational) antes do serviço
- Serviço (`ConferenciaService`) antes do controller
- Story completa antes de passar para a próxima

### Parallel Opportunities

- Setup: T001 isolado
- Foundational: T002, T003, T005, T006, T008 em paralelo (arquivos distintos); T004/T007 tocam arquivos das 001/002/004 (coordenar)
- US1: T009–T014 (testes) em paralelo; depois T015 → T016
- US2: T017 (teste); depois T018 → T019
- Com equipe: após a Foundational, US1 e US2 coordenando `ConferenciaService.java` e `ConferenciaController.java` (compartilhados)

---

## Parallel Example: User Story 1

```bash
# Testes da US1 juntos (devem falhar primeiro):
Task: "Teste CalculadoraConferenciaTest.java"   # puro, cobertura obrigatória
Task: "Teste ConferirJogoIT.java"
Task: "Teste AguardandoSorteioIT.java"
Task: "Teste IdempotenciaConferenciaIT.java"
Task: "Teste ConferenciaIsolamentoIT.java"
Task: "Teste GateContaAtivaConferenciaIT.java"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1: Setup (pacote + enums)
2. Phase 2: Foundational (CRÍTICO — migração V8, leitura do concurso, cálculo puro, DTOs, gate, base de teste)
3. Phase 3: User Story 1 (conferir um jogo)
4. **PARAR e VALIDAR**: acertos/faixa/premiado corretos; aguardando-sorteio; idempotência; isolamento; gate
5. Deploy/demo se pronto

### Incremental Delivery

1. Setup + Foundational → base de conferência pronta
2. US1 → conferir um jogo (MVP)
3. US2 → visão consolidada das conferências
4. Cada story agrega valor sem quebrar a anterior

### Parallel Team Strategy

Após a Foundational: Dev A → US1; Dev B → US2 (sobre o `conferir` da US1). Coordenar `ConferenciaService.java` e `ConferenciaController.java`.

---

## Notes

- [P] = arquivos diferentes, sem dependência pendente
- [Story] mapeia a tarefa à user story para rastreabilidade
- Verificar que os testes falham antes de implementar (TDD nos caminhos críticos — Princípios II/III)
- Conferência **determinística**: mesmo jogo × mesmo concurso → mesmo resultado; **somente leitura** sobre jogo/concurso (FR-010)
- Dono **sempre** derivado do token verificado (`UsuarioAutenticado`), nunca do cliente (Princípios I e VII)
- Auditoria **única por jogo×concurso** (UNIQUE); repetir a conferência não duplica (FR-011 / Princípio V)
- **Ordem de migração**: `V8` pressupõe `V5` (003) e `V6`/`V7` (004); garantir o merge **003 → 004 → 005** (ou `spring.flyway.out-of-order=true`) para evitar lacuna no Flyway (ver plan.md/data-model.md)
- Commit após cada tarefa ou grupo lógico
- Parar em qualquer checkpoint para validar a story isoladamente
