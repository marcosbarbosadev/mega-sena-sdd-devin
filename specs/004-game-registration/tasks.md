---
description: "Task list — Cadastro de Jogos (backend)"
---

# Tasks: Cadastro de Jogos

**Input**: Design documents from `/specs/004-game-registration/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (jogos-api.yaml), quickstart.md. **Depende da 002** (identidade + gate `ROLE_USUARIO`) e da **001** (tabela `concurso`, leitura).

**Tests**: INCLUÍDOS — a Constituição (Princípio III) e o plan.md tornam obrigatória a cobertura nos caminhos críticos: validação 6–9/1–60/sem repetição, regra "tudo ou geração" (FR-003), geração com **fonte de aleatoriedade determinística** (R4), gate da janela de edição (FR-008), isolamento por `usuario_id` (FR-006) e gate de conta ATIVA (FR-009). Reutilizam a base Testcontainers MySQL e o **verificador de identidade dublê** da 002.

**Organization**: Tarefas agrupadas por user story (US1 informar → US2 gerar → US3 listar/obter → US4 editar/excluir). Escopo: **apenas o backend** (`backend/`).

## Format: `[ID] [P?] [Story] Descrição com caminho de arquivo`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: User story a que a tarefa pertence (US1–US4)

## Path Conventions

- Backend: `backend/src/main/java/com/megasena/sync/`, recursos em `backend/src/main/resources/`, testes em `backend/src/test/java/com/megasena/sync/`
- Pacote desta feature: `jogo/`. Arquivos compartilhados (coordenar): `config/SecurityConfig.java`, `config/GlobalExceptionHandler.java` (da 002) e `concurso/ConcursoRepository.java` (da 001)

---

## Phase 1: Setup (Infraestrutura compartilhada)

**Purpose**: Estrutura do contexto de jogos (sem novas dependências)

- [ ] T001 [P] Criar o pacote `jogo` e o enum `TipoEventoJogo.java` (CADASTRO, EDICAO, EXCLUSAO) em `backend/src/main/java/com/megasena/sync/jogo/TipoEventoJogo.java`

---

## Phase 2: Foundational (Pré-requisitos bloqueantes)

**Purpose**: Esquema, entidades, repositórios, validação, resolução de concurso, auditoria, segurança e handler que TODAS as user stories usam

**⚠️ CRITICAL**: Nenhuma user story pode começar antes desta fase concluída

- [ ] T002 [P] Criar migração `backend/src/main/resources/db/migration/V6__create_jogo.sql`: tabela `jogo` (`id` BINARY(16) PK, `usuario_id` BINARY(16) NOT NULL FK→usuario(id), `concurso_numero` INT NOT NULL, `quantidade` TINYINT NOT NULL, `criado_em`/`atualizado_em` DATETIME(6) NOT NULL; `idx_jogo_usuario`, `idx_jogo_usuario_concurso`; `CHECK (quantidade BETWEEN 6 AND 9)`) e tabela `jogo_dezena` (`jogo_id` BINARY(16) NOT NULL FK→jogo(id) ON DELETE CASCADE, `dezena` TINYINT, `CHECK (dezena BETWEEN 1 AND 60)`, `UNIQUE(jogo_id,dezena)`) conforme data-model.md — não altera tabelas das 001/002
- [ ] T003 [P] Criar migração `backend/src/main/resources/db/migration/V7__create_evento_jogo.sql`: tabela `evento_jogo` (`id` BIGINT PK auto, `usuario_id` BINARY(16) NOT NULL FK→usuario(id), `jogo_id` BINARY(16) NOT NULL **sem FK**, `tipo` ENUM('CADASTRO','EDICAO','EXCLUSAO') NOT NULL, `criado_em` DATETIME(6) NOT NULL; `idx_evento_jogo_usuario`, `idx_evento_jogo_data`)
- [ ] T004 [P] Criar entidade JPA `backend/src/main/java/com/megasena/sync/jogo/Jogo.java` (id UUID BINARY(16), usuarioId, concursoNumero, quantidade, dezenas como `@ElementCollection` em `jogo_dezena` ou conjunto; `@PrePersist`/`@PreUpdate` para timestamps)
- [ ] T005 [P] Criar `backend/src/main/java/com/megasena/sync/jogo/JogoRepository.java` com consultas **sempre filtradas por dono**: `List<Jogo> findByUsuarioId(UUID)`, `Optional<Jogo> findByIdAndUsuarioId(UUID, UUID)`
- [ ] T006 [P] Criar `backend/src/main/java/com/megasena/sync/jogo/EventoJogo.java` + `EventoJogoRepository.java` + `AuditoriaJogoService.java` (registra CADASTRO/EDICAO/EXCLUSAO sem PII além do necessário — Princípio V)
- [ ] T007 [P] Criar exceções em `backend/src/main/java/com/megasena/sync/jogo/`: `ApostaInvalidaException.java` (→400), `SemConcursoAbertoException.java` (→409), `JogoBloqueadoException.java` (→409)
- [ ] T008 [P] Criar DTOs em `backend/src/main/java/com/megasena/sync/jogo/`: `CadastroJogoRequest.java` (`dezenas` OU `quantidade` — exatamente um; validação Bean Validation básica) e `JogoResponse.java` (id, concursoNumero, dezenas ordenadas, editavel, timestamps)
- [ ] T009 [P] Criar `backend/src/main/java/com/megasena/sync/jogo/ApostaValidator.java`: valida quantidade ∈ {6,7,8,9}, dezenas 1–60, sem repetição e coerência entre `quantidade` e o nº de dezenas (FR-004); lança `ApostaInvalidaException`
- [ ] T010 Estender `backend/src/main/java/com/megasena/sync/concurso/ConcursoRepository.java` (arquivo da 001 — coordenar) com `Optional<Integer> findMaxNumero()` e `boolean existsByNumero(int)`; criar `backend/src/main/java/com/megasena/sync/jogo/ConcursoAbertoService.java` com `int proximoEmAberto()` (= max+1; sem concursos → `SemConcursoAbertoException`) e `boolean foiSorteado(int numero)` (= `existsByNumero`)
- [ ] T011 Atualizar `backend/src/main/java/com/megasena/sync/config/SecurityConfig.java` (arquivo da 002 — coordenar): mapear `/api/jogos/**` para exigir `ROLE_USUARIO` (somente conta ATIVA); manter as regras existentes
- [ ] T012 Atualizar `backend/src/main/java/com/megasena/sync/config/GlobalExceptionHandler.java` (arquivo da 002 — coordenar) para o schema `Erro`: `ApostaInvalidaException`→400 `VALIDACAO`; cadastro parcial→400 `CADASTRO_PARCIAL`; `SemConcursoAbertoException`→409 `SEM_CONCURSO_ABERTO`; `JogoBloqueadoException`→409 `JOGO_BLOQUEADO`; jogo inexistente/de outro dono→404 `NAO_ENCONTRADO`
- [ ] T013 [P] Criar suporte de teste `backend/src/test/java/com/megasena/sync/jogo/JogoIntegrationTest.java`: base reutilizando o `AbstractIntegrationTest` (Testcontainers MySQL) e o `VerificadorDeIdentidadeFake` da 002, com helpers para semear concursos (`concurso`), autenticar uma conta **ATIVA** e uma **PENDENTE**, e criar jogos de apoio

**Checkpoint**: Fundação pronta — entidades, repositórios isolados, validação, resolução de concurso, auditoria, gate de conta ativa e handler disponíveis.

---

## Phase 3: User Story 1 - Cadastrar um jogo informando as dezenas (Priority: P1) 🎯 MVP

**Goal**: Uma conta ATIVA cadastra um jogo informando 6–9 dezenas válidas; o jogo é salvo, isolado pelo dono e vinculado ao próximo concurso em aberto (número fixo).

**Independent Test**: Autenticar como ATIVA, `POST /api/jogos` com 6 dezenas válidas → 201, `concursoNumero = MAX+1`, visível só ao dono; contagem/intervalo/repetição inválidos → 400; sem concursos → 409.

### Tests for User Story 1 ⚠️ (escrever ANTES e garantir que falham)

- [ ] T014 [P] [US1] Teste de cadastro informado `backend/src/test/java/com/megasena/sync/jogo/CadastrarInformadoIT.java`: `POST /api/jogos` com `dezenas` válidas (admin/usuário ATIVO) → 201, `concursoNumero = MAX(concurso.numero)+1`, jogo pertence ao usuário do token e grava evento `CADASTRO` (Acceptance Scenario 1 / SC-002)
- [ ] T015 [P] [US1] Teste de validação `backend/src/test/java/com/megasena/sync/jogo/CadastroInvalidoIT.java`: quantidade fora de 6–9, dezena fora de 1–60, repetição, ou nº de dezenas ≠ quantidade → 400 `VALIDACAO` sem persistir (Scenarios 2,3,4 / SC-006)
- [ ] T016 [P] [US1] Teste sem concurso aberto `backend/src/test/java/com/megasena/sync/jogo/SemConcursoAbertoIT.java`: base sem nenhum `concurso` → `POST /api/jogos` → 409 `SEM_CONCURSO_ABERTO` (edge case / FR-005)
- [ ] T016a [P] [US1] Teste de gate de conta ativa `backend/src/test/java/com/megasena/sync/jogo/GateContaAtivaIT.java`: requisições a `/api/jogos/**` com conta **PENDENTE/REPROVADO** (helper de T013) → 403 `ACESSO_NEGADO`; sem token → 401 (FR-009 / Princípio IX)

### Implementation for User Story 1

- [ ] T017 [US1] Criar `backend/src/main/java/com/megasena/sync/jogo/JogoService.java` com `JogoResponse cadastrarInformado(UUID usuarioId, List<Integer> dezenas)` (`@Transactional`): valida via `ApostaValidator` (T009), resolve `concursoNumero` via `ConcursoAbertoService.proximoEmAberto()` (T010), persiste o `Jogo` (T004/T005) e audita `CADASTRO` (T006) — arquivo compartilhado com US2/US3/US4 (coordenar)
- [ ] T018 [US1] Criar `backend/src/main/java/com/megasena/sync/jogo/JogoController.java` com `POST /api/jogos` (modo `dezenas`): obtém `usuarioId` do `UsuarioAutenticado` (nunca do cliente — Princípio I), chama `cadastrarInformado`, responde 201 + `JogoResponse` (conforme jogos-api.yaml) — arquivo compartilhado (coordenar); depende de T017

**Checkpoint**: US1 funcional e testável de forma independente — MVP entregável (cadastro informado).

---

## Phase 4: User Story 2 - Cadastrar um jogo com geração automática (Priority: P1)

**Goal**: Uma conta ATIVA informa só a quantidade (6–9) e o sistema gera as dezenas distintas, salvando como qualquer jogo; pedidos parciais (dezenas + quantidade) são recusados.

**Independent Test**: `POST /api/jogos` com `quantidade=8` → 201, jogo com 8 dezenas distintas 1–60 (fonte determinística no teste); quantidade fora de 6–9 → 400; informar `dezenas`+`quantidade` juntos → 400 `CADASTRO_PARCIAL`.

### Tests for User Story 2 ⚠️

- [ ] T019 [P] [US2] Teste de geração `backend/src/test/java/com/megasena/sync/jogo/GeracaoIT.java`: `POST /api/jogos` com `quantidade=8` (fonte de aleatoriedade dublê/semente fixa) → 201 com exatamente 8 dezenas distintas entre 1 e 60, vinculado ao próximo concurso (Acceptance Scenario 1 / SC-004)
- [ ] T020 [P] [US2] Teste de quantidade inválida `backend/src/test/java/com/megasena/sync/jogo/GeracaoInvalidaIT.java`: `quantidade` fora de 6–9 → 400 (Acceptance Scenario 2)
- [ ] T021 [P] [US2] Teste de cadastro parcial `backend/src/test/java/com/megasena/sync/jogo/CadastroParcialIT.java`: corpo com `dezenas` **e** `quantidade` (ou nenhum) → 400 `CADASTRO_PARCIAL` (Acceptance Scenario 3 / FR-003 / SC-003)

### Implementation for User Story 2

- [ ] T022 [P] [US2] Criar a porta `backend/src/main/java/com/megasena/sync/jogo/GeradorDeDezenas.java` (`Set<Integer> gerar(int quantidade)`) e o adaptador `GeradorDeDezenasAleatorio.java` com **fonte de aleatoriedade injetável** (`RandomGenerator` por construtor) gerando N dezenas distintas 1–60 (R4)
- [ ] T023 [US2] Adicionar `JogoResponse cadastrarGerado(UUID usuarioId, int quantidade)` ao `JogoService` (T017): gera via `GeradorDeDezenas` (T022), reusa `ApostaValidator`, persiste e audita `CADASTRO` — arquivo compartilhado (coordenar); depende de T022
- [ ] T024 [US2] Estender o `POST /api/jogos` do `JogoController` (T018) para **despachar** entre `dezenas` (US1) e `quantidade` (US2) e **recusar** corpo parcial/ambíguo (→ `CADASTRO_PARCIAL`) — arquivo compartilhado (coordenar); depende de T023

**Checkpoint**: US1 e US2 funcionam — cadastro informado e gerado, com a regra "tudo ou geração".

---

## Phase 5: User Story 3 - Consultar meus jogos (Priority: P2)

**Goal**: O usuário lista e obtém seus jogos (dezenas, concurso vinculado, se é editável), vendo **apenas** os próprios.

**Independent Test**: Com jogos de dois usuários, listar como um → só os seus; obter o próprio → 200; obter o de outro → 404; o campo `editavel` reflete se o concurso vinculado já foi sorteado.

### Tests for User Story 3 ⚠️

- [ ] T025 [P] [US3] Teste de listagem isolada `backend/src/test/java/com/megasena/sync/jogo/ListarJogosIT.java`: `GET /api/jogos` retorna só os jogos do usuário do token; jogos de outro usuário não aparecem (Acceptance Scenarios 1,2 / SC-002)
- [ ] T026 [P] [US3] Teste de obtenção `backend/src/test/java/com/megasena/sync/jogo/ObterJogoIT.java`: `GET /api/jogos/{id}` do próprio → 200; de outro usuário → 404 `NAO_ENCONTRADO` (isolamento / FR-006)
- [ ] T027 [P] [US3] Teste do flag `editavel` `backend/src/test/java/com/megasena/sync/jogo/EditavelFlagIT.java`: jogo cujo concurso vinculado ainda não tem linha em `concurso` → `editavel=true`; após semear o concurso vinculado → `editavel=false` (R3)

### Implementation for User Story 3

- [ ] T028 [US3] Adicionar `List<JogoResponse> listar(UUID usuarioId)` e `JogoResponse obter(UUID id, UUID usuarioId)` ao `JogoService` (T017), usando `JogoRepository.findByUsuarioId`/`findByIdAndUsuarioId` (não-dono → 404) — arquivo compartilhado (coordenar)
- [ ] T029 [US3] Implementar o mapeamento `Jogo → JogoResponse` calculando `editavel` via `ConcursoAbertoService.foiSorteado(concursoNumero)` (T010) — junto ao `JogoService`/um mapper no pacote `jogo`
- [ ] T030 [US3] Adicionar `GET /api/jogos` e `GET /api/jogos/{id}` ao `JogoController` (T018), derivando `usuarioId` do token — arquivo compartilhado (coordenar); depende de T028, T029

**Checkpoint**: US1–US3 funcionais — cadastro (2 modos) e consulta isolada.

---

## Phase 6: User Story 4 - Editar ou excluir um jogo antes do sorteio (Priority: P3)

**Goal**: Enquanto o concurso vinculado não foi sorteado, o dono edita (dezenas e quantidade 6–9, mesmo concurso) ou exclui o jogo; após o sorteio, é somente leitura.

**Independent Test**: Editar/excluir um jogo cujo concurso não foi sorteado → sucesso; após semear o concurso vinculado, editar/excluir → 409; jogo de outro usuário → 404.

### Tests for User Story 4 ⚠️

- [ ] T031 [P] [US4] Teste de edição `backend/src/test/java/com/megasena/sync/jogo/EditarJogoIT.java`: `PUT /api/jogos/{id}` (concurso não sorteado) trocando dezenas **e** quantidade (ex.: 6→9) → 200, mesmo `concursoNumero`, grava evento `EDICAO` (Acceptance Scenario 1 / FR-008)
- [ ] T032 [P] [US4] Teste de exclusão `backend/src/test/java/com/megasena/sync/jogo/ExcluirJogoIT.java`: `DELETE /api/jogos/{id}` (não sorteado) → 204, dezenas removidas em cascata, grava evento `EXCLUSAO` (Acceptance Scenario 2)
- [ ] T033 [P] [US4] Teste de bloqueio pós-sorteio `backend/src/test/java/com/megasena/sync/jogo/BloqueioPosSorteioIT.java`: com o concurso vinculado já presente em `concurso`, `PUT` e `DELETE` → 409 `JOGO_BLOQUEADO` sem alterar (Acceptance Scenario 3 / SC-005)
- [ ] T034 [P] [US4] Teste de não-dono `backend/src/test/java/com/megasena/sync/jogo/EdicaoNaoDonoIT.java`: `PUT`/`DELETE` de jogo de outro usuário → 404 `NAO_ENCONTRADO` (Acceptance Scenario 4 / FR-006)

### Implementation for User Story 4

- [ ] T035 [US4] Adicionar `JogoResponse editar(UUID id, UUID usuarioId, CadastroJogoRequest req)` ao `JogoService` (T017): resolve o jogo do dono (não-dono → 404); se `foiSorteado(concursoNumero)` → `JogoBloqueadoException`; revalida (informado ou gerado, reusando T009/T022), **substitui** dezenas e quantidade mantendo o `concursoNumero`, audita `EDICAO` — arquivo compartilhado (coordenar)
- [ ] T036 [US4] Adicionar `void excluir(UUID id, UUID usuarioId)` ao `JogoService` (T017): resolve o jogo do dono (não-dono → 404); se sorteado → `JogoBloqueadoException`; exclui (cascade `jogo_dezena`), audita `EXCLUSAO` — arquivo compartilhado (coordenar)
- [ ] T037 [US4] Adicionar `PUT /api/jogos/{id}` e `DELETE /api/jogos/{id}` ao `JogoController` (T018), derivando `usuarioId` do token — arquivo compartilhado (coordenar); depende de T035, T036

**Checkpoint**: Todas as user stories independentemente funcionais — cadastro (2 modos), consulta e edição/exclusão com janela pré-sorteio, tudo isolado e auditado.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validação final e ajustes que atravessam as stories

- [ ] T038 [P] Validar os passos do `specs/004-game-registration/quickstart.md` (migrações V6/V7, cadastro informado/gerado, parcial/inválido, isolamento, edição/exclusão e bloqueio pós-sorteio, auditoria)
- [ ] T039 [P] Atualizar `backend/README.md` documentando os endpoints de jogos (`/api/jogos/**`), o gate de conta ATIVA e as tabelas `jogo`/`jogo_dezena`/`evento_jogo`
- [ ] T040 Rodar `cd backend && ./mvnw clean verify` garantindo todos os testes verdes e revisão final de logs (sem PII/credenciais em claro — Princípio V)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências
- **Foundational (Phase 2)**: depende do Setup — BLOQUEIA todas as user stories
- **User Stories (Phase 3–6)**: dependem da Foundational; depois podem seguir em paralelo ou em ordem (US1 → US2 → US3 → US4)
- **Polish (Phase 7)**: depende das stories desejadas concluídas

### User Story Dependencies

- **US1 (P1)**: cria `JogoService.cadastrarInformado` + `JogoController.POST`. MVP, sem dependência de outras stories
- **US2 (P1)**: **estende** `JogoService`/`JogoController` (arquivos compartilhados com US1) com o modo geração + regra parcial. Adiciona a porta de geração
- **US3 (P2)**: **estende** os mesmos arquivos com listar/obter + mapeamento `editavel`
- **US4 (P3)**: **estende** os mesmos arquivos com editar/excluir + gate de janela

### Within Each User Story

- Testes escritos e falhando ANTES da implementação (TDD nos caminhos críticos — Princípio III)
- Entidades/repos/validação/resolução de concurso (Foundational) antes dos serviços
- Serviço (`JogoService`) antes do controller
- Story completa antes de passar para a próxima

### Parallel Opportunities

- Setup: T001 isolado
- Foundational: T002–T009 e T013 em paralelo (arquivos distintos); T010/T011/T012 tocam arquivos das 001/002 (coordenar)
- US1: T014–T016a (testes) em paralelo; depois T017 → T018
- US2: T019–T021 (testes) e T022 (porta) em paralelo; depois T023 → T024
- US3: T025–T027 (testes) em paralelo; depois T028 → T029 → T030
- US4: T031–T034 (testes) em paralelo; depois T035/T036 → T037
- Com equipe: após a Foundational, US1–US4 por devs diferentes, **coordenando** `JogoService.java` e `JogoController.java` (compartilhados pelas quatro stories)

---

## Parallel Example: User Story 1

```bash
# Testes da US1 juntos (devem falhar primeiro):
Task: "Teste CadastrarInformadoIT.java"
Task: "Teste CadastroInvalidoIT.java"
Task: "Teste SemConcursoAbertoIT.java"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1: Setup (pacote + enum)
2. Phase 2: Foundational (CRÍTICO — migrações, entidades, validação, resolução de concurso, auditoria, gate, handler)
3. Phase 3: User Story 1 (cadastro informado)
4. **PARAR e VALIDAR**: cadastro válido cria jogo vinculado ao próximo concurso, isolado; inválido recusado; sem concurso → 409
5. Deploy/demo se pronto

### Incremental Delivery

1. Setup + Foundational → base de jogos pronta
2. US1 → cadastro informado (MVP)
3. US2 → geração automática
4. US3 → consulta isolada
5. US4 → editar/excluir antes do sorteio
6. Cada story agrega valor sem quebrar a anterior

### Parallel Team Strategy

Após a Foundational: Dev A → US1; Dev B → US2; Dev C → US3; Dev D → US4. Coordenar `JogoService.java` e `JogoController.java` (compartilhados).

---

## Notes

- [P] = arquivos diferentes, sem dependência pendente
- [Story] mapeia a tarefa à user story para rastreabilidade
- Verificar que os testes falham antes de implementar (TDD nos caminhos críticos — Princípio III)
- Dono do jogo **sempre** derivado do token verificado (`UsuarioAutenticado`), nunca do cliente (Princípios I e VII)
- Validação de aposta antes de persistir; geração reusa a mesma validação (Princípio II)
- `concurso_numero` é INT fixo no cadastro (não FK); `editavel` é derivado da existência do concurso (R2/R3)
- Toda operação de jogo gera uma linha em `evento_jogo` (FR-010 / Princípio V)
- Commit após cada tarefa ou grupo lógico
- Parar em qualquer checkpoint para validar a story isoladamente
- **Ordem de migração**: `V6`/`V7` pressupõem a `V5` (feature 003) aplicada; garantir
  o merge **003 → 004** (ou `spring.flyway.out-of-order=true`) para evitar lacuna no
  histórico do Flyway (ver plan.md → Storage)
