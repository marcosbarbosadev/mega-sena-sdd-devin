# Implementation Plan: Conferência Automática de Jogos

**Branch**: `005-game-checking` | **Date**: 2026-06-12 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-game-checking/spec.md`

## Summary

Conferir os jogos do usuário (004) contra o resultado oficial do concurso vinculado
(001), exibindo **acertos** (interseção com as 6 dezenas sorteadas), **faixa**
(sena/quina/quadra) e **premiado** (≥ 4 acertos), **sem valores monetários**. A
conferência é **computada sob demanda** de forma determinística (não materializada
como dado de domínio) e só fica disponível quando o concurso vinculado já existe na
base da 001 (sorteado); antes disso o jogo aparece como **aguardando sorteio**. Cada
conferência de um jogo gera **uma** linha de auditoria por `jogo × concurso`
(idempotente — Princípio V). Tudo isolado por usuário (Princípio I) e restrito a
conta **ativa** (gate da 002). Entrega o **backend**; a UI Angular é incremento
posterior (precedente 001/002/003/004).

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.3.x (Web, Data JPA, Validation, Security),
Flyway (+ flyway-mysql), MySQL Connector/J. **Sem novas dependências externas**:
lê os jogos da 004 e os concursos/dezenas da 001; identidade/gate vêm da 002. Build:
Maven. Testes: JUnit 5, Mockito, Testcontainers (MySQL), `spring-security-test` +
verificador de identidade dublê da 002.

**Storage**: MySQL. Uma migração nova `V8__create_evento_conferencia.sql` (trilha de
auditoria), continuando `V1`/`V2` (001), `V3`/`V4` (002), `V5` (003) e `V6`/`V7`
(004). **Nenhuma tabela materializa o resultado** da conferência (FR-011).

**Testing**: JUnit 5 + Mockito (unidade: cálculo de acertos/faixa/premiado para
jogos de 6–9 dezenas — cobertura **obrigatória** pela constituição; idempotência;
estado aguardando-sorteio); Testcontainers MySQL (integração: leitura cruzada
jogo×concurso, isolamento por `usuario_id`, gravação única do evento de auditoria);
`spring-security-test` + dublê (gate de conta ATIVA).

**Target Platform**: Servidor Linux (containerizado)

**Project Type**: Web application (monorepo). Implementa o **backend** (`backend/`).
A apresentação da conferência (UI Angular) é incremento posterior.

**Performance Goals**: Volume baixo. A conferência de um jogo é uma interseção de
conjuntos pequenos (≤ 9 vs 6) após uma leitura indexada do concurso e das dezenas;
a listagem confere os jogos do usuário (consulta por `usuario_id`). Sem metas
especiais além de resposta praticamente imediata.

**Constraints**: Determinística e **idempotente** (Princípio II); **sob demanda**,
sem materializar resultado (FR-011); isolada por `usuario_id` derivado do token,
nunca do cliente (Princípio I); acesso exige conta ATIVA (gate da 002 — Princípio
IX); somente leitura sobre jogo e concurso (FR-010); auditoria única por
`jogo × concurso` (Princípio V). **Nota de design**: os endpoints de conferência são
`GET`; a gravação da auditoria é um efeito colateral **deliberado e idempotente**
(UNIQUE `jogo_id, concurso_numero`) — prefetch/retries produzem no máximo uma linha.
Sem mutação de domínio (ver research.md R6).

**Scale/Scope**: Uma tabela de auditoria (`evento_conferencia`) + ~2 endpoints REST.
Sem cache distribuído.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Aderência neste plano |
|-----------|----------------------|
| I. Isolamento Multiusuário (NÃO-NEGOCIÁVEL) | A conferência opera sobre jogos resolvidos por `findByIdAndUsuarioId`/`findByUsuarioId` (filtro pelo `usuarioId` do `UsuarioAutenticado`); acesso cruzado → 404. Nenhum jogo de outro usuário é conferido/exposto (FR-007). ✅ |
| II. Integridade dos Dados de Sorteio | Conferência **determinística e idempotente** (FR-006): `acertos = |jogo.dezenas ∩ concurso.dezenas|`; faixa/premiado derivados; **cobertura de testes obrigatória** para o cálculo de acertos (quadra/quina/sena), conforme a constituição. Somente leitura sobre o sorteio (FR-010). ✅ |
| III. Qualidade via Testes Pragmáticos | Cobertura obrigatória: cálculo de acertos/faixa/premiado (6–9 dezenas), idempotência, estado aguardando-sorteio, isolamento e gate de conta ativa. ✅ |
| IV. Resiliência da Integração | Sem nova integração externa; lê tabelas locais (concurso/jogo). — |
| V. Observabilidade & Auditoria | `evento_conferencia` registra a conferência (quem, jogo, concurso, resultado, quando), **única por jogo×concurso** (FR-011). Logging estruturado correlacionado. ✅ |
| VI. Simplicidade & YAGNI | Resultado **derivado, não materializado**; reaproveita jogos (004), concursos (001) e identidade/gate (002); um contexto novo (`conferencia`), uma tabela de auditoria, ~2 endpoints. ✅ |
| VII. Identidade Gerenciada Externamente | Dono derivado do ID token verificado (002); nenhuma credencial nova. ✅ |
| VIII. Design Cloud-Native | Stateless; conferência sem estado global; config herdada do ambiente. ✅ |
| IX. Acesso Somente Após Aprovação | Rotas de conferência exigem `ROLE_USUARIO` (somente conta ATIVA, gate da 002); PENDENTE/REPROVADO → 403. ✅ |

**Resultado**: PASS — nenhuma violação. Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/005-game-checking/
├── plan.md              # Este arquivo (/speckit-plan)
├── research.md          # Fase 0 — decisões (cálculo, gatilho, idempotência da auditoria)
├── data-model.md        # Fase 1 — resultado derivado + tabela de auditoria
├── quickstart.md        # Fase 1 — como configurar/rodar/verificar
├── contracts/           # Fase 1 — contrato de interface
│   └── conferencia-api.yaml  # API REST de conferência
├── checklists/
│   └── requirements.md       # Checklist de qualidade da spec (já existente)
└── tasks.md             # Fase 2 — gerado por /speckit-tasks (NÃO criado aqui)
```

