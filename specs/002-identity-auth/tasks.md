---
description: "Task list — Identidade & Autenticação (backend)"
---

# Tasks: Identidade & Autenticação

**Input**: Design documents from `/specs/002-identity-auth/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (auth-api.yaml, identity-provider.md), quickstart.md

**Tests**: INCLUÍDOS — a Constituição (Princípio III) e o plan.md tornam obrigatória a
cobertura nos caminhos críticos: derivação da identidade a partir do token, gate de estado
(pendente/ativo/reprovado), provisionamento JIT, unicidade (FR-008), regras de e-mail
verificado/vínculo (FR-014/FR-015) e degradação quando o provedor está indisponível
(FR-013 / Princípio IV). Os testes usam um **verificador de identidade dublê** (sem rede
nem credencial real).

**Organization**: Tarefas agrupadas por user story (P1 US1 → P1 US2 → P3 US3) para
implementação e teste independentes. Escopo: **apenas o backend** (`backend/`).

## Format: `[ID] [P?] [Story] Descrição com caminho de arquivo`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: User story a que a tarefa pertence (US1, US2, US3)

## Path Conventions

- Backend: `backend/src/main/java/com/megasena/sync/`, recursos em
  `backend/src/main/resources/`, testes em `backend/src/test/java/com/megasena/sync/`
- Pacotes desta feature: `identidade/`, `identidade/provedor/`, `identidade/provedor/firebase/`,
  `config/` (atualizações da cadeia de segurança da feature 001)

---

## Phase 1: Setup (Infraestrutura compartilhada)

**Purpose**: Dependências e configuração do provedor de identidade

- [ ] T001 Adicionar dependência `com.google.firebase:firebase-admin` (com versão em `<properties>`) ao `backend/pom.xml`
- [ ] T002 [P] Criar `backend/src/main/java/com/megasena/sync/config/IdentidadeProperties.java` (`@ConfigurationProperties("megasena.identidade")`) mapeando `project-id` e `admins-bootstrap` (lista de e-mails); adicionar o bloco `megasena.identidade.*` ao `backend/src/main/resources/application.yml` conforme quickstart.md
- [ ] T003 Criar `backend/src/main/java/com/megasena/sync/config/FirebaseConfig.java` que inicializa um `FirebaseApp` a partir de `GOOGLE_APPLICATION_CREDENTIALS` + `project-id`, com guarda para não inicializar quando ausente (perfis de teste usam o verificador dublê)

---

## Phase 2: Foundational (Pré-requisitos bloqueantes)

**Purpose**: Esquema, entidades, porta+adaptador de identidade, cadeia de segurança e
auditoria que TODAS as user stories usam

**⚠️ CRITICAL**: Nenhuma user story pode começar antes desta fase concluída

- [ ] T004 [P] Criar migração Flyway `backend/src/main/resources/db/migration/V3__create_usuario.sql`: tabela `usuario` (`id` BINARY(16) PK, `provider_uid` VARCHAR(128) NOT NULL UNIQUE, `email` VARCHAR(320) NOT NULL UNIQUE, `papel` ENUM('USUARIO','ADMINISTRADOR') NOT NULL, `estado` ENUM('PENDENTE','ATIVO','REPROVADO') NOT NULL, `metodo_login` ENUM('SENHA','GOOGLE') NOT NULL, `criado_em`/`atualizado_em` DATETIME(6) NOT NULL, `ultimo_acesso_em` DATETIME(6) NULL) conforme data-model.md
- [ ] T005 [P] Criar migração Flyway `backend/src/main/resources/db/migration/V4__create_evento_identidade.sql`: tabela `evento_identidade` (`id` BIGINT PK auto, `usuario_id` BINARY(16) NULL FK→usuario(id), `tipo` ENUM('CADASTRO','AUTENTICACAO','LOGOUT','ACESSO_NEGADO') NOT NULL, `metodo_login` ENUM('SENHA','GOOGLE') NULL, `sucesso` BOOLEAN NOT NULL, `motivo` VARCHAR(80) NULL, `correlation_id` VARCHAR(64) NULL, `criado_em` DATETIME(6) NOT NULL) com índices `idx_evento_usuario` e `idx_evento_tipo_data`
- [ ] T006 [P] Criar enums `Papel.java` (USUARIO, ADMINISTRADOR), `EstadoConta.java` (PENDENTE, ATIVO, REPROVADO) e `MetodoLogin.java` (SENHA, GOOGLE) em `backend/src/main/java/com/megasena/sync/identidade/`
- [ ] T007 [P] Criar entidade JPA `backend/src/main/java/com/megasena/sync/identidade/Usuario.java` (id UUID `BINARY(16)`, provider_uid, email, papel, estado, metodoLogin, timestamps; `@PrePersist`/`@PreUpdate`)
- [ ] T008 [P] Criar entidade JPA `backend/src/main/java/com/megasena/sync/identidade/EventoIdentidade.java` + enum `TipoEvento.java` (CADASTRO, AUTENTICACAO, LOGOUT, ACESSO_NEGADO) no mesmo pacote
- [ ] T009 [P] Criar `backend/src/main/java/com/megasena/sync/identidade/UsuarioRepository.java` (`findByProviderUid`, `existsByEmail`, `findByEmail`)
- [ ] T010 [P] Criar `backend/src/main/java/com/megasena/sync/identidade/EventoIdentidadeRepository.java`
- [ ] T011 [P] Criar a **porta** `backend/src/main/java/com/megasena/sync/identidade/provedor/VerificadorDeIdentidade.java` (`IdentidadeVerificada verify(String)`, `void revogarSessoes(String)`), o objeto de valor `IdentidadeVerificada.java` (uid, email, emailVerificado, metodoLogin) e as exceções `IdentidadeInvalidaException.java` e `ProvedorIndisponivelException.java` conforme contracts/identity-provider.md
- [ ] T012 [P] Implementar o **adaptador** `backend/src/main/java/com/megasena/sync/identidade/provedor/firebase/FirebaseVerificadorDeIdentidade.java`: `verify` via `FirebaseAuth.verifyIdToken(token, true)` (mapeia `sub`/`email`/`email_verified`/`firebase.sign_in_provider`, traduz falhas para as exceções da porta) e `revogarSessoes` via `revokeRefreshTokens(uid)` — depende de T011, T003
- [ ] T013 [P] Criar o principal de segurança `backend/src/main/java/com/megasena/sync/config/UsuarioAutenticado.java` (carrega `usuarioId`, `papel`, `estado`)
- [ ] T014 [P] Implementar `backend/src/main/java/com/megasena/sync/identidade/AuditoriaIdentidadeService.java` (registra `EventoIdentidade` sem credenciais/PII em claro — R8/Princípio V) — depende de T010
- [ ] T015 [P] Criar suporte de teste: verificador dublê `backend/src/test/java/com/megasena/sync/support/VerificadorDeIdentidadeFake.java` (devolve `IdentidadeVerificada` controlada por token e registra `revogarSessoes`) e base `IdentidadeIntegrationTest.java` reutilizando o `AbstractIntegrationTest` (Testcontainers MySQL) da feature 001, com o dublê como `@Primary`
- [ ] T016 Definir a costura de resolução de conta `backend/src/main/java/com/megasena/sync/identidade/ResolvedorDeConta.java` (interface: `UsuarioAutenticado resolver(IdentidadeVerificada)`) — implementação concreta vem na US1 (T024/T025)
- [ ] T017 Implementar `backend/src/main/java/com/megasena/sync/config/IdentidadeTokenFilter.java` (`OncePerRequestFilter`): extrai `Authorization: Bearer`, chama `VerificadorDeIdentidade.verify`, em sucesso delega ao `ResolvedorDeConta` e popula o `SecurityContext`; sem token/ inválido segue sem autenticação (401 pela autorização); `ProvedorIndisponivelException` → 503 — depende de T011, T013, T016
- [ ] T018 Atualizar `backend/src/main/java/com/megasena/sync/config/SecurityConfig.java`: **substituir** o `AdminTokenFilter` (feature 001) pelo `IdentidadeTokenFilter`; manter stateless; `/api/auth/**` exige autenticação; `/api/concursos/**` permanece leitura aberta; demais negados (o gate de recursos de usuário é adicionado na US2) — depende de T017
- [ ] T019 Atualizar `backend/src/main/java/com/megasena/sync/config/GlobalExceptionHandler.java` para o schema `Erro` (auth-api.yaml): 401 `NAO_AUTENTICADO` genérico (SC-006), 403 com `codigo` (EMAIL_NAO_VERIFICADO/CONTA_PENDENTE/CONTA_REPROVADA) e 503 quando o provedor está indisponível

**Checkpoint**: Fundação pronta — entidades, porta+adaptador de identidade, filtro de token, segurança e auditoria disponíveis. User stories podem começar.

---

## Phase 3: User Story 1 - Auto-cadastro de nova conta (Priority: P1) 🎯 MVP

**Goal**: A primeira requisição autenticada de uma identidade ainda sem conta cria a conta
em estado **PENDENTE** (just-in-time), recusa identidades Google com e-mail não verificado,
provisiona o admin inicial por configuração e nunca duplica contas.

**Independent Test**: Com o verificador dublê, chamar `GET /api/auth/me` para uma
identidade nova (e-mail+senha verificado) e verificar que a conta passa a existir em
PENDENTE; repetir não cria segunda conta; identidade Google sem e-mail verificado é
recusada sem persistir; e-mail em `admins-bootstrap` nasce ATIVO+ADMINISTRADOR.

### Tests for User Story 1 ⚠️ (escrever ANTES e garantir que falham)

- [ ] T020 [P] [US1] Teste de provisionamento JIT `backend/src/test/java/com/megasena/sync/identidade/ProvisionamentoJitIT.java`: 1ª chamada autenticada cria conta PENDENTE/USUARIO e registra evento `CADASTRO`; nenhuma coluna de senha existe (SC-004) — Acceptance Scenario 1
- [ ] T021 [P] [US1] Teste de admin bootstrap `backend/src/test/java/com/megasena/sync/identidade/AdminBootstrapIT.java`: identidade cujo e-mail está em `megasena.identidade.admins-bootstrap` nasce ATIVO + ADMINISTRADOR
- [ ] T022 [P] [US1] Teste de e-mail não verificado `backend/src/test/java/com/megasena/sync/identidade/EmailNaoVerificadoIT.java`: identidade Google com `emailVerificado=false` → 403 `EMAIL_NAO_VERIFICADO` e **nenhuma** conta persistida (FR-015) — Acceptance Scenario 2 / edge case
- [ ] T023 [P] [US1] Teste de unicidade/vínculo `backend/src/test/java/com/megasena/sync/identidade/UnicidadeContaIT.java`: mesmo `provider_uid` em duas chamadas → uma única conta; segunda identidade com mesmo e-mail (mesmo uid, FR-014) não duplica (FR-008) — Acceptance Scenario 3

### Implementation for User Story 1

- [ ] T024 [US1] Implementar `backend/src/main/java/com/megasena/sync/identidade/ProvisionamentoService.java`: resolve-or-create por `provider_uid`; recusa Google com e-mail não verificado (FR-015) lançando exceção mapeada a 403; aplica regra de admin bootstrap (R5); cria em PENDENTE/USUARIO por padrão; idempotente sob violação de UNIQUE; audita `CADASTRO` — depende de T009, T014
- [ ] T025 [US1] Implementar `backend/src/main/java/com/megasena/sync/identidade/ResolvedorDeContaImpl.java` (impl de T016): carrega/provisiona via `ProvisionamentoService` e monta `UsuarioAutenticado` com as autoridades conforme o estado (ATIVO→`ROLE_USUARIO` [+`ROLE_ADMINISTRADOR`]; PENDENTE/REPROVADO→sem autoridade de acesso) — depende de T024, T013
- [ ] T026 [US1] Implementar `backend/src/main/java/com/megasena/sync/identidade/ContaController.java` (`GET /api/auth/me`) + DTO `ContaResponse.java`: retorna a conta resolvida/provisionada (200 inclusive para PENDENTE/REPROVADO, para a pessoa conhecer seu estado); 403 `EMAIL_NAO_VERIFICADO` quando aplicável — conforme auth-api.yaml; depende de T025

**Checkpoint**: US1 funcional e testável de forma independente — MVP entregável (cadastro em PENDENTE).

---

## Phase 4: User Story 2 - Autenticar e acessar conforme o estado da conta (Priority: P1)

**Goal**: Identidade verificada vira identidade isolada; somente conta **ATIVO** acessa
recursos de usuário; **PENDENTE/REPROVADO** são barradas com o estado; toda requisição
autenticada resolve para exatamente um usuário, derivado do token no servidor.

**Independent Test**: Com o verificador dublê, acessar um recurso de usuário protegido com
conta ATIVO (200, resolvendo para aquele `usuarioId`); com conta PENDENTE/REPROVADO
(403 com o estado); sem token / token inválido (401 genérico); provedor indisponível
(503 sem persistir); e confirmar que um identificador enviado pelo cliente é ignorado.

### Tests for User Story 2 ⚠️

- [ ] T027 [P] [US2] Teste de acesso ATIVO + isolamento `backend/src/test/java/com/megasena/sync/identidade/AcessoAtivoIT.java`: conta ATIVO acessa `GET /api/perfil` (200) e a resposta resolve para exatamente o `usuarioId` do token (Acceptance Scenario 1 / SC-002)
- [ ] T028 [P] [US2] Teste de gate PENDENTE/REPROVADO `backend/src/test/java/com/megasena/sync/identidade/AcessoBloqueadoIT.java`: conta PENDENTE → 403 `CONTA_PENDENTE`; conta REPROVADO → 403 `CONTA_REPROVADA`; registra `ACESSO_NEGADO` (Scenarios 2 e 3 / SC-003)
- [ ] T029 [P] [US2] Teste de credenciais inválidas `backend/src/test/java/com/megasena/sync/identidade/AutenticacaoInvalidaIT.java`: token ausente/expirado/assinatura inválida → 401 genérico, sem revelar existência de e-mail (Scenario 4 / FR-012 / SC-006)
- [ ] T030 [P] [US2] Teste de provedor indisponível `backend/src/test/java/com/megasena/sync/identidade/ProvedorIndisponivelIT.java`: o verificador dublê lança `ProvedorIndisponivelException` → `GET /api/auth/me` e `GET /api/perfil` respondem **503** e **nenhuma** conta é persistida (FR-013 / Princípio IV)
- [ ] T031 [P] [US2] Teste de identidade derivada do servidor `backend/src/test/java/com/megasena/sync/identidade/IdentidadeDoServidorIT.java`: requisição com `usuarioId`/`email` de outro usuário no corpo/query é ignorada; o usuário resolvido vem do token (Scenario 5 / FR-005 / Princípio I)

### Implementation for User Story 2

- [ ] T032 [US2] Implementar `backend/src/main/java/com/megasena/sync/identidade/PerfilController.java` (`GET /api/perfil`): primeiro recurso de usuário, exige conta ATIVO e devolve o `ContaResponse` do próprio usuário autenticado (demonstra gate + isolamento) — depende de T026
- [ ] T033 [US2] Estender `backend/src/main/java/com/megasena/sync/config/SecurityConfig.java` com o **gate de recursos de usuário**: `/api/perfil/**` (e futuros recursos de usuário) exigem `ROLE_USUARIO` (somente ATIVO); manter `/api/auth/**` aberto a qualquer autenticado — arquivo compartilhado com T018 (coordenar)
- [ ] T034 [US2] Implementar a auditoria de acesso e `ultimo_acesso_em`: atualizar `ultimo_acesso_em` e registrar `AUTENTICACAO` na resolução bem-sucedida (em `ResolvedorDeContaImpl`); registrar `ACESSO_NEGADO` via `AccessDeniedHandler`/`AuthenticationEntryPoint` em `backend/src/main/java/com/megasena/sync/config/` mapeando o estado da conta para `CONTA_PENDENTE`/`CONTA_REPROVADA` — depende de T025, T014

**Checkpoint**: US1 e US2 funcionam independentemente — identidade isolada estabelecida e gate de estado aplicado no servidor.

---

## Phase 5: User Story 3 - Encerrar a sessão (Priority: P3)

**Goal**: Usuário autenticado encerra a sessão; o provedor revoga os refresh tokens e
acessos subsequentes exigem nova autenticação.

**Independent Test**: Autenticar, chamar `POST /api/auth/logout` (204) e verificar que o
verificador registrou a revogação; um token marcado como revogado passa a ser rejeitado
(401).

### Tests for User Story 3 ⚠️

- [ ] T035 [P] [US3] Teste de logout `backend/src/test/java/com/megasena/sync/identidade/LogoutIT.java`: `POST /api/auth/logout` retorna 204, o verificador dublê registra `revogarSessoes(uid)` e um evento `LOGOUT` é gravado (Acceptance Scenario 1)
- [ ] T036 [P] [US3] Teste de token revogado `backend/src/test/java/com/megasena/sync/identidade/TokenRevogadoIT.java`: após a revogação, requisição com o token revogado (dublê sinaliza revogado via `IdentidadeInvalidaException`) → 401, exigindo nova autenticação (FR-010)

### Implementation for User Story 3

- [ ] T037 [US3] Adicionar `POST /api/auth/logout` ao `backend/src/main/java/com/megasena/sync/identidade/ContaController.java`: chama `VerificadorDeIdentidade.revogarSessoes(uid)`, audita `LOGOUT` e responde 204 — depende de T012, T014, T026

**Checkpoint**: Todas as user stories independentemente funcionais.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validação final e ajustes que atravessam as stories

- [ ] T038 [P] Validar os passos do `specs/002-identity-auth/quickstart.md` (configuração por ambiente, migrações V3/V4, geração de token via emulador, verificações dos acceptance scenarios)
- [ ] T039 [P] Atualizar `backend/README.md` documentando a configuração de identidade (`megasena.identidade.project-id`, `admins-bootstrap`, `GOOGLE_APPLICATION_CREDENTIALS`) e o fluxo de obtenção de token via emulador
- [ ] T040 Rodar `cd backend && ./mvnw clean verify` garantindo todos os testes verdes e revisão final de logs/erros (sem credenciais/PII em claro — Princípio V)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências — começa imediatamente
- **Foundational (Phase 2)**: depende do Setup — BLOQUEIA todas as user stories
- **User Stories (Phase 3–5)**: dependem da Foundational; depois podem seguir em paralelo
  ou em ordem de prioridade (US1 → US2 → US3)
- **Polish (Phase 6)**: depende das stories desejadas concluídas

### User Story Dependencies

- **US1 (P1)**: começa após a Foundational — preenche a costura `ResolvedorDeConta` (T016)
  com o provisionamento JIT. Sem dependência de outras stories (MVP)
- **US2 (P1)**: começa após a Foundational; **reutiliza** `ResolvedorDeContaImpl` (T025) e
  o `ContaResponse` (T026) da US1, e **estende** o `SecurityConfig` (T018→T033, arquivo
  compartilhado). Testável de forma independente
- **US3 (P3)**: começa após a Foundational; adiciona o endpoint de logout ao
  `ContaController` (T026, arquivo compartilhado com a US1)

### Within Each User Story

- Testes escritos e falhando ANTES da implementação (TDD nos caminhos críticos)
- Entidades/repos/porta (Foundational) antes dos serviços
- Serviços (`ProvisionamentoService`, `ResolvedorDeContaImpl`) antes dos controllers
- Story completa antes de passar para a próxima prioridade

### Parallel Opportunities

- Setup: T002 em paralelo; T001/T003 tocam pom/config
- Foundational: T004–T015 majoritariamente paralelos (arquivos distintos); T016→T017→T018
  sequenciais (costura → filtro → SecurityConfig); T012 depende de T011/T003
- US1: T020–T023 (testes) em paralelo; depois T024 → T025 → T026
- US2: T027–T031 (testes) em paralelo; T032/T033/T034 coordenam `SecurityConfig`/resolver
- US3: T035–T036 em paralelo; T037
- Com equipe: após a Foundational, US1/US2/US3 por devs diferentes, **coordenando os
  arquivos compartilhados** `ContaController.java` (US1/US3) e `SecurityConfig.java` (US2)

---

## Parallel Example: User Story 1

```bash
# Testes da US1 juntos (devem falhar primeiro):
Task: "Teste ProvisionamentoJitIT.java"
Task: "Teste AdminBootstrapIT.java"
Task: "Teste EmailNaoVerificadoIT.java"
Task: "Teste UnicidadeContaIT.java"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1: Setup (firebase-admin, config, FirebaseApp)
2. Phase 2: Foundational (CRÍTICO — entidades, porta+adaptador, filtro, segurança)
3. Phase 3: User Story 1 (auto-cadastro JIT em PENDENTE)
4. **PARAR e VALIDAR**: cadastro cria conta PENDENTE; e-mail não verificado recusado; admin bootstrap; sem duplicidade
5. Deploy/demo se pronto

### Incremental Delivery

1. Setup + Foundational → fundação de identidade pronta
2. US1 → auto-cadastro em PENDENTE (MVP)
3. US2 → identidade isolada + gate de estado (coração da feature)
4. US3 → encerrar sessão
5. Cada story agrega valor sem quebrar a anterior

### Parallel Team Strategy

Após a Foundational: Dev A → US1; Dev B → US2 (sobre a costura/DTO da US1); Dev C → US3.
Coordenar `ContaController.java` (US1/US3) e `SecurityConfig.java` (Foundational/US2).

---

## Notes

- [P] = arquivos diferentes, sem dependência pendente
- [Story] mapeia a tarefa à user story para rastreabilidade
- Verificar que os testes falham antes de implementar (TDD nos caminhos críticos — Princípio III)
- Identidade **sempre** derivada do token verificado, nunca do cliente (Princípios I e VII)
- Nenhuma senha persistida; nenhum segredo no código (Princípios VII e VIII)
- Commit após cada tarefa ou grupo lógico
- Parar em qualquer checkpoint para validar a story isoladamente
