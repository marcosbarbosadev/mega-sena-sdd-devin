# Relatório de Desvios — Implementação vs. Especificações

**Data da auditoria:** 2026-06-17
**Escopo:** Conformidade do backend (`backend/src/...`) com as specs em `specs/001` … `specs/006`.
**Método:** Leitura de cada `spec.md`, `plan.md`, `data-model.md`, `tasks.md`, `contracts/` e `checklists/`, confrontados linha a linha com o código Java, as migrations Flyway (V1–V8) e os testes.

> Este documento é insumo para uma nova sessão de implementação/correção. Cada item traz: o que a spec exige, o que o código faz hoje (com `arquivo:linha`), o impacto e a correção sugerida.

---

## Visão geral (priorização)

| Feature | Conformidade | Críticos | Médios | Baixos |
|---|---|:--:|:--:|:--:|
| 001 — Mega-Sena Sync | Alta | 0 | 1 | 4 |
| 002 — Identity Auth | Alta | 0 | 2 | 3 |
| 003 — Account Approval | Alta | 0 | 3 | 1 |
| **004 — Game Registration** | **Baixa** | **4** | **6** | **2** |
| 005 — Game Checking | Média | 2 | 3 | 2 |
| 006 — Backend Complete (guarda-chuva) | Média-Alta | 0 | 4 | 3 |

**Onde focar primeiro:** a **Feature 004** é a mais grave — viola requisitos centrais (FR-005, FR-008) e uma decisão de clarificação registrada na própria spec. A **Feature 005** depende diretamente da 004 e tem 2 desvios críticos próprios. As demais features estão majoritariamente fiéis; os desvios são códigos de erro fora do contrato, lacunas de teste e documentação dessincronizada.

**Temas transversais recorrentes (aparecem em várias features):**
1. **Códigos de erro divergentes do contrato** — o `GlobalExceptionHandler` emite códigos genéricos (`VALIDACAO`, `NAO_ENCONTRADO`, `VALIDACAO_JOGO`) onde os contratos OpenAPI exigem códigos estáveis específicos (`MOTIVO_OBRIGATORIO`, `CONTA_NAO_ENCONTRADA`, `CADASTRO_PARCIAL`, `SEM_CONCURSO_ABERTO`, `JOGO_BLOQUEADO`).
2. **FKs em tabelas de auditoria** — várias migrations criaram FK de `*_id` para a entidade de origem (`evento_jogo.jogo_id`, `evento_conferencia.jogo_id`) onde o `data-model.md` exige **ausência de FK** para que a trilha sobreviva à exclusão (Princípio V). Isso força `deleteBy...` que apaga a auditoria.
3. **Cobertura de testes abaixo do `tasks.md`** — consolidação de muitos ITs especificados num único arquivo por feature; faltam testes de caminhos críticos obrigatórios (concorrência, provedor indisponível, gate de conta, identidade derivada do servidor).
4. **Documentação dessincronizada** — `data-model.md` da 006, `quickstart.md`, `README` e `plan.md` descrevem rotas, env vars e schema que não batem com o código real.

---

## Feature 001 — Mega-Sena Sync

**Resumo:** Implementação amplamente fiel. Modelo de dados, contratos de leitura/admin, idempotência, lacuna, degradação graciosa, validação FR-010 e carga histórica retomável corretos. 1 desvio médio + 4 baixos.

### [MÉDIO] Autenticação admin não usa o token Bearer de serviço do contrato
- **Spec:** `contracts/admin-api.yaml:74-79` define `adminAuth` como `http bearer` = credencial de serviço **desacoplada do login de usuários**. `research.md:90-99` rejeita explicitamente "acoplamento prematuro". `tasks.md:60` (T017) e `quickstart.md:29,54` usam `megasena.admin.token`.
- **Implementação:** `config/SecurityConfig.java:80` protege `/api/admin/**` com `.hasRole("ADMINISTRADOR")` (identidade Firebase da feature 002), não com o token de serviço. `config/MegaSenaProperties.java:50-55` e `application.yml:32-33` ainda declaram `megasena.admin.token`, mas **nada o consome** (config morta).
- **Impacto:** Operação admin acoplada ao login de usuários — o oposto do que `research.md` decidiu. Config `megasena.admin.token` é enganosa. Não quebra funcionalidade.
- **Correção:** Decidir conscientemente: (a) atualizar `admin-api.yaml`/`research.md`/`quickstart.md`/`README` para o modelo de identidade Firebase, **ou** (b) implementar o filtro do token estático para `/api/admin/sync/**`. Em qualquer caso, remover a config órfã se não for usada. *(Ver também o mesmo item na Feature 006.)*

### [BAIXO] Coluna `dezena` é INT, não TINYINT
- **Spec:** `data-model.md:30`, `tasks.md:48` (T005) → `dezena TINYINT`.
- **Implementação:** `db/migration/V1__create_concurso.sql:12` usa `INT`; `concurso/ConcursoDezena.java:29` usa `Integer`. CHECK 1..60 e UNIQUE corretos.
- **Impacto:** Cosmético. Invariante de range garantida pelo CHECK.
- **Correção:** Alterar para `TINYINT` em nova migração, se quiser fidelidade exata. Baixa prioridade.