### Source Code (repository root)

```text
backend/
├── pom.xml                         # sem novas dependências
└── src/
    ├── main/
    │   ├── java/com/megasena/sync/
    │   │   ├── concurso/                   # (da 001) leitura: existência + dezenas sorteadas por número
    │   │   ├── identidade/                 # (da 002) UsuarioAutenticado, gate
    │   │   ├── jogo/                        # (da 004) Jogo, JogoRepository (isolado por usuario_id)
    │   │   ├── conferencia/                 # NÚCLEO desta feature
    │   │   │   ├── ConferenciaService.java        # confere sob demanda + audita (idempotente)
    │   │   │   ├── ConferenciaController.java      # GET /api/jogos/{id}/conferencia, GET /api/conferencias
    │   │   │   ├── Conferencia.java                # resultado derivado (não-persistido): status, acertos, faixa, premiado
    │   │   │   ├── Faixa.java                      # enum SENA/QUINA/QUADRA/NENHUMA
    │   │   │   ├── StatusConferencia.java          # enum CONFERIDO/AGUARDANDO_SORTEIO
    │   │   │   ├── ConferenciaResponse.java        # DTO de saída
    │   │   │   ├── EventoConferencia.java          # auditoria (1 por jogo×concurso)
    │   │   │   └── EventoConferenciaRepository.java
    │   │   └── config/
    │   │       └── SecurityConfig.java            # ATUALIZADO: /api/conferencias/** exige ROLE_USUARIO
    │   └── resources/
    │       └── db/migration/
    │           └── V8__create_evento_conferencia.sql
    └── test/
        └── java/com/megasena/sync/
            ├── conferencia/           # cálculo acertos/faixa/premiado, idempotência, aguardando-sorteio, isolamento
            └── config/                # gate de conta ATIVA nas rotas de conferência

frontend/                              # (adiado) Angular LTS — exibição da conferência
```

**Structure Decision**: Mantém o monorepo e o pacote raiz `com.megasena.sync`.
Introduz o contexto **`conferencia`**, que **lê** `jogo` (004) e `concurso` (001) e
não os modifica. A única mudança em código existente é o `SecurityConfig` (mapear
`/api/conferencias/**` a `ROLE_USUARIO`; `/api/jogos/**` já exige `ROLE_USUARIO`
desde a 004). O resultado da conferência é **derivado em memória** (`Conferencia`),
não persistido; apenas a **auditoria** (`evento_conferencia`) é gravada.

## Complexity Tracking

> Sem violações da Constitution Check — nenhuma justificativa de complexidade necessária.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
