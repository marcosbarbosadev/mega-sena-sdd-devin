---
description: "Task list — Aprovação de Contas (Admin) (backend)"
---

# Tasks: Aprovação de Contas (Admin)

**Input**: Design documents from `/specs/003-account-approval/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (admin-moderation-api.yaml), quickstart.md. **Depende da feature 002** (entidade `Usuario`, estados, cadeia de identidade/segurança, `ROLE_ADMINISTRADOR`) já aplicada.

**Tests**: INCLUÍDOS — a Constituição (Princípio III) e o plan.md tornam obrigatória a cobertura nos caminhos críticos: guarda de transição (só de PENDENTE), exigência de motivo na reprovação (FR-004a), autorização admin-only (FR-002), concorrência (UPDATE condicionado — FR-008) e gravação da decisão na auditoria (FR-007). Os testes reutilizam o **verificador de identidade dublê** e a base Testcontainers MySQL da 002 (sem rede nem credencial real).

**Organization**: Tarefas agrupadas por user story (US1 listar → US2 aprovar → US3 reprovar; todas P1) para implementação e teste independentes. Escopo: **apenas o backend** (`backend/`).

## Format: `[ID] [P?] [Story] Descrição com caminho de arquivo`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: User story a que a tarefa pertence (US1, US2, US3)

## Path Conventions

- Backend: `backend/src/main/java/com/megasena/sync/`, recursos em `backend/src/main/resources/`, testes em `backend/src/test/java/com/megasena/sync/`
- Pacote desta feature: `moderacao/`. Arquivos compartilhados com a 002 (coordenar): `config/SecurityConfig.java`, `config/GlobalExceptionHandler.java`, `identidade/UsuarioRepository.java`

---

## Phase 1: Setup (Infraestrutura compartilhada)

**Purpose**: Estrutura do novo contexto de moderação (sem novas dependências — identidade e segurança vêm da 002)

- [ ] T001 [P] Criar o pacote `moderacao` e o enum `Decisao.java` (APROVADO, REPROVADO) em `backend/src/main/java/com/megasena/sync/moderacao/Decisao.java`

---

## Phase 2: Foundational (Pré-requisitos bloqueantes)

**Purpose**: Esquema, entidade de auditoria, exceção, consultas de transição e cadeia de autorização que TODAS as user stories usam

**⚠️ CRITICAL**: Nenhuma user story pode começar antes desta fase concluída

- [ ] T002 [P] Criar migração Flyway `backend/src/main/resources/db/migration/V5__create_decisao_moderacao.sql`: tabela `decisao_moderacao` (`id` BIGINT PK auto, `usuario_id` BINARY(16) NOT NULL FK→usuario(id), `admin_id` BINARY(16) NOT NULL FK→usuario(id), `decisao` ENUM('APROVADO','REPROVADO') NOT NULL, `motivo` VARCHAR(500) NULL, `criado_em` DATETIME(6) NOT NULL), com `idx_decisao_usuario`, `idx_decisao_admin_data`, as duas FKs e o `CONSTRAINT ck_decisao_motivo CHECK ((decisao='REPROVADO' AND motivo IS NOT NULL) OR decisao='APROVADO')` conforme data-model.md — **não** altera `usuario`
- [ ] T003 [P] Criar entidade JPA `backend/src/main/java/com/megasena/sync/moderacao/DecisaoModeracao.java` (id, usuarioId, adminId, decisao `@Enumerated(STRING)`, motivo, criadoEm; `@PrePersist` para `criadoEm`)
- [ ] T004 [P] Criar `backend/src/main/java/com/megasena/sync/moderacao/DecisaoModeracaoRepository.java` (`JpaRepository`; consultas de auditoria por `usuarioId`/`adminId` se necessário)
- [ ] T005 [P] Criar `backend/src/main/java/com/megasena/sync/moderacao/TransicaoInvalidaException.java` (estado de origem ≠ PENDENTE → 409) e `backend/src/main/java/com/megasena/sync/moderacao/ContaNaoEncontradaException.java` (conta alvo inexistente → 404)
- [ ] T006 Estender `backend/src/main/java/com/megasena/sync/identidade/UsuarioRepository.java` (arquivo da 002 — coordenar) com: `List<Usuario> findByEstado(EstadoConta estado)` (fila de pendentes) e um `@Modifying @Query` de **transição condicionada** `int transicionarDePendente(@Param("id") UUID id, @Param("novoEstado") EstadoConta novoEstado, @Param("agora") Instant agora)` cujo `WHERE id=:id AND estado='PENDENTE'` retorna linhas afetadas (R1)
- [ ] T007 Atualizar `backend/src/main/java/com/megasena/sync/config/SecurityConfig.java` (arquivo da 002 — coordenar): mapear `/api/admin/**` para exigir `ROLE_ADMINISTRADOR`; manter stateless e as regras existentes (`/api/auth/**`, `/api/perfil/**`, `/api/concursos/**`)
- [ ] T008 Atualizar `backend/src/main/java/com/megasena/sync/config/GlobalExceptionHandler.java` (arquivo da 002 — coordenar) para o schema `Erro`: `TransicaoInvalidaException` → 409 `TRANSICAO_INVALIDA`; validação de corpo (`MethodArgumentNotValidException` do motivo) → 400 `MOTIVO_OBRIGATORIO`; `ContaNaoEncontradaException` → 404 `CONTA_NAO_ENCONTRADA`; reutilizar 403 `ACESSO_NEGADO`/401 `NAO_AUTENTICADO` já existentes
- [ ] T009 [P] Criar suporte de teste `backend/src/test/java/com/megasena/sync/moderacao/ModeracaoIntegrationTest.java`: base reutilizando o `AbstractIntegrationTest` (Testcontainers MySQL) e o `VerificadorDeIdentidadeFake` da 002, com helpers para semear conta **PENDENTE** e autenticar um **ADMINISTRADOR/ATIVO** e um **USUARIO/ATIVO** comum

**Checkpoint**: Fundação pronta — tabela de auditoria, exceção, consultas de transição, gate admin e handler de erros disponíveis. User stories podem começar.

---

## Phase 3: User Story 1 - Listar e revisar contas pendentes (Priority: P1) 🎯 MVP

**Goal**: Um administrador lista a fila de contas PENDENTE com o mínimo para decidir (e-mail, método de cadastro, data do pedido); não-admin é barrado; contas já decididas não aparecem.

**Independent Test**: Semear contas PENDENTE/ATIVO/REPROVADO e chamar `GET /api/admin/contas/pendentes` como admin → vê só as pendentes com os campos certos; fila vazia → 200 com array vazio; como usuário comum → 403.

### Tests for User Story 1 ⚠️ (escrever ANTES e garantir que falham)

- [ ] T010 [P] [US1] Teste de listagem `backend/src/test/java/com/megasena/sync/moderacao/ListarPendentesIT.java`: com contas em PENDENTE/ATIVO/REPROVADO, `GET /api/admin/contas/pendentes` (admin) retorna 200 somente com as **PENDENTE** e campos `id,email,metodoLogin,criadoEm` (Acceptance Scenario 1)
- [ ] T011 [P] [US1] Teste de fila vazia `backend/src/test/java/com/megasena/sync/moderacao/FilaVaziaIT.java`: sem pendentes, `GET /api/admin/contas/pendentes` retorna 200 e array vazio (Acceptance Scenario 2)
- [ ] T012 [P] [US1] Teste de autorização admin-only `backend/src/test/java/com/megasena/sync/moderacao/ModeracaoAutorizacaoIT.java`: usuário comum (USUARIO/ATIVO) em `GET /api/admin/contas/pendentes` → 403 `ACESSO_NEGADO`; sem token → 401 (Acceptance Scenario 3 / FR-002 / SC-002)

### Implementation for User Story 1

- [ ] T013 [P] [US1] Criar DTO de projeção `backend/src/main/java/com/megasena/sync/moderacao/ContaPendenteResponse.java` (id uuid, email, metodoLogin, criadoEm)
- [ ] T014 [US1] Criar `backend/src/main/java/com/megasena/sync/moderacao/ModeracaoService.java` com `List<ContaPendenteResponse> listarPendentes()` usando `UsuarioRepository.findByEstado(PENDENTE)` (T006) — arquivo compartilhado com US2/US3 (coordenar)
- [ ] T015 [US1] Criar `backend/src/main/java/com/megasena/sync/moderacao/ModeracaoController.java` com `GET /api/admin/contas/pendentes` retornando 200 + lista (conforme admin-moderation-api.yaml) — arquivo compartilhado com US2/US3 (coordenar); depende de T014

**Checkpoint**: US1 funcional e testável de forma independente — MVP entregável (fila de pendentes visível só para admin).

---

## Phase 4: User Story 2 - Aprovar uma conta pendente (Priority: P1)

**Goal**: Um administrador aprova uma conta PENDENTE → ATIVO; o usuário passa a ter acesso; a decisão APROVADO é auditada; transições inválidas e corridas são recusadas sem estado inconsistente.

**Independent Test**: Aprovar uma conta PENDENTE (204) e verificar estado ATIVO + acesso liberado (`GET /api/perfil` 200) + 1 linha em `decisao_moderacao`; aprovar uma conta não-pendente → 409 sem alterar estado; duas aprovações concorrentes → só a 1ª vence.

### Tests for User Story 2 ⚠️

- [ ] T016 [P] [US2] Teste de aprovação `backend/src/test/java/com/megasena/sync/moderacao/AprovarContaIT.java`: `POST /api/admin/contas/{id}/aprovar` (admin) em conta PENDENTE → 204, estado vira ATIVO e grava `DecisaoModeracao` APROVADO com `admin_id` derivado do token (Acceptance Scenario 1 / FR-007 / SC-003)
- [ ] T017 [P] [US2] Teste de acesso pós-aprovação `backend/src/test/java/com/megasena/sync/moderacao/AcessoAposAprovacaoIT.java`: uma requisição da conta que **antes** da aprovação recebia 403 em `GET /api/perfil` passa a receber 200 **após** a aprovação, sem recadastrar (FR-006 / SC-005 / edge "aprovação durante sessão pendente")
- [ ] T018 [P] [US2] Teste de transição inválida `backend/src/test/java/com/megasena/sync/moderacao/AprovarNaoPendenteIT.java`: aprovar conta já ATIVO/REPROVADO → 409 `TRANSICAO_INVALIDA` sem alterar estado (Acceptance Scenario 3 / FR-005 / SC-004)
- [ ] T019 [P] [US2] Teste de concorrência `backend/src/test/java/com/megasena/sync/moderacao/DecisaoConcorrenteIT.java`: duas decisões concorrentes sobre a mesma conta PENDENTE — exatamente uma vence (204) e a outra recebe 409; estado final consistente, uma única linha de decisão (FR-008 / SC-006)
- [ ] T020 [P] [US2] Teste de conta inexistente `backend/src/test/java/com/megasena/sync/moderacao/ContaInexistenteIT.java`: aprovar id inexistente → 404 `CONTA_NAO_ENCONTRADA` (edge case)
- [ ] T020a [P] [US2] Teste de identidade do admin pelo servidor `backend/src/test/java/com/megasena/sync/moderacao/IdentidadeAdminDoServidorIT.java`: ao aprovar/reprovar enviando um `admin_id`/`adminId` espúrio no corpo ou query, a `DecisaoModeracao` gravada registra o **admin do token verificado** (`UsuarioAutenticado`), ignorando o valor do cliente (FR-009 / Princípios I e VII)

### Implementation for User Story 2

- [ ] T021 [US2] Adicionar `void aprovar(UUID usuarioId, UUID adminId)` ao `ModeracaoService` (T014, `@Transactional`): primeiro `usuarioRepository.findById(usuarioId)` — **ausente → `ContaNaoEncontradaException` (404)**; em seguida `transicionarDePendente(id, ATIVO, agora)` — **0 linhas → `TransicaoInvalidaException` (409)** (conta não-pendente ou decidida concorrentemente); grava `DecisaoModeracao(APROVADO, motivo=null)` na mesma transação — arquivo compartilhado (coordenar); depende de T006, T004
- [ ] T022 [US2] Adicionar `POST /api/admin/contas/{id}/aprovar` ao `ModeracaoController` (T015): obtém `adminId` do `UsuarioAutenticado` (nunca do cliente — Princípio I), chama `aprovar(...)`, responde 204 — arquivo compartilhado (coordenar); depende de T021

**Checkpoint**: US1 e US2 funcionam independentemente — admin aprova e o acesso é liberado, com auditoria e proteção de concorrência.

---

## Phase 5: User Story 3 - Reprovar uma conta pendente (Priority: P1)

**Goal**: Um administrador reprova uma conta PENDENTE → REPROVADO **com motivo obrigatório**; a conta permanece sem acesso; a decisão REPROVADO + motivo é auditada; transições inválidas são recusadas.

**Independent Test**: Reprovar uma conta PENDENTE com motivo (204) e verificar estado REPROVADO + acesso ainda bloqueado (`GET /api/perfil` 403) + 1 linha `decisao_moderacao` REPROVADO com o motivo; reprovar sem motivo → 400; reprovar conta não-pendente → 409.

### Tests for User Story 3 ⚠️

- [ ] T023 [P] [US3] Teste de reprovação `backend/src/test/java/com/megasena/sync/moderacao/ReprovarContaIT.java`: `POST /api/admin/contas/{id}/reprovar` com `{motivo}` (admin) em conta PENDENTE → 204, estado vira REPROVADO e grava `DecisaoModeracao` REPROVADO com o `motivo` e `admin_id` (Acceptance Scenario 1 / FR-007)
- [ ] T024 [P] [US3] Teste de motivo obrigatório `backend/src/test/java/com/megasena/sync/moderacao/ReprovarSemMotivoIT.java`: reprovar com corpo sem `motivo` (ou vazio) → 400 `MOTIVO_OBRIGATORIO` e nenhuma alteração de estado (Acceptance Scenario 2 / FR-004a)
- [ ] T025 [P] [US3] Teste de acesso pós-reprovação `backend/src/test/java/com/megasena/sync/moderacao/AcessoAposReprovacaoIT.java`: após reprovar, a conta continua sem acesso — `GET /api/perfil` retorna 403 (FR-006)
- [ ] T026 [P] [US3] Teste de transição inválida `backend/src/test/java/com/megasena/sync/moderacao/ReprovarNaoPendenteIT.java`: reprovar conta já ATIVO/REPROVADO → 409 `TRANSICAO_INVALIDA` (Acceptance Scenario 3 / FR-005)

### Implementation for User Story 3

- [ ] T027 [P] [US3] Criar DTO `backend/src/main/java/com/megasena/sync/moderacao/ReprovacaoRequest.java` com `@NotBlank String motivo` (validação na aplicação — FR-004a)
- [ ] T028 [US3] Adicionar `void reprovar(UUID usuarioId, UUID adminId, String motivo)` ao `ModeracaoService` (T014, `@Transactional`): primeiro `findById` — **ausente → `ContaNaoEncontradaException` (404)**; depois `transicionarDePendente(id, REPROVADO, agora)` — **0 linhas → `TransicaoInvalidaException` (409)**; grava `DecisaoModeracao(REPROVADO, motivo)` na mesma transação — arquivo compartilhado (coordenar); depende de T006, T004
- [ ] T029 [US3] Adicionar `POST /api/admin/contas/{id}/reprovar` ao `ModeracaoController` (T015) com `@Valid @RequestBody ReprovacaoRequest`: obtém `adminId` do `UsuarioAutenticado`, chama `reprovar(...)`, responde 204 — arquivo compartilhado (coordenar); depende de T028, T027

**Checkpoint**: Todas as user stories independentemente funcionais — fila, aprovação e reprovação com motivo, todas auditadas e admin-only.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validação final e ajustes que atravessam as stories

- [ ] T030 [P] Validar os passos do `specs/003-account-approval/quickstart.md` (migração V5, fila, aprovar/reprovar, 400/409/403, conferência da auditoria)
- [ ] T031 [P] Atualizar `backend/README.md` documentando os endpoints de moderação (`/api/admin/contas/**`), o papel exigido (ADMINISTRADOR) e a tabela de auditoria `decisao_moderacao`
- [ ] T032 Rodar `cd backend && ./mvnw clean verify` garantindo todos os testes verdes e revisão final de logs (sem PII/credenciais em claro — Princípio V); confirmar que não existe nenhum outro caminho de mutação de estado da conta além de `/aprovar` e `/reprovar` (FR-010 — sem `PATCH estado` ou ativação direta)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências — começa imediatamente
- **Foundational (Phase 2)**: depende do Setup — BLOQUEIA todas as user stories
- **User Stories (Phase 3–5)**: dependem da Foundational; depois podem seguir em paralelo ou em ordem (US1 → US2 → US3)
- **Polish (Phase 6)**: depende das stories desejadas concluídas

### User Story Dependencies

- **US1 (P1)**: começa após a Foundational — cria `ModeracaoService` (listar) e `ModeracaoController` (GET). Sem dependência de outras stories (MVP)
- **US2 (P1)**: começa após a Foundational; **estende** `ModeracaoService`/`ModeracaoController` (arquivos compartilhados com US1) com aprovar. Testável de forma independente
- **US3 (P1)**: começa após a Foundational; **estende** os mesmos arquivos com reprovar + `ReprovacaoRequest`. Independente

### Within Each User Story

- Testes escritos e falhando ANTES da implementação (TDD nos caminhos críticos — Princípio III)
- Entidade/repos/exceção/consultas (Foundational) antes do serviço
- Serviço (`ModeracaoService`) antes do controller
- Story completa antes de passar para a próxima

### Parallel Opportunities

- Setup: T001 isolado
- Foundational: T002–T005 e T009 em paralelo (arquivos distintos); T006/T007/T008 tocam arquivos da 002 (coordenar, não paralelizar entre si se houver sobreposição de revisão)
- US1: T010–T012 (testes) em paralelo; depois T013 → T014 → T015
- US2: T016–T020a (testes) em paralelo; depois T021 → T022
- US3: T023–T026 (testes) em paralelo; T027 em paralelo; depois T028 → T029
- Com equipe: após a Foundational, US1/US2/US3 por devs diferentes, **coordenando os arquivos compartilhados** `ModeracaoService.java` e `ModeracaoController.java`

---

## Parallel Example: User Story 2

```bash
# Testes da US2 juntos (devem falhar primeiro):
Task: "Teste AprovarContaIT.java"
Task: "Teste AcessoAposAprovacaoIT.java"
Task: "Teste AprovarNaoPendenteIT.java"
Task: "Teste DecisaoConcorrenteIT.java"
Task: "Teste ContaInexistenteIT.java"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1: Setup (pacote + enum `Decisao`)
2. Phase 2: Foundational (CRÍTICO — migração V5, entidade/repo de auditoria, exceção, consultas de transição, gate admin, handler)
3. Phase 3: User Story 1 (fila de pendentes admin-only)
4. **PARAR e VALIDAR**: admin vê só pendentes; usuário comum recebe 403; fila vazia OK
5. Deploy/demo se pronto

### Incremental Delivery

1. Setup + Foundational → base de moderação pronta
2. US1 → fila de pendentes (MVP)
3. US2 → aprovar (libera acesso, com auditoria e concorrência)
4. US3 → reprovar com motivo (mantém bloqueio, com auditoria)
5. Cada story agrega valor sem quebrar a anterior

### Parallel Team Strategy

Após a Foundational: Dev A → US1; Dev B → US2; Dev C → US3. Coordenar `ModeracaoService.java` e `ModeracaoController.java` (compartilhados pelas três stories).

---

## Notes

- [P] = arquivos diferentes, sem dependência pendente
- [Story] mapeia a tarefa à user story para rastreabilidade
- Verificar que os testes falham antes de implementar (TDD nos caminhos críticos — Princípio III)
- Identidade do admin **sempre** derivada do token verificado (`UsuarioAutenticado`), nunca do cliente (Princípios I e VII)
- Transição **somente a partir de PENDENTE** via UPDATE condicionado; 0 linhas → 409 (FR-005/FR-008)
- Motivo obrigatório na reprovação, validado na aplicação (`@NotBlank`) e no banco (`CHECK`) (FR-004a)
- Toda decisão gera exatamente uma linha em `decisao_moderacao` (FR-007 / Princípio V)
- Commit após cada tarefa ou grupo lógico
- Parar em qualquer checkpoint para validar a story isoladamente