### [BAIXO] Propriedades de retry órfãs e com nome divergente
- **Spec:** `tasks.md:38` (T004) e `quickstart.md:27-28` → `megasena.sync.retry.max-attempts` e `megasena.sync.retry.backoff-ms`.
- **Implementação:** `application.yml:30-31` e `MegaSenaProperties.java:38-47` usam `megasena.sync.retry-max-attempts` / `retry-wait-duration` (estrutura plana). Além disso, **não são lidas** pelo Resilience4j — o retry efetivo vem de `resilience4j.retry.instances.caixaApi` (`application.yml:41-52`); as props `megasena.sync.retry-*` ficam mortas.
- **Impacto:** Ajustar `megasena.sync.retry-*` não muda comportamento. Confuso.
- **Correção:** Ligar Resilience4j às props `megasena.*` ou remover as órfãs e documentar `resilience4j.retry.*`.

### [BAIXO] Base-URL da fonte divergente no quickstart
- **Spec:** `quickstart.md:23` lista `.../portaldeloterias/api` (sem `/megasena`); `contracts/caixa-source.md:11` e `research.md:13` usam `.../api/megasena`.
- **Implementação:** `application.yml:24` e `MegaSenaProperties.java:22` usam `.../api/megasena`; `fonte/CaixaSourceClient.java:24` (`.uri("")` / `.uri("/{numero}")`) — coerente com o contrato. Só o quickstart está desatualizado.
- **Correção:** Corrigir `quickstart.md:23`.

### [INFO] Índice de `sync_run` — conforme
- `V2__create_sync_run.sql:14-15` cria `idx_sync_run_status` e `idx_sync_run_iniciado(iniciado_em DESC)`. OK (MySQL pode ignorar a direção DESC, mas o índice existe).

**Conformes:** modelo `concurso`/`concurso_dezena`, modelo `sync_run`, enums `OrigemSync`/`StatusSync`, contrato de leitura (`/api/concursos/latest`, `/{numero}`), contrato admin (status 200/404, run 202, lock 409), validação FR-010 (`ConcursoValidator`), mapeamento dd/MM/yyyy, idempotência (FR-006), imutabilidade (FR-012), preenchimento de lacunas (FR-014), carga histórica retomável (FR-005), lock de execução única, degradação graciosa (FR-007), retry/backoff (FR-009), cron (FR-003), headers HTTP, logging estruturado, e a suíte de ITs/UTs completa.

---

## Feature 002 — Identity Auth

**Resumo:** Fiel no essencial (modelo, porta/adaptador Firebase, provisionamento JIT, bootstrap admin, gate de estado, endpoints `/api/auth/me`, `/api/auth/logout`, `/api/perfil`). Desvios em auditoria incompleta e cobertura de testes.

### [MÉDIO] Auditoria de autenticação negada (`sucesso=false`) não é registrada
- **Spec:** `data-model.md:81-82` e FR-011 — evento `AUTENTICACAO` deve cobrir falha de verificação (`sucesso=false`, `usuario_id` nulo, `motivo=TOKEN_INVALIDO`). `research.md:R8` reforça.
- **Implementação:** `config/IdentidadeTokenFilter.java:44-46` apenas faz `log.debug` no catch de `IdentidadeInvalidaException`; não chama `AuditoriaIdentidadeService`. `identidade/ResolvedorDeContaImpl.java:31` só registra `AUTENTICACAO` no sucesso.
- **Impacto:** Trilha perde todas as tentativas negadas (FR-011).
- **Correção:** No catch de `IdentidadeInvalidaException` do filtro, gravar `EventoIdentidade` `AUTENTICACAO` com `sucesso=false`, `usuario_id` nulo, `motivo=TOKEN_INVALIDO`.

### [MÉDIO] Cobertura de testes muito abaixo do `tasks.md`
- **Spec:** `tasks.md` exige (TDD, Princípio III) T023 `UnicidadeContaIT`, T027 `AcessoAtivoIT`, T029 `AutenticacaoInvalidaIT`, T030 `ProvedorIndisponivelIT`, T031 `IdentidadeDoServidorIT`, T035 `LogoutIT`, T036 `TokenRevogadoIT`.
- **Implementação:** Só existem 4 ITs em `src/test/.../identidade/`: `ProvisionamentoJitIT`, `AdminBootstrapIT`, `EmailNaoVerificadoIT`, `AcessoBloqueadoIT`. O dublê já suporta `simularIndisponibilidade` e `getSessoesRevogadas` (`VerificadorDeIdentidadeFake.java:32,36`), mas nenhum teste os usa.
- **Impacto:** FR-013 (provedor indisponível/503), FR-010 (logout/revogação), FR-005 (identidade do servidor) sem verificação automatizada.
- **Correção:** Adicionar os ITs faltantes (mínimo: 503 provedor indisponível, logout com revogação registrada, token revogado→401, isolamento por `usuarioId`).

### [BAIXO] `correlation_id` nunca é preenchido
- **Spec:** `data-model.md:70` define a coluna; FR-011/Princípio V pedem correlação por requisição.
- **Implementação:** `identidade/AuditoriaIdentidadeService.java:16-24` não recebe nem grava `correlationId`; coluna existe (`EventoIdentidade.java:41-42`) mas é sempre NULL.
- **Correção:** Propagar correlation id (ex.: MDC) para `registrar(...)`.

