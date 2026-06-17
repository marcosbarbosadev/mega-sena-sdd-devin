# Phase 1 — Data Model: Cadastro de Jogos

Deriva as entidades da spec (Key Entities + FR). A feature **lê** `concurso` (001) e
referencia `usuario` (002), e **adiciona** as tabelas de jogos e de auditoria.
Schema aplicado por migrações Flyway `V6__`/`V7__`, continuando `V1`/`V2` (001),
`V3`/`V4` (002) e `V5` (003).

---

## Entidade: `Jogo` (tabela `jogo`)

Uma aposta do usuário. **`usuario_id` é a chave de isolamento** (Princípio I); o
`concurso_numero` é o snapshot imutável do concurso vinculado (R2).

| Campo | Tipo (MySQL) | Regras |
|-------|--------------|--------|
| `id` | `BINARY(16)` (UUID) | PK. |
| `usuario_id` | `BINARY(16)` | NOT NULL. FK → `usuario(id)`. Dono; eixo de isolamento. |
| `concurso_numero` | `INT` | NOT NULL. Número do concurso vinculado, **fixado no cadastro** (não é FK — R2). Positivo. |
| `quantidade` | `TINYINT` | NOT NULL. Quantidade de dezenas; **∈ {6,7,8,9}** (FR-001/FR-004). Redundante com a contagem de `jogo_dezena` (conveniência/validação). |
| `criado_em` | `DATETIME(6)` | NOT NULL. |
| `atualizado_em` | `DATETIME(6)` | NOT NULL. Atualizado em cada edição. |

**Índices/constraints**:
- `PRIMARY KEY (id)`
- `KEY idx_jogo_usuario (usuario_id)` — listagem isolada por dono (FR-007).
- `KEY idx_jogo_usuario_concurso (usuario_id, concurso_numero)` — consultas do dono por concurso (base da 005).
- `CONSTRAINT fk_jogo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)`
- `CONSTRAINT ck_jogo_quantidade CHECK (quantidade BETWEEN 6 AND 9)`

> **Sem campo de origem** (clarificação 2026-06-11): a geração é só conveniência de
> entrada; o jogo guarda apenas as dezenas.

### Dezenas (tabela filha `jogo_dezena`)

As 6–9 dezenas do jogo, normalizadas para integridade (espelha o padrão de
`concurso_dezena` da 001).

| Campo | Tipo (MySQL) | Regras |
|-------|--------------|--------|
| `jogo_id` | `BINARY(16)` | NOT NULL. FK → `jogo(id)` `ON DELETE CASCADE`. |
| `dezena` | `TINYINT` | Entre 1 e 60 (`CHECK`). |
| | | `UNIQUE (jogo_id, dezena)` — sem repetição. |

**Invariante**: cada `jogo` tem **entre 6 e 9** dezenas distintas entre 1 e 60
(FR-004). A contagem (6–9) é validada na **aplicação** antes de persistir (o banco
garante 1–60 e a ausência de repetição; a faixa 6–9 e a coerência com `quantidade`
ficam na aplicação).

### Janela de edição/exclusão (estado derivado, não persistido)

Não há coluna de estado no `jogo`. A **editabilidade** é derivada em tempo de
leitura (R3):

```
editavel(jogo) = NÃO EXISTE concurso WHERE concurso.numero = jogo.concurso_numero
```

- Enquanto o concurso vinculado **não foi sorteado** (sem linha em `concurso`), o
  jogo é **editável/excluível** (FR-008).
- Após o sorteio (a 001 ingere a linha), o jogo é **somente leitura**; editar/
  excluir → `JogoBloqueadoException` (409).

---

## Entidade nova: `EventoJogo` (tabela `evento_jogo`)

Trilha de auditoria das operações de jogo (FR-010, Princípio V). Padrão das demais
features (`evento_identidade`, `decisao_moderacao`).

| Campo | Tipo (MySQL) | Regras |
|-------|--------------|--------|
| `id` | `BIGINT AUTO_INCREMENT` | PK. |
| `usuario_id` | `BINARY(16)` | NOT NULL. FK → `usuario(id)`. Dono que executou a operação (do token). |
| `jogo_id` | `BINARY(16)` | NOT NULL. Identificador do jogo afetado (valor histórico; pode apontar para jogo já excluído — **sem FK** para sobreviver ao `DELETE`). |
| `tipo` | `ENUM('CADASTRO','EDICAO','EXCLUSAO')` | NOT NULL. |
| `criado_em` | `DATETIME(6)` | NOT NULL. |

**Índices/constraints**:
- `PRIMARY KEY (id)`
- `KEY idx_evento_jogo_usuario (usuario_id)`
- `KEY idx_evento_jogo_data (tipo, criado_em)`
- `CONSTRAINT fk_evento_jogo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)`

> `jogo_id` **não** tem FK para `jogo`, propositalmente: a auditoria de `EXCLUSAO`
> deve persistir após o jogo deixar de existir.

---

## Objetos de transporte (não persistidos)

- **`CadastroJogoRequest`** (corpo de `POST /api/jogos`): ou **`dezenas`** (lista de
  6–9 inteiros 1–60) **ou** **`quantidade`** (6–9) para geração — exatamente um dos
  dois; ambos/nenhum/parcial → recusado (FR-003). O mesmo contrato serve à edição
  (`PUT`), permitindo trocar dezenas e quantidade.
- **`JogoResponse`**: `id`, `concursoNumero`, `dezenas` (ordenadas), `editavel`
  (derivado — R3), `criadoEm`/`atualizadoEm`.

---

## Regras derivadas dos requisitos

- **Isolamento (FR-006, Princípio I)**: toda consulta/escrita inclui o predicado
  `usuario_id = :usuarioAutenticado`; `GET/PUT/DELETE /{id}` resolvem o jogo
  **e** verificam a posse antes de qualquer ação (não-dono → 404).
- **Validação (FR-004, Princípio II)**: quantidade ∈ {6,7,8,9}, dezenas 1–60, sem
  repetição; coerência entre `quantidade` e o nº de dezenas (cadastro informado).
- **Tudo ou geração (FR-003)**: no cadastro/edição, ou `dezenas` completas ou
  `quantidade` para geração; parcial → 400.
- **Vínculo fixo (FR-005, R2)**: `concurso_numero = MAX(concurso.numero)+1` no
  cadastro; imutável depois (a edição não re-vincula).
- **Gate de conta ativa (FR-009, Princípio IX)**: `/api/jogos/**` exige
  `ROLE_USUARIO` (somente ATIVO).

---

## Migrações Flyway

- **`V6__create_jogo.sql`** — cria `jogo` (PK, FK `usuario`, `CHECK` quantidade,
  índices) e `jogo_dezena` (FK `ON DELETE CASCADE`, `CHECK` 1–60, `UNIQUE`).
- **`V7__create_evento_jogo.sql`** — cria `evento_jogo` (FK `usuario`, índices; sem
  FK para `jogo`).

Ambas idempotentes na aplicação em base nova (portão de merge nº 3 da constituição).
Não alteram tabelas das features 001/002.
