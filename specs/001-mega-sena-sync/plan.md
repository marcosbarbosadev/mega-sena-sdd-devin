# Implementation Plan: Sincronização com a API da Mega Sena

**Branch**: `001-mega-sena-sync` | **Date**: 2026-05-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-mega-sena-sync/spec.md`

## Summary

Ingerir e manter atualizados os dados de concursos da Mega Sena a partir da fonte
oficial da Caixa, armazenando localmente número, data, dezenas sorteadas e valor do
prêmio. A sincronização roda via cron uma vez ao dia, com retry e degradação graciosa
(serve o último dado válido em cache quando a fonte cai). Suporta carga histórica
retomável e expõe ao perfil admin a consulta de status e o disparo manual de
recuperação. Os dados de concurso são referência global e somente leitura, servindo de
base para as futuras features de cadastro e conferência de jogos.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.x (Web, Data JPA, Validation, Security,
Scheduling), Flyway, MySQL Connector/J, Resilience4j (retry/timeout), cliente HTTP
`RestClient` (Spring). Testes: JUnit 5, Mockito, Testcontainers (MySQL), WireMock
(dublê da fonte externa). Build: Maven.

**Storage**: MySQL (schema versionado com Flyway)

**Testing**: JUnit 5 + Mockito (unidade); WireMock (integração com a fonte externa);
Testcontainers MySQL (integração de persistência)

**Target Platform**: Servidor Linux (containerizado)

**Project Type**: Web application (monorepo). Esta feature implementa **apenas o
backend** (`backend/`); o frontend Angular (`frontend/`) é introduzido por features
posteriores com UI voltada ao usuário.

**Performance Goals**: Carga histórica completa (~2.900 concursos) concluída em minutos;
sincronização incremental diária processa o(s) concurso(s) novo(s) em segundos; leitura
de um concurso é uma consulta indexada por chave primária.

**Constraints**: Sincronização idempotente; concurso confirmado é imutável; deve servir
o último dado válido quando a fonte estiver indisponível; retry com backoff e timeout
explícito. A fonte oficial pode ser lenta/instável e exige cadeia TLS completa e header
`User-Agent` de navegador.

**Scale/Scope**: ~2.900 concursos históricos (Mega Sena desde 1996, ~3 sorteios/semana);
1 concurso novo por sorteio. Dataset pequeno, baixo volume de escrita.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Aderência neste plano |
|-----------|----------------------|
| I. Isolamento Multiusuário (NÃO-NEGOCIÁVEL) | Dados de concurso são **referência global e somente leitura** (FR-013) — não são por-usuário, então não há superfície de vazamento entre usuários. As operações administrativas (status, disparo manual) ficam restritas ao perfil **admin** via Spring Security. ✅ |
| II. Integridade dos Dados de Sorteio | Validação obrigatória (6 dezenas distintas 1–60, valor ≥ 0) antes de persistir; upsert idempotente por número de concurso; concurso confirmado imutável. Testes obrigatórios para validação e idempotência. ✅ |
| III. Qualidade via Testes Pragmáticos | Cobertura obrigatória nos pontos críticos: mapeamento/validação da fonte, idempotência, e integração externa (sucesso/timeout/resposta malformada via WireMock). ✅ |
| IV. Resiliência da Integração | Sincronização agendada (cron diário) e assíncrona; Resilience4j com retry/backoff e timeout; persistência local serve como cache durante indisponibilidade; falhas registradas. ✅ |
| V. Observabilidade & Auditoria | Logging estruturado por execução; entidade `SyncRun` é a trilha de auditoria (origem, status, concursos afetados, erro, duração). ✅ |
| VI. Simplicidade & YAGNI | Um único módulo backend; sem camadas extras; frontend adiado até existir UI de usuário. Sem premiação detalhada/acúmulo (fora de escopo). ✅ |

**Resultado**: PASS — nenhuma violação. Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/001-mega-sena-sync/
├── plan.md              # Este arquivo (/speckit-plan)
├── research.md          # Fase 0 — decisões de pesquisa
├── data-model.md        # Fase 1 — entidades e regras
├── quickstart.md        # Fase 1 — como rodar/verificar
├── contracts/           # Fase 1 — contratos de interface
│   ├── admin-api.yaml        # API REST (status, disparo manual, leitura de concursos)
│   └── caixa-source.md       # Contrato da fonte externa (Caixa) + mapeamento
└── tasks.md             # Fase 2 — gerado por /speckit-tasks (NÃO criado aqui)
```

### Source Code (repository root)

```text
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/megasena/sync/
    │   │   ├── concurso/        # entidade Concurso, repositório, serviço de leitura
    │   │   ├── sincronizacao/   # job agendado, serviço de sync, SyncRun, lock
    │   │   ├── fonte/           # cliente da fonte oficial (RestClient + Resilience4j) + mapeamento
    │   │   ├── admin/           # controllers REST admin (status, disparo manual)
    │   │   └── config/          # Security (perfil admin), scheduling, HTTP client, Resilience4j
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/    # migrações Flyway (V1__*.sql, ...)
    └── test/
        └── java/com/megasena/sync/
            ├── sincronizacao/   # testes de idempotência, lock, retomada
            ├── fonte/           # testes WireMock (sucesso/timeout/malformado)
            └── concurso/        # testes de validação e persistência (Testcontainers)

frontend/                        # (adiado) Angular LTS — introduzido por features com UI
```

**Structure Decision**: Monorepo web com `backend/` (Spring Boot) e `frontend/`
(Angular) na raiz. Esta feature cria e implementa somente `backend/`; `frontend/` é um
placeholder que features posteriores (cadastro/conferência) irão preencher. A
organização por pacotes segue o domínio (concurso, sincronizacao, fonte, admin),
favorecendo coesão e testes por contexto.

## Complexity Tracking

> Sem violações da Constitution Check — nenhuma justificativa de complexidade necessária.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