### [BAIXO] `ACESSO_NEGADO` lê principal sem guarda de nulidade
- **Spec:** `data-model.md:84-86`, T034 — registrar via `AccessDeniedHandler` com `motivo`.
- **Implementação:** `config/SecurityConfig.java:51-69` implementa e registra `ACESSO_NEGADO` (CONTA_PENDENTE/CONTA_REPROVADA) — **conforme**. Observação: lê `getAuthentication()` (linha 52) sem null-check; risco de NPE em borda. Nenhum teste cobre REPROVADO (só PENDENTE em `AcessoBloqueadoIT`).
- **Correção:** Guarda de null + teste para conta REPROVADA.

### [BAIXO] Corpo de erro do filtro montado como JSON manual
- **Spec:** T017/T019 — filtro mapeia 503/403; handler global mapeia o resto.
- **Implementação:** `IdentidadeTokenFilter.java:46-57` escreve JSON 503/403 manualmente (fora do schema `Erro`/Jackson); `GlobalExceptionHandler.java:36-46` repete para a camada de controller. Funciona, mas duplicado.
- **Correção:** Extrair writer comum para os corpos de erro do filtro.

**Conformes:** migrations V3/V4 (colunas, enums, uniques `uk_usuario_provider_uid`/`uk_usuario_email`, FK, índices), entidades e enums (`EstadoConta`/`Papel`/`MetodoLogin`/`TipoEvento`), sem coluna de senha (SC-004), porta `VerificadorDeIdentidade` + adaptador Firebase (`verifyIdToken(token,true)`, `revokeRefreshTokens`, mapeamento de provider), provisionamento JIT + bootstrap + unicidade idempotente, gate de estado (papéis só p/ ATIVO), identidade derivada do token, contratos `/api/auth/me`, `/api/auth/logout` (204+revogação+auditoria), `/api/perfil`, 401 genérico (SC-006), setup Firebase condicional.

---

## Feature 003 — Account Approval

**Resumo:** Majoritariamente fiel (modelo, 3 endpoints, transição atômica condicionada FR-005/FR-008, autorização admin-only, motivo obrigatório, identidade do admin pelo token, auditoria). 2 desvios de código de erro + 1 lacuna de teste.

### [MÉDIO] Código de erro de conta inexistente: `NAO_ENCONTRADO` em vez de `CONTA_NAO_ENCONTRADA`
- **Spec:** `contracts/admin-moderation-api.yaml:148` exige `codigo: CONTA_NAO_ENCONTRADA` (404); `tasks.md:47` (T008).
- **Implementação:** `config/GlobalExceptionHandler.java:48-52` mapeia `ContaNaoEncontradaException` → `errorBody(404, "NAO_ENCONTRADO", ...)`. Status 404 correto, `codigo` errado.
- **Impacto:** Clientes que dependem do código estável do contrato não reconhecem a condição.
- **Correção:** `errorBody(404, "CONTA_NAO_ENCONTRADA", ex.getMessage())`.

### [MÉDIO] Código de erro de motivo obrigatório: `VALIDACAO` em vez de `MOTIVO_OBRIGATORIO`
- **Spec:** `contracts/admin-moderation-api.yaml:188-197` define 400 com `codigo: MOTIVO_OBRIGATORIO`; `tasks.md:47` (T008), `research.md:82`.
- **Implementação:** `config/GlobalExceptionHandler.java:72-80` trata `MethodArgumentNotValidException` genericamente como `errorBody(400, "VALIDACAO", ...)`. O teste `ModeracaoIT.reprovarSemMotivoRetorna400` só checa o status, então não pega o desvio.
- **Correção:** Para o endpoint de reprovação, emitir `MOTIVO_OBRIGATORIO` (tratar o campo `motivo` especificamente ou mapear pelo código de erro do binding).

### [MÉDIO] Cobertura de testes abaixo do `tasks.md` / Princípio III
- **Spec:** `tasks.md` exige T017 (acesso pós-aprovação FR-006/SC-005), T019 (concorrência FR-008/SC-006), T020 (404 conta inexistente), T020a (identidade do admin do servidor ignorando `adminId` espúrio FR-009), T025 (acesso pós-reprovação 403), T026 (reprovar não-pendente 409) e asserção de que `decisao_moderacao` grava `admin_id`/`motivo` (FR-007/SC-003).
- **Implementação:** Apenas `moderacao/ModeracaoIT.java` (6 testes): listar pendentes, aprovar→ATIVO, reprovar→REPROVADO, reprovar sem motivo→400, aprovar já-aprovada→409, não-admin→403. Faltam concorrência, 404, gate pós-decisão, identidade do admin e asserção da linha de auditoria.
- **Impacto:** FR-007/FR-008/FR-009 e SC-003/SC-005/SC-006 sem verificação. Os 2 desvios de código de erro acima passam despercebidos por essa lacuna.
- **Correção:** Adicionar os testes T016–T026.

