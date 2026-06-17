# Phase 1 — Data Model: Aprovação de Contas (Admin)

Deriva a entidade da spec (Key Entities + FR). A feature **lê e transiciona** a
`Usuario` da feature 002 (sem alterar seu schema) e **adiciona** uma tabela de
auditoria de decisões. Schema aplicado por migração Flyway `V5__`, continuando
`V1`/`V2` (001) e `V3`/`V4` (002).

---

## Entidade reaproveitada: `Usuario` (tabela `usuario` — definida na 002)

Esta feature **não altera** o schema de `usuario`. Ela atua sobre dois campos:

- `estado` (`ENUM('PENDENTE','ATIVO','REPROVADO')`) — alvo da transição.
- `atualizado_em` (`DATETIME(6)`) — atualizado na transição.

E **lê** para a fila de pendentes: `id`, `email`, `metodo_login`, `criado_em`.

### Transições de estado executadas por esta feature

A 002 cria a conta em **PENDENTE** e aplica o gate de acesso; a **003 executa as
transições** sob decisão do administrador:

```
                 aprovar (admin)
   PENDENTE ───────────────────────▶ ATIVO        (terminal nesta feature)
      │
      │ reprovar (admin, motivo obrigatório)
      ▼
   REPROVADO                                       (terminal nesta feature)
```

**Regras**:
- Transição permitida **apenas a partir de `PENDENTE`** (FR-005). `ATIVO` e
  `REPROVADO` são **terminais** nesta feature (clarificação 2026-06-11: sem
  reabertura).
- A transição é um **UPDATE condicionado**: `... WHERE id=? AND estado='PENDENTE'`.
  **Linhas afetadas = 0** ⇒ `TransicaoInvalidaException` (409): a conta não está pendente
  (já decidida ou inexistente) — cobre concorrência (FR-008) e estado inválido
  (FR-005) com a mesma cláusula atômica.
- O UPDATE e a gravação da `DecisaoModeracao` ocorrem na **mesma transação**.
- Após `ATIVO`, o gate de acesso da 002 passa a liberar o usuário (FR-006); após
  `REPROVADO`, permanece bloqueado.

---

## Entidade nova: `DecisaoModeracao` (tabela `decisao_moderacao`)

Trilha de auditoria **dedicada** das decisões de moderação (FR-007, Princípio V).
Distinta de `evento_identidade` por ter dois atores e um motivo de texto (ver
research.md R2).

| Campo | Tipo (MySQL) | Regras |
|-------|--------------|--------|
| `id` | `BIGINT AUTO_INCREMENT` | PK. |
| `usuario_id` | `BINARY(16)` | NOT NULL. FK → `usuario(id)`. Conta **alvo** da decisão. |
| `admin_id` | `BINARY(16)` | NOT NULL. FK → `usuario(id)`. Administrador **responsável** (derivado do token verificado, nunca do cliente). |
| `decisao` | `ENUM('APROVADO','REPROVADO')` | NOT NULL. |
| `motivo` | `VARCHAR(500)` | **NOT NULL quando `decisao='REPROVADO'`**; NULL quando `APROVADO` (FR-004a). Texto fornecido pelo admin. |
| `criado_em` | `DATETIME(6)` | NOT NULL. Momento da decisão. |

**Índices/constraints**:
- `PRIMARY KEY (id)`
- `KEY idx_decisao_usuario (usuario_id)` — auditoria por conta alvo.
- `KEY idx_decisao_admin_data (admin_id, criado_em)` — auditoria por administrador.
- `CONSTRAINT fk_decisao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)`
- `CONSTRAINT fk_decisao_admin FOREIGN KEY (admin_id) REFERENCES usuario(id)`
- `CONSTRAINT ck_decisao_motivo CHECK ( (decisao='REPROVADO' AND motivo IS NOT NULL) OR (decisao='APROVADO') )`
  — reforça FR-004a no banco (defesa em profundidade, além da validação na aplicação).

**Observação**: o motivo é validado **na aplicação** (Bean Validation: obrigatório e
não-vazio na reprovação) e **no banco** (CHECK), honrando a disciplina de
integridade. Não contém credenciais nem PII sensível além do que o admin escrever.

---

## Objetos de transporte (não persistidos)

- **`ContaPendenteResponse`** (projeção de leitura da fila): `id` (uuid), `email`,
  `metodoLogin` (`SENHA`/`GOOGLE`), `criadoEm`. Mínimo necessário para decidir
  (FR-001) — sem expor dados de jogos ou de outras áreas.
- **`ReprovacaoRequest`** (corpo de `POST .../reprovar`): `motivo` (String,
  `@NotBlank`). A aprovação não tem corpo (não exige motivo — FR-004a).
- **`Decisao`** (enum de domínio): `APROVADO` / `REPROVADO`.

---

## Migração Flyway

- **`V5__create_decisao_moderacao.sql`** — cria `decisao_moderacao` com as duas FKs
  para `usuario`, o `CHECK` do motivo e os índices de auditoria. Idempotente na
  aplicação em base nova (portão de merge nº 3 da constituição). **Não** altera
  `usuario`.
