# Implementation Plan: Aprovação de Contas (Admin)

**Branch**: `003-account-approval` | **Date**: 2026-06-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-account-approval/spec.md`

## Summary

Dar ao **administrador** os meios de fechar a porta de aprovação que a feature 002
abriu: listar as contas em estado **PENDENTE**, **aprovar** (PENDENTE → ATIVO) ou
**reprovar** (PENDENTE → REPROVADO, com **motivo obrigatório**), registrando cada
decisão numa trilha de auditoria dedicada (quem decidiu, sobre quem, qual decisão,
motivo, quando). A feature reaproveita integralmente a fundação da 002 — entidade
`Usuario` com `EstadoConta`/`Papel`, o filtro de identidade verificada e a
autoridade `ROLE_ADMINISTRADOR` — e **não** introduz novas integrações externas.
As transições ocorrem **somente a partir de PENDENTE** (ativo e reprovado são
terminais nesta feature) e a concorrência é resolvida por **UPDATE condicionado ao
estado** (a primeira decisão vence; a segunda é recusada). Entrega o **backend**
(API REST de moderação); a tela administrativa Angular é um incremento posterior,
seguindo o precedente das features 001 e 002.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.3.x (Web, Data JPA, Validation, Security),
Flyway (+ flyway-mysql), MySQL Connector/J. **Sem novas dependências externas**: a
identidade do administrador é resolvida pela cadeia de segurança já entregue pela
002 (porta `VerificadorDeIdentidade` + `IdentidadeTokenFilter`). Build: Maven.
Testes: JUnit 5, Mockito, Testcontainers (MySQL), `spring-security-test`, com o
**verificador de identidade dublê** da 002 para autenticar admin/usuário sem rede.

**Storage**: MySQL (nova migração Flyway `V5__create_decisao_moderacao.sql`,
continuando `V1`/`V2` da 001 e `V3`/`V4` da 002). Sem alteração de schema em
`usuario` (a transição usa UPDATE condicionado, não exige coluna de versão).

**Testing**: JUnit 5 + Mockito (unidade: guarda de transição, exigência de motivo
na reprovação, autorização admin-only); Testcontainers MySQL (integração de
persistência, concorrência via UPDATE condicionado, gravação da decisão);
`spring-security-test` + verificador dublê (admin autenticado vs. usuário comum
negado).

**Target Platform**: Servidor Linux (containerizado)

**Project Type**: Web application (monorepo). Esta feature implementa o **backend**
(`backend/`). A UI administrativa (fila de pendentes + ações) é introduzida como
incremento Angular posterior, alinhada ao crescimento incremental do Princípio VI
e ao precedente das features 001/002.

**Performance Goals**: Volume baixo (fila de pendentes pequena, uso restrito).
Listagem de pendentes é consulta indexada por `estado`; aprovação/reprovação é um
UPDATE condicionado de uma linha + um INSERT de auditoria. Decisão concluída em
poucos milissegundos; admin decide uma conta em < 1 min (SC-001).

**Constraints**: Stateless no processo (identidade do admin derivada do token a
cada requisição — Princípios VII/VIII); todas as ações restritas a
`ROLE_ADMINISTRADOR`, reforçado no servidor (Princípio I); transição apenas a
partir de PENDENTE com resolução de concorrência sem estado inconsistente
(FR-008); motivo obrigatório na reprovação (FR-004a); toda decisão auditada
(Princípio V).

**Scale/Scope**: Uma tabela nova (`decisao_moderacao`) + 3 endpoints REST de
moderação. Sem cache distribuído, sem paginação especial (volume baixo assumido).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Aderência neste plano |
|-----------|----------------------|
| I. Isolamento Multiusuário (NÃO-NEGOCIÁVEL) | A moderação é a **exceção legítima e privilegiada** ao filtro por-usuário: um ADMINISTRADOR atua sobre contas de terceiros. Mitigação: todos os endpoints exigem `ROLE_ADMINISTRADOR` (reforçado no servidor); a identidade do admin vem do token verificado (`UsuarioAutenticado`), nunca de ID do cliente; usuário comum recebe 403. Nenhum dado de jogos/usuário é exposto — apenas o mínimo da conta para decidir. ✅ |
| II. Integridade dos Dados de Sorteio | Não aplicável (não toca concursos/conferência). — |
| III. Qualidade via Testes Pragmáticos | Cobertura obrigatória: guarda de transição (só de PENDENTE), exigência de motivo na reprovação (FR-004a), autorização admin-only (FR-002), concorrência (UPDATE condicionado — 1ª decisão vence, FR-008) e gravação da decisão na auditoria. ✅ |
| IV. Resiliência da Integração | Sem nova integração externa; operação puramente local (DB). Reaproveita a cadeia de identidade da 002. — (sem nova superfície de falha externa) |
| V. Observabilidade & Auditoria | Tabela `decisao_moderacao` é a trilha dedicada (ator admin, conta alvo, decisão, motivo, quando — FR-007); logging estruturado correlacionado por requisição. ✅ |
| VI. Simplicidade & YAGNI | Reaproveita `Usuario`/estados/segurança da 002; **UPDATE condicionado** em vez de coluna de versão (sem alterar `usuario`); um contexto novo (`moderacao`), uma tabela, três endpoints; histórico e UI adiados (Out of Scope). ✅ |
| VII. Identidade Gerenciada Externamente | Identidade do admin derivada do ID token verificado (reusa porta + filtro da 002); nenhuma credencial nova manipulada. ✅ |
| VIII. Design Cloud-Native | Processo stateless; configuração herdada do ambiente; operação idempotente sob concorrência. ✅ |
| IX. Acesso Somente Após Aprovação | **Esta feature É o mecanismo de aprovação** — converte PENDENTE em ATIVO/REPROVADO sob decisão de admin, com auditoria; mantém ativo/reprovado terminais. Fecha o gate aberto pela 002. ✅ |

**Resultado**: PASS — nenhuma violação. Complexity Tracking vazio. A exceção do
Princípio I (admin agindo sobre contas de terceiros) é inerente à feature de
moderação e mitigada por autorização `ROLE_ADMINISTRADOR` no servidor; não é uma
violação, e sim o desenho previsto pelo Princípio IX.

## Project Structure

### Documentation (this feature)

```text
specs/003-account-approval/
├── plan.md              # Este arquivo (/speckit-plan)
├── research.md          # Fase 0 — decisões (concorrência, modelagem da auditoria, API)
├── data-model.md        # Fase 1 — entidade decisao_moderacao + transições executadas aqui
├── quickstart.md        # Fase 1 — como configurar/rodar/verificar
├── contracts/           # Fase 1 — contrato de interface
│   └── admin-moderation-api.yaml   # API REST de moderação (listar/aprovar/reprovar)
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
    │   │   ├── identidade/                 # (da 002) Usuario, EstadoConta, Papel, UsuarioRepository...
    │   │   ├── moderacao/                  # NÚCLEO desta feature
    │   │   │   ├── ModeracaoController.java       # GET pendentes, POST aprovar, POST reprovar
    │   │   │   ├── ModeracaoService.java          # transições guardadas + auditoria (transacional)
    │   │   │   ├── ContaPendenteResponse.java     # projeção: id, email, metodoLogin, criadoEm
    │   │   │   ├── ReprovacaoRequest.java          # corpo com motivo (obrigatório)
    │   │   │   ├── Decisao.java                    # enum APROVADO/REPROVADO
    │   │   │   ├── DecisaoModeracao.java           # entidade de auditoria da decisão
    │   │   │   ├── DecisaoModeracaoRepository.java
    │   │   │   ├── TransicaoInvalidaException.java # estado não permite a transição (409)
    │   │   │   └── ContaNaoEncontradaException.java # conta alvo inexistente (404)
    │   │   └── config/
    │   │       └── SecurityConfig.java            # ATUALIZADO: /api/admin/** exige ROLE_ADMINISTRADOR
    │   └── resources/
    │       └── db/migration/
    │           └── V5__create_decisao_moderacao.sql
    └── test/
        └── java/com/megasena/sync/
            ├── moderacao/             # guarda de transição, motivo obrigatório, concorrência, auditoria
            └── config/                # autorização admin-only (admin vs usuário comum) com verificador dublê

frontend/                              # (adiado) Angular LTS — tela de fila de pendentes + ações (incremento posterior)
```

**Structure Decision**: Mantém o monorepo e o pacote raiz `com.megasena.sync`.
Introduz o contexto de domínio **`moderacao`**, que depende do contexto
**`identidade`** da 002 (lê `Usuario`/estado, altera o estado sob guarda) mas não o
modifica estruturalmente. A única mudança em código existente é o `SecurityConfig`,
para mapear `/api/admin/**` à autoridade `ROLE_ADMINISTRADOR`. A entidade de
auditoria da decisão é **separada** de `EventoIdentidade` (ver research.md): a
decisão tem forma própria (ator admin + alvo + motivo livre) que não cabe na
semântica de eventos de identidade.

## Complexity Tracking

> Sem violações da Constitution Check — nenhuma justificativa de complexidade necessária.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