### [BAIXO] Dois timestamps distintos para a decisão
- **Spec:** `data-model.md:41` — UPDATE e gravação da `DecisaoModeracao` na mesma transação; `criado_em` = momento da decisão. `tasks.md:42` (T003) prevê `@PrePersist`.
- **Implementação:** `moderacao/DecisaoModeracao.java:40-43` usa `Instant.now()` no `@PrePersist`, enquanto `ModeracaoService.java:35,52` usa outro `Instant.now()` para o UPDATE — diferença de microssegundos entre `usuario.atualizado_em` e `decisao_moderacao.criado_em`.
- **Correção:** Opcional — passar o mesmo `Instant` para ambos.

**Conformes:** migration V5 (colunas, FKs, enum `decisao`, `motivo`, índices, CHECK), entidade e enum `Decisao`, endpoints (`GET /api/admin/contas/pendentes`, `POST /{id}/aprovar`, `POST /{id}/reprovar` com 204), `ContaPendenteResponse`, transição condicionada (`transicionarDePendente` com `WHERE estado='PENDENTE'` → 0 linhas → 409, 404 antes via `findById`), autorização `/api/admin/**`, motivo obrigatório (`@NotBlank` + CHECK), ausência de PATCH de estado (FR-010), identidade do admin via `@AuthenticationPrincipal`.

---

## Feature 004 — Game Registration ⚠️ (MAIS GRAVE)

**Resumo:** Estrutura geral presente e caminho feliz funciona, mas **NÃO seguiu fielmente a spec**: 4 desvios críticos que violam FR-005/FR-008 e uma decisão de clarificação registrada (2026-06-11), além de 6 médios. **Priorizar esta feature.**

### [CRÍTICO] `concursoNumero` vem do cliente; não há resolução do "próximo concurso em aberto"
- **Spec:** FR-005 + `data-model.md` — o servidor vincula o jogo ao próximo concurso em aberto (`MAX(concurso.numero)+1`), snapshot imutável; cliente nunca escolhe (Out of Scope: "Escolha manual de qual concurso apostar"). `tasks.md` T010 exigia `ConcursoAbertoService.proximoEmAberto()` e `ConcursoRepository.findMaxNumero()`.
- **Implementação:** `jogo/JogoRequest.java:8` recebe `concursoNumero` do corpo e grava direto (`jogo/JogoService.java:64`). Não existem `ConcursoAbertoService` nem `findMaxNumero()`. Teste confirma cliente enviando `"concursoNumero":9999` (`JogoIT.java:195`).
- **Impacto:** Usuário aposta em qualquer concurso arbitrário (até passados/inexistentes). Quebra a base da conferência (005).
- **Correção:** Remover `concursoNumero` do `JogoRequest`; criar `ConcursoAbertoService.proximoEmAberto()` (= max+1, senão `SemConcursoAbertoException`) e atribuir no cadastro.

### [CRÍTICO] Edição permite re-vincular o concurso
- **Spec:** FR-008 + clarificação 2026-06-11 — edição altera dezenas/quantidade mantendo o mesmo concurso (não re-vincula).
- **Implementação:** `jogo/JogoService.java:107-109` reescreve o concurso na edição (`if (request.concursoNumero() != null) jogo.setConcursoNumero(...)`).
- **Impacto:** Viola imutabilidade do snapshot; dono move aposta para outro concurso.
- **Correção:** Remover o bloco; edição preserva `concursoNumero`.

### [CRÍTICO] Cadastro sem concurso sincronizado não tratado (sem 409 `SEM_CONCURSO_ABERTO`)
- **Spec:** Edge case + contrato (409 `SEM_CONCURSO_ABERTO`); `tasks.md` T016. Não existe `SemConcursoAbertoException` (T007).
- **Implementação:** Como o número vem do cliente, não há verificação. A FK `fk_jogo_concurso` (V6) faz um número inexistente gerar **500 genérico**, não 409.
- **Correção:** Implementar resolução de concurso aberto com 409 quando não houver dados.

### [CRÍTICO] Modelo de "editável" e FK incompatíveis com data-model (R3)
- **Spec:** `data-model.md` R3 — `editavel = NÃO EXISTE concurso WHERE numero = jogo.concurso_numero`; `concurso_numero` é INT fixo **NÃO-FK** (`data-model.md:19`, T002).
- **Implementação:** `V6__create_jogo.sql:12` cria FK `concurso_numero → concurso(numero)` e a coluna é `NULL` (linha 4). `verificarEditavel` baseia-se em "tem dezenas sorteadas" (`JogoService.java:152-156`) e ainda aceita `concursoNumero == null`.
- **Impacto:** Semântica diferente da spec; a FK impede gravar o "próximo em aberto" ainda não sincronizado, tornando o design da spec inviável.
- **Correção:** Remover a FK de `concurso_numero`, torná-lo `NOT NULL`, derivar `editavel` por existência da linha do concurso (R3).

### [MÉDIO] Endpoint `GET /api/jogos/{id}` ausente
- **Spec:** `contracts/jogos-api.yaml` define `GET /jogos/{id}` (200/404); T026/T030.
- **Implementação:** `jogo/JogoController.java:29-53` só tem POST, GET (lista), PUT, DELETE.
- **Correção:** Adicionar `GET /api/jogos/{id}` via `findByIdAndUsuarioId` (não-dono → 404).

