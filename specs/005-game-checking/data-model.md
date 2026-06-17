# Phase 1 — Data Model: Conferência Automática de Jogos

A feature **lê** `jogo`/`jogo_dezena` (004) e `concurso`/`concurso_dezena` (001) e
**não os modifica**. O resultado da conferência é **derivado em memória** (não
persistido como dado de domínio — FR-011); apenas a **auditoria** é gravada. Schema
por migração `V8__`, continuando `V1`/`V2` (001), `V3`/`V4` (002), `V5` (003) e
`V6`/`V7` (004).

---

## Objeto derivado (não persistido): `Conferencia`

Resultado computado sob demanda a partir do jogo e do concurso vigente.

| Campo | Tipo | Regras |
|-------|------|--------|
| `jogoId` | UUID | Jogo conferido (do dono autenticado). |
| `concursoNumero` | int | Concurso vinculado do jogo. |
| `status` | `StatusConferencia` | `CONFERIDO` (concurso sorteado) ou `AGUARDANDO_SORTEIO`. |
| `acertos` | int (0–6) | Presente apenas quando `CONFERIDO`. `|dezenas(jogo) ∩ dezenas(concurso)|`. |
| `faixa` | `Faixa` | `SENA`(6)/`QUINA`(5)/`QUADRA`(4)/`NENHUMA`(≤3). Apenas quando `CONFERIDO`. |
| `premiado` | boolean | `acertos ≥ 4`. Apenas quando `CONFERIDO`. |

**Regras de cálculo (FR-002/003/004, R1)**:
- `acertos = |dezenas(jogo) ∩ dezenas(concurso)|`, onde `dezenas(concurso)` são as 6
  sorteadas (máx. 6 acertos, inclusive para jogos de 7–9 dezenas).
- `faixa`: 6→SENA, 5→QUINA, 4→QUADRA, ≤3→NENHUMA.
- `premiado = acertos ≥ 4`.
- `status = AGUARDANDO_SORTEIO` quando **não** existe `concurso` com
  `numero = jogo.concurso_numero` (R2); nesse caso `acertos/faixa/premiado` ausentes.

**Enums**:
- `Faixa`: `SENA`, `QUINA`, `QUADRA`, `NENHUMA`.
- `StatusConferencia`: `CONFERIDO`, `AGUARDANDO_SORTEIO`.

---

## Entidade nova: `EventoConferencia` (tabela `evento_conferencia`)

Trilha de auditoria da conferência (FR-011, Princípio V). **Uma por jogo×concurso.**

| Campo | Tipo (MySQL) | Regras |
|-------|--------------|--------|
| `id` | `BIGINT AUTO_INCREMENT` | PK. |
| `usuario_id` | `BINARY(16)` | NOT NULL. FK → `usuario(id)`. Quem conferiu (do token). |
| `jogo_id` | `BINARY(16)` | NOT NULL. Jogo conferido (**sem FK** — robustez histórica, espelha `evento_jogo`). |
| `concurso_numero` | `INT` | NOT NULL. Concurso vinculado conferido. |
| `acertos` | `TINYINT` | NOT NULL. 0–6 (snapshot do resultado na 1ª conferência). |
| `faixa` | `ENUM('SENA','QUINA','QUADRA','NENHUMA')` | NOT NULL. |
| `premiado` | `BOOLEAN` | NOT NULL. |
| `criado_em` | `DATETIME(6)` | NOT NULL. Momento da 1ª conferência. |

**Índices/constraints**:
- `PRIMARY KEY (id)`
- `UNIQUE KEY uk_conf_jogo_concurso (jogo_id, concurso_numero)` — garante **1 registro
  por jogo×concurso** (idempotência da auditoria — R4); a 2ª conferência não insere.
- `KEY idx_conf_usuario (usuario_id)` — auditoria por usuário.
- `CONSTRAINT fk_conf_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)`
- `CONSTRAINT ck_conf_acertos CHECK (acertos BETWEEN 0 AND 6)`

> O snapshot (`acertos/faixa/premiado`) registra o que foi conferido na 1ª vez. Uma
> correção posterior do concurso (001) afeta a **computação ao vivo** (`Conferencia`),
> **não** este registro histórico, que permanece único por jogo×concurso.

---

## Regras derivadas dos requisitos

- **Isolamento (FR-007, Princípio I)**: a conferência só resolve jogos via
  `JogoRepository.findByIdAndUsuarioId`/`findByUsuarioId` (004); não-dono → 404.
- **Somente leitura (FR-010)**: nenhuma escrita em `jogo`/`jogo_dezena`/`concurso`;
  a única escrita é o `INSERT` idempotente em `evento_conferencia`.
- **Idempotência (FR-006)**: o cálculo é puro; o registro de auditoria é único por
  jogo×concurso (UNIQUE).
- **Gate de conta ativa (FR-008, Princípio IX)**: `/api/jogos/**` e `/api/conferencias/**`
  exigem `ROLE_USUARIO`.

---

## Migração Flyway

- **`V8__create_evento_conferencia.sql`** — cria `evento_conferencia` com a UNIQUE
  `(jogo_id, concurso_numero)`, a FK para `usuario`, o `CHECK` de acertos e o índice
  por usuário. Não altera tabelas das features 001/002/003/004.

> **Ordem de migração (dependência de release)**: `V8` pressupõe `V5` (003) e
> `V6`/`V7` (004) aplicadas. Garantir a ordem de merge **003 → 004 → 005** (ou
> `spring.flyway.out-of-order=true`) para não deixar lacuna no histórico do Flyway.
