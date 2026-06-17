# Implementation Plan: Cadastro de Jogos

**Branch**: `004-game-registration` | **Date**: 2026-06-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-game-registration/spec.md`

## Summary

Permitir que um usuário com conta **ativa** registre suas apostas da Mega Sena —
informando 6 a 9 dezenas (1–60) **ou** pedindo a geração automática — e gerencie
esses jogos (listar, editar e excluir antes do sorteio). Cada jogo é **isolado por
usuário** (FK `usuario_id`, derivado do token verificado da 002) e vinculado ao
**próximo concurso em aberto**, gravado como **número fixo (snapshot imutável)** no
cadastro. O "próximo em aberto" é `max(concurso.numero)+1` lido dos concursos
sincronizados pela 001; um jogo é editável/excluível enquanto **não existir** um
`concurso` com aquele número (ainda não sorteado) e torna-se somente leitura após o
sorteio. Não se persiste origem (geração é só conveniência de entrada). Entrega o
**backend** (API REST); a UI Angular é incremento posterior, seguindo o precedente
das features 001/002/003.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.3.x (Web, Data JPA, Validation, Security),
Flyway (+ flyway-mysql), MySQL Connector/J. **Sem novas dependências externas**: a
identidade/gate de conta ativa vêm da 002; os concursos vêm da 001 (leitura). A
geração de dezenas usa uma fonte de aleatoriedade **injetável** (porta interna)
para ser determinística nos testes. Build: Maven. Testes: JUnit 5, Mockito,
Testcontainers (MySQL), `spring-security-test` + verificador de identidade dublê da
002.

**Storage**: MySQL. Novas migrações Flyway `V6__create_jogo.sql` (tabelas `jogo` +
`jogo_dezena`) e `V7__create_evento_jogo.sql`, continuando `V1`/`V2` (001),
`V3`/`V4` (002) e `V5` (003).

> **Ordem de migração (dependência de release)**: `V6`/`V7` pressupõem que a `V5`
> da feature 003 já foi aplicada. Esta branch saiu de `main` (sem a `V5`). Antes do
> merge, garantir a **ordem 003 → 004**; alternativamente, habilitar
> `spring.flyway.out-of-order=true` ou renumerar na integração, para não deixar
> lacuna de versão no histórico do Flyway.

**Testing**: JUnit 5 + Mockito (unidade: validação 6–9/1–60/sem repetição, regra
"tudo ou geração", geração com fonte determinística, gate de janela de edição);
Testcontainers MySQL (integração de persistência, isolamento por `usuario_id`,
vínculo ao próximo concurso); `spring-security-test` + dublê (conta ATIVA acessa,
PENDENTE/REPROVADO bloqueadas).

**Target Platform**: Servidor Linux (containerizado)

**Project Type**: Web application (monorepo). Esta feature implementa o **backend**
(`backend/`). A UI de seleção de dezenas (lista 1–60) é introduzida como incremento
Angular posterior (Princípio VI; precedente 001/002/003).

**Performance Goals**: Volume baixo (apostas pessoais esporádicas). Cadastro e
edição são escritas de 1 jogo + 6–9 dezenas; listagem é consulta indexada por
`usuario_id`; "próximo em aberto" é `MAX(numero)` por PK. Cadastro completável em
< 1 min (SC-001).

**Constraints**: Isolamento total por `usuario_id` derivado do token, nunca do
cliente (Princípio I); acesso exige conta **ATIVA** (gate `ROLE_USUARIO` herdado da
002 — Princípio IX); validação de aposta antes de persistir (Princípio II);
edição/exclusão apenas antes do sorteio do concurso vinculado; toda operação de
jogo auditada (Princípio V); stateless (Princípio VIII).

**Scale/Scope**: Duas tabelas de domínio (`jogo`, `jogo_dezena`) + uma de auditoria
(`evento_jogo`); ~5 endpoints REST. Sem cache distribuído, sem paginação especial
(volume baixo).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Aderência neste plano |
|-----------|----------------------|
| I. Isolamento Multiusuário (NÃO-NEGOCIÁVEL) | `jogo.usuario_id` (FK → `usuario`) é o eixo de isolamento; **toda** consulta/escrita filtra pelo `usuarioId` do `UsuarioAutenticado` (derivado do token), nunca de id do cliente; leitura/edição/exclusão restritas ao dono (FR-006). Acesso cruzado → 404/403. ✅ |
| II. Integridade dos Dados de Sorteio | Validação obrigatória antes de persistir: quantidade ∈ {6,7,8,9}, dezenas 1–60, sem repetição (FR-004); a geração produz conjuntos válidos; reforço por constraints (`UNIQUE`+`CHECK`) em `jogo_dezena`. Produto restringe a 6–9 (subconjunto do oficial 6–15) — validação mais restritiva. ✅ |
| III. Qualidade via Testes Pragmáticos | Cobertura obrigatória: validação, isolamento, geração (fonte determinística), regra "tudo ou geração" (FR-003), gate de janela de edição (FR-008) e gate de conta ativa. ✅ |
| IV. Resiliência da Integração | Sem nova integração externa; lê a tabela `concurso` local da 001. Sem concurso sincronizado → recusa graciosa (edge case). — |
| V. Observabilidade & Auditoria | Tabela `evento_jogo` registra cadastro/edição/exclusão (quem, qual jogo, quando — FR-010); logging estruturado correlacionado. ✅ |
| VI. Simplicidade & YAGNI | Reaproveita identidade/segurança da 002 e concursos da 001; `concurso_numero` é **INT simples** (não FK — o concurso pode não existir ainda no cadastro); sem campo de origem; um contexto novo (`jogo`); UI adiada. ✅ |
| VII. Identidade Gerenciada Externamente | Dono do jogo derivado do ID token verificado (porta + filtro da 002); nenhuma credencial nova. ✅ |
| VIII. Design Cloud-Native | Stateless; config herdada do ambiente; geração sem estado global (fonte injetável). ✅ |
| IX. Acesso Somente Após Aprovação | Todas as rotas de jogos exigem `ROLE_USUARIO` (somente conta **ATIVA**, gate da 002); PENDENTE/REPROVADO recebem 403. ✅ |

**Resultado**: PASS — nenhuma violação. Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/004-game-registration/
├── plan.md              # Este arquivo (/speckit-plan)
├── research.md          # Fase 0 — decisões (próximo em aberto, vínculo sem FK, geração, auditoria)
├── data-model.md        # Fase 1 — entidades, regras e janela de edição
├── quickstart.md        # Fase 1 — como configurar/rodar/verificar
├── contracts/           # Fase 1 — contrato de interface
│   └── jogos-api.yaml        # API REST de jogos (criar/listar/obter/editar/excluir)
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
    │   │   ├── concurso/                   # (da 001) leitura: maior número + existência por número
    │   │   ├── identidade/                 # (da 002) Usuario, UsuarioAutenticado, gate
    │   │   ├── jogo/                        # NÚCLEO desta feature
    │   │   │   ├── Jogo.java                      # entidade (id, usuario_id, concurso_numero, dezenas, timestamps)
    │   │   │   ├── JogoRepository.java            # consultas SEMPRE filtradas por usuario_id
    │   │   │   ├── JogoService.java               # criar/editar/excluir + regras (transacional)
    │   │   │   ├── GeradorDeDezenas.java          # porta: gera N dezenas distintas 1–60
    │   │   │   ├── GeradorDeDezenasAleatorio.java # adaptador (fonte de aleatoriedade injetável)
    │   │   │   ├── CadastroJogoRequest.java       # corpo: ou dezenas[] OU quantidade (geração)
    │   │   │   ├── JogoResponse.java              # id, concursoNumero, dezenas, editavel
    │   │   │   ├── ConcursoAbertoService.java     # próximo em aberto + se o vinculado foi sorteado
    │   │   │   ├── EventoJogo.java                # auditoria (CADASTRO/EDICAO/EXCLUSAO)
    │   │   │   ├── EventoJogoRepository.java
    │   │   │   ├── ApostaInvalidaException.java   # validação 6–9/1–60/repetição/parcial → 400
    │   │   │   ├── SemConcursoAbertoException.java# nenhum concurso sincronizado → 409
    │   │   │   └── JogoBloqueadoException.java    # edição/exclusão após sorteio → 409
    │   │   └── config/
    │   │       └── SecurityConfig.java            # ATUALIZADO: /api/jogos/** exige ROLE_USUARIO
    │   └── resources/
    │       └── db/migration/
    │           ├── V6__create_jogo.sql            # tabelas jogo + jogo_dezena
    │           └── V7__create_evento_jogo.sql
    └── test/
        └── java/com/megasena/sync/
            ├── jogo/                  # validação, tudo-ou-geração, geração determinística, janela de edição, isolamento
            └── config/                # gate de conta ATIVA (ATIVO acessa; PENDENTE/REPROVADO 403)

frontend/                              # (adiado) Angular LTS — seleção de dezenas (1–60) + lista de jogos
```

**Structure Decision**: Mantém o monorepo e o pacote raiz `com.megasena.sync`.
Introduz o contexto **`jogo`**, que depende de **`identidade`** (002, dono + gate) e
**`concurso`** (001, leitura para o próximo em aberto e a janela de edição). A única
mudança em código existente é o `SecurityConfig` (mapear `/api/jogos/**` a
`ROLE_USUARIO`). O `concurso_numero` é um **INT simples** (não FK), pois no momento
do cadastro o concurso-alvo ainda não foi sorteado e pode não existir como linha em
`concurso` (ver research.md R2).

## Complexity Tracking

> Sem violações da Constitution Check — nenhuma justificativa de complexidade necessária.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