### [MÉDIO] `JogoResponse` não expõe `editavel` (obrigatório no schema)
- **Spec:** schema `Jogo` exige `editavel` (`contracts` linha 54, `data-model` linha 97).
- **Implementação:** `jogo/JogoResponse.java:7-14` não tem o campo.
- **Correção:** Adicionar `editavel` calculado pelo estado do concurso vinculado.

### [MÉDIO] Persistência de `tipo_selecao` contraria clarificação
- **Spec:** clarificação 2026-06-11 + `data-model.md` ("Sem campo de origem") — origem informado/gerado NÃO deve ser persistida.
- **Implementação:** `tipo_selecao ENUM('MANUAL','AUTOMATICO') NOT NULL` (`V6:5`), enum `TipoSelecao`, campo em `Jogo.java:34-36`, exposto em `JogoResponse.java:10`.
- **Correção:** Remover `tipo_selecao` do schema, entidade e response.

### [MÉDIO] Migrations divergem do data-model
- **Spec:** V6 — `jogo.quantidade TINYINT` com `CHECK (quantidade BETWEEN 6 AND 9)`; `jogo_dezena.dezena TINYINT`, sem coluna `id` (PK `(jogo_id,dezena)`). V7 — `evento_jogo.jogo_id` **sem FK** (`data-model` 75/85, T002/T003); índice `idx_evento_jogo_data (tipo, criado_em)`.
- **Implementação:** `jogo` sem coluna `quantidade` nem CHECK 6–9; `jogo_dezena` usa `id BIGINT AUTO_INCREMENT` e `dezena INT` (`V6:16-18`); `evento_jogo.jogo_id` **tem FK** `fk_evento_jogo_jogo → jogo(id)` (`V7:10`) e falta o índice `(tipo, criado_em)`.
- **Impacto:** A FK força o `excluir()` a apagar eventos antes do jogo (`JogoService.java:127`) — a auditoria histórica é destruída (viola FR-010/Princípio V).
- **Correção:** Alinhar migrations; remover FK de `evento_jogo.jogo_id`; preservar eventos.

### [MÉDIO] Auditoria de EXCLUSÃO não é registrada
- **Spec:** FR-010 / `data-model` — toda operação (incl. EXCLUSAO) gera linha em `evento_jogo`; T036.
- **Implementação:** `jogo/JogoService.java:121-129` apaga eventos e o jogo, sem `registrarEvento(..., EXCLUSAO)`.
- **Correção:** Registrar `EXCLUSAO` (sem FK que impeça a sobrevivência do registro).

### [MÉDIO] Códigos de erro e status divergem do contrato
- **Spec:** `VALIDACAO`/`CADASTRO_PARCIAL` (400), `SEM_CONCURSO_ABERTO` (409), `JOGO_BLOQUEADO` (409), `NAO_ENCONTRADO` (404).
- **Implementação:** `VALIDACAO_JOGO` (`GlobalExceptionHandler.java:63`); cadastro parcial cai em `JogoValidacaoException`→`VALIDACAO_JOGO` em vez de `CADASTRO_PARCIAL` (`JogoService.java:44-48`); `JOGO_NAO_EDITAVEL` em vez de `JOGO_BLOQUEADO` (linha 69); `SEM_CONCURSO_ABERTO` inexistente.
- **Correção:** Alinhar códigos e separar a exceção de cadastro parcial.

### [BAIXO] Fonte de aleatoriedade não injetável (R4)
- **Spec:** `research.md` R4 / T022 — `RandomGenerator` injetável por construtor para testes determinísticos.
- **Implementação:** `jogo/FonteAleatoriedadeImpl.java:14` instancia `new SecureRandom()` internamente. Testes usam fake separado (mitiga parcialmente).
- **Correção:** Injetar `RandomGenerator` por construtor no adaptador real.

### [BAIXO] Nomenclatura divergente das tasks
- **Spec:** `CadastroJogoRequest`, `ApostaInvalidaException`, `SemConcursoAbertoException`, `JogoBloqueadoException`, `ApostaValidator`, `GeradorDeDezenas`, `ConcursoAbertoService`, ITs separados por story.
- **Implementação:** `JogoRequest`, `JogoValidacaoException`, `JogoNaoEditavelException`, validação inline, `FonteAleatoriedade`, único `JogoIT.java`.
- **Correção:** Opcional — alinhar nomes; mínimo, garantir cobertura equivalente.

**Conformes:** pacote `jogo/` com isolamento por dono (`findByIdAndUsuarioId`, não-dono→404), `usuarioId` do token (FR-006), gate de conta ATIVA (`/api/jogos/**`→`ROLE_USUARIO`), validação 6–9 dezenas / 1–60 / sem repetição (FR-004), dois modos de cadastro e rejeição de ambos (FR-001/002/003 parcial), geração de N dezenas distintas, listagem isolada (FR-007), dezenas ordenadas, endpoints POST/GET-lista/PUT/DELETE com 201/200/200/204.

---

## Feature 005 — Game Checking

**Resumo:** Núcleo correto (acertos por interseção, faixas, `premiado`, endpoints, isolamento, idempotência por UNIQUE). 2 desvios críticos (regra "aguardando sorteio" e contrato de resposta) + 3 médios. Depende da correção da 004.

### [CRÍTICO] Critério de `AGUARDANDO_SORTEIO` diverge da spec (concurso inexistente vira 404)
- **Spec:** FR-005 + R2 (`research.md:27-41`, `data-model.md:29-30`) — `status = AGUARDANDO_SORTEIO` quando **NÃO existe** concurso com `numero = jogo.concurso_numero`. Edge case (`spec.md:84-85`).
- **Implementação:** `conferencia/ConferenciaService.java:43-44` lança `ResourceNotFoundException` → **404** quando o concurso não existe; `AGUARDANDO_SORTEIO` só retorna quando o concurso existe mas tem dezenas vazias (linhas 51-56). O teste `conferirConcursoAguardandoSorteio` (`ConferenciaIT.java:132-139`) passa porque o setup cria concurso 1001 existente-sem-dezenas (`ConferenciaIT.java:81-85`), **mascarando** o desvio.
- **Impacto:** Jogo cujo concurso ainda não foi ingerido retorna 404 em vez de 200 AGUARDANDO_SORTEIO (FR-005/SC-004).
- **Correção:** Em `conferir`, tratar `findByNumero(...)` vazio retornando `AGUARDANDO_SORTEIO` (como já faz `conferirSemRegistro` na linha 86). Adicionar teste com concurso inexistente.

### [CRÍTICO] Contrato de resposta diverge de `conferencia-api.yaml`
- **Spec:** schema `Conferencia` (`conferencia-api.yaml:28-51`): campos `jogoId`, `concursoNumero`, `status` (enum `CONFERIDO|AGUARDANDO_SORTEIO`); `acertos`/`faixa`/`premiado` **só quando CONFERIDO**. Sem dezenas no payload.
- **Implementação:** `conferencia/ConferenciaResponse.java:6-15` adiciona `dezenasJogadas`/`dezenasSorteadas` (não previstos), usa `status` como String livre, e **sempre** preenche `acertos/faixa/premiado` — no AGUARDANDO_SORTEIO envia `acertos=0, faixa=NENHUMA, premiado=false` (`ConferenciaService.java:52-55`).
- **Impacto:** Cliente recebe campos espúrios; `acertos=0/não premiado` confundível com jogo já conferido.
- **Correção:** Alinhar ao schema (remover dezenas ou atualizar contrato deliberadamente); omitir `acertos/faixa/premiado` quando AGUARDANDO_SORTEIO; usar enum `StatusConferencia`.

### [MÉDIO] Artefatos exigidos ausentes (`Conferencia` record, `StatusConferencia`, `CalculadoraConferencia`)
- **Spec:** T001/T005/T006 (`tasks.md:31,44-45`), `plan.md:112-113` — enum `StatusConferencia`, record `Conferencia`, classe pura `CalculadoraConferencia` (núcleo testável sem I/O, R1/Princípio II).
- **Implementação:** Nenhum dos três existe. Status é String (`ConferenciaResponse.java:14`); cálculo inline e **duplicado** em `conferir` (`ConferenciaService.java:58-61`) e `conferirSemRegistro` (95-98).
- **Impacto:** Sem unidade pura para teste obrigatório (T009); risco de divergência entre as duas cópias.
- **Correção:** Extrair `CalculadoraConferencia` pura, reutilizar nos dois caminhos; introduzir `StatusConferencia`.

### [MÉDIO] Teste unitário puro do cálculo (T009) ausente
- **Spec:** T009 (`tasks.md:61`), `plan.md:69-70`, `quickstart.md:95-99` — cobertura obrigatória de 6, 7, 8 e 9 dezenas via teste puro.
- **Implementação:** Só `ConferenciaIT.java` (integração); 9 dezenas em 1 teste de integração (linhas 163-171); sem teste puro nem cobertura sistemática de 7/8 dezenas e todas as faixas.
- **Correção:** Adicionar `CalculadoraConferenciaTest` cobrindo 6–9 dezenas e todas as faixas/limiar de premiado.

### [MÉDIO] Migration V8 diverge do data-model (FK em `jogo_id`, tipo de `acertos`, sem CHECK)
- **Spec:** `data-model.md:44-59`, T002 — `acertos TINYINT`; `jogo_id` **sem FK** (robustez histórica, espelha `evento_jogo`); `CHECK (acertos BETWEEN 0 AND 6)`; FK só para `usuario`.
- **Implementação:** `V8__create_evento_conferencia.sql` usa `acertos INT` (linha 6); adiciona `fk_conferencia_jogo → jogo(id)` (linha 13, **proibido**) e `fk_conferencia_concurso → concurso(numero)` (linha 14, não previsto); **sem** o CHECK.
- **Impacto:** A FK em `jogo_id` exige `deleteByJogoId` (`EventoConferenciaRepository.java:17`), que apaga a auditoria ao excluir o jogo — contraria a trilha permanente (Princípio V).
- **Correção:** Remover FKs de `jogo_id`/`concurso_numero`, adicionar CHECK, usar TINYINT; revisar `deleteByJogoId` (auditoria deve persistir).

### [BAIXO] `GET /api/conferencias` não grava auditoria (inconsistente com o item e o contrato)
- **Spec:** `conferencia-api.yaml:114-118` — conferências de jogos já sorteados gravam (idempotente) a auditoria também na listagem; R6 pede consistência item/lista. T018 (`tasks.md:89`) manda a lista reusar `conferir`.
- **Implementação:** `listarConferencias` usa `conferirSemRegistro` (`ConferenciaService.java:71-104`), que não grava `evento_conferencia`; só o endpoint de item grava.
- **Correção:** Fazer a listagem reutilizar `conferir` (gravação idempotente).

### [BAIXO] Granularidade de testes e gate de conta
- **Spec:** T008–T017 preveem ITs separados; T014 exige teste de gate.
- **Implementação:** Tudo em `ConferenciaIT.java`; **não há teste de gate** (PENDENTE/REPROVADO→403, sem token→401) nas rotas de conferência, apesar de o `SecurityConfig` cobrir a rota.
- **Correção:** Acrescentar teste de gate para `/api/conferencias` e `/api/jogos/{id}/conferencia`.

**Conformes:** endpoints `GET /api/jogos/{id}/conferencia` e `GET /api/conferencias`, `usuarioId` do token (FR-007), isolamento via `findByIdAndUsuarioId`→404 cruzado, cálculo de acertos (máx. 6 inclusive p/ 7–9 dezenas), faixas SENA/QUINA/QUADRA/NENHUMA + `premiado = acertos≥4`, auditoria idempotente (UNIQUE `(jogo_id, concurso_numero)` + check-then-insert), gate `ROLE_USUARIO`, enum `Faixa`, só-leitura sobre jogo/concurso (FR-010).

---

## Feature 006 — Backend Complete (guarda-chuva)

**Resumo:** Spec consolida 001–005. Código funcional e bem estruturado; a maioria dos requisitos transversais tem implementação real (Spring Security, gate de estado, `GlobalExceptionHandler`, isolamento, Flyway, Resilience4j, Testcontainers+WireMock). Desvios concentram-se em **documentação dessincronizada**, **config morta** e **tasks marcadas como concluídas sem implementação**.

### [MÉDIO] `MEGASENA_ADMIN_TOKEN` é config morta — admin sync usa papel Firebase
- **Spec:** `plan.md:131-132,207`, `quickstart.md:122-128`, `README.md:23,61` mostram `/api/admin/sync/status` e `/run` com `Authorization: Bearer $MEGASENA_ADMIN_TOKEN`.
- **Implementação:** Propriedade existe (`MegaSenaProperties.java:50-55`, `application.yml:32-33`) mas **não é referenciada** por nenhuma autenticação. `/api/admin/**` exige `hasRole("ADMINISTRADOR")` (`SecurityConfig.java:80`) — JWT Firebase de ADMINISTRADOR ATIVO. Valor `changeme` dá falsa sensação de segurança.
- **Impacto:** Documentação enganosa (quickstart/README não funcionam); confusão no deploy. *(Mesmo item da Feature 001.)*
- **Correção:** Remover a propriedade/env var da doc, **ou** implementar o filtro do token estático. Alinhar doc ao modelo Firebase.

### [MÉDIO] Dockerfile inexistente (Tarefa 7.4 marcada `[x]`)
- **Spec:** `tasks.md:325-330` marca Dockerfile multi-stage, build, health checks, execução em container; `plan.md:212`, `research.md:386-392`.
- **Implementação:** Nenhum Dockerfile no repositório.
- **Correção:** Criar o Dockerfile multi-stage ou desmarcar as tasks 7.4 e ajustar a doc de deploy.

### [MÉDIO] Health checks / Actuator / Prometheus ausentes (Tarefa 7.1 marcada `[x]`)
- **Spec:** `tasks.md:308`, `plan.md:213-214`, `quickstart.md:336-343`, `research.md:362-370` — `/actuator/health` e `/actuator/prometheus`.
- **Implementação:** `pom.xml` sem `spring-boot-starter-actuator` nem `micrometer-registry-prometheus`; `application.yml` sem bloco `management`. Endpoints não existem e seriam barrados por `anyRequest().denyAll()` (`SecurityConfig.java:84`).
- **Correção:** Adicionar actuator + micrometer-prometheus, configurar `management.endpoints`, liberar `/actuator/health`.

### [MÉDIO] `data-model.md` substancialmente divergente do schema real
- **Spec (data-model.md):** `usuario.id` BIGINT com `firebase_uid`; `papel ENUM('USUARIO','ADMIN')`; `evento_identidade.tipo_evento ENUM('CADASTRO','AUTH_SUCCESS','AUTH_FAILED','LOGOUT')`; `jogo.tipo_selecao ENUM('INFORMADO','GERADO')`; idempotência de conferência por `(usuario_id, jogo_id)`; `concurso` com `id` separado de `numero`; `concurso_dezena` PK `(concurso_id, dezena)`; migrações `V1__create_initial_schema`/etc.
- **Implementação (real):** `usuario.id` `BINARY(16)` UUID com `provider_uid` (`V3`, `Usuario.java:20-24`); `papel ENUM('USUARIO','ADMINISTRADOR')`; `evento_identidade.tipo ENUM('CADASTRO','AUTENTICACAO','LOGOUT','ACESSO_NEGADO')` (`V4`); `jogo.tipo_selecao ENUM('MANUAL','AUTOMATICO')` (`V6`); idempotência por `UNIQUE (jogo_id, concurso_numero)` (`V8`); `concurso.numero` é PK direto; `concurso_dezena` PK `id` + UNIQUE `(concurso_numero, dezena)`; migrações nomeadas por tabela `V1__create_concurso`…`V8`.
- **Impacto:** `data-model.md` não é referência confiável (o schema real é coerente e até melhor — UUID, checks).
- **Correção:** Reescrever `data-model.md` a partir das migrations V1–V8 reais.

### [BAIXO] Rotas documentadas (plan/quickstart) divergem das implementadas
- **Spec:** `POST /api/contas`, `GET /api/perfil`, `POST /api/logout`, `/api/admin/moderacao/{id}/aprovar|reprovar`, `/api/conferencia/{jogoId}`, `/api/conferencia`.
- **Implementação:** auto-cadastro JIT no filtro (sem `POST /api/contas`); `/api/auth/me` e `/api/auth/logout` (`ContaController.java`); moderação em `/api/admin/contas/{id}/...` (`ModeracaoController.java`); conferência em `/api/jogos/{id}/conferencia` e `/api/conferencias`.
- **Correção:** Atualizar a tabela de endpoints do `plan.md` e exemplos do `quickstart`/`README`.

### [BAIXO] `/api/perfil` e `PerfilController` redundantes / inconsistentes com `/api/auth/me`
- **Spec:** endpoint de "dados da própria conta" (`plan.md:101`); PENDENTE deve poder ver o estado da própria conta.
- **Implementação:** dois endpoints com o mesmo `ContaResponse`: `GET /api/auth/me` (`.authenticated()`, `SecurityConfig.java:79`) e `GET /api/perfil` (`hasRole("USUARIO")`, linha 81 + `PerfilController.java`). Como USUARIO só é concedido a ATIVO, `/api/perfil` fica inacessível a PENDENTE enquanto `/api/auth/me` não — comportamentos divergentes para a mesma necessidade.
- **Correção:** Consolidar num único endpoint; decidir se PENDENTE pode consultá-lo (recomendado: sim) e remover o redundante.

### [BAIXO] Regra `/api/conferencias/**` não cobre a conferência individual
- **Spec:** NFR-004/FR-056 — conferência exige conta ATIVA.
- **Implementação:** `SecurityConfig.java:83` protege `/api/conferencias/**`, mas a conferência individual está em `/api/jogos/{id}/conferencia` — coberta por acaso via `/api/jogos/**` (linha 82). Sem furo atual, mas frágil.
- **Correção:** Tornar a regra explícita para ambos os padrões ou unificar o prefixo.

**Conformes:** stack Spring Boot 3.3.5 + Java 21 + MySQL/Flyway + Security/JWT Firebase + Resilience4j; `GlobalExceptionHandler` global com corpo `{status, codigo, mensagem}`; gate de estado (papéis só p/ ATIVO, `accessDeniedHandler` audita `ACESSO_NEGADO`); isolamento multiusuário via `@AuthenticationPrincipal`; auditoria (`evento_identidade`/`evento_jogo`/`evento_conferencia`/`decisao_moderacao`/`sync_run`); config externalizável; resiliência/degradação (Firebase condicional, 503); testes com Testcontainers+WireMock; agendamento configurável; integridade de schema (checks, uniques de idempotência).

---

## Plano de ataque sugerido para a próxima sessão

**Bloco 1 — Crítico (Feature 004 + 005, comportamento):**
1. Criar `ConcursoAbertoService.proximoEmAberto()` + `ConcursoRepository.findMaxNumero()`; remover `concursoNumero` do `JogoRequest`; resolver concurso no servidor (FR-005).
2. Remover re-vínculo na edição (FR-008).
3. `SemConcursoAbertoException` → 409 `SEM_CONCURSO_ABERTO`.
4. Remover FKs de `evento_jogo.jogo_id` e `evento_conferencia.jogo_id`/`concurso_numero`; preservar/gravar auditoria de EXCLUSÃO; rever `deleteByJogoId`.
5. Corrigir regra `AGUARDANDO_SORTEIO` (concurso inexistente → 200, não 404) + teste real.
6. Alinhar `ConferenciaResponse` ao contrato; extrair `CalculadoraConferencia` pura (deduplicar).

**Bloco 2 — Médio (contratos e cobertura):**
7. Padronizar códigos de erro no `GlobalExceptionHandler` conforme cada contrato (`CONTA_NAO_ENCONTRADA`, `MOTIVO_OBRIGATORIO`, `CADASTRO_PARCIAL`, `JOGO_BLOQUEADO`, `VALIDACAO`).
8. Adicionar `editavel` ao `JogoResponse`; `GET /api/jogos/{id}`.
9. Registrar auditoria de autenticação negada (Feature 002).
10. Preencher as lacunas de teste obrigatórias (002, 003, 004, 005).
11. Decidir o modelo de auth admin (token estático vs. papel Firebase) e remover config morta.
12. Adicionar Actuator/health + Dockerfile (ou desmarcar as tasks 7.x).

**Bloco 3 — Baixo (doc e ajustes finos):**
13. Reescrever `006/data-model.md` a partir das migrations reais; corrigir rotas no `plan.md`/`quickstart`/`README`; consolidar `/api/perfil` vs `/api/auth/me`; TINYINT vs INT; props de retry; base-url do quickstart.