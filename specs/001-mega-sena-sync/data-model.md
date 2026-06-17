# Data Model: Sincronização com a API da Mega Sena

**Feature**: 001-mega-sena-sync | **Date**: 2026-05-31

Modelo mínimo alinhado ao escopo (apenas número, data, dezenas e valor do prêmio).
Premiação detalhada e acúmulo estão fora de escopo. Esquema versionado via Flyway em
MySQL.

## Entidade: Concurso

Representa um sorteio oficial confirmado. Dado de referência global, somente leitura
para o restante do produto, imutável após confirmado.

| Campo | Tipo | Regras |
|-------|------|--------|
| `numero` | INT (PK) | Número do concurso; positivo; único (chave natural). |
| `data_sorteio` | DATE | Data oficial do sorteio; obrigatória. |
| `valor_premio` | DECIMAL(15,2) | Valor do prêmio (faixa Sena); ≥ 0. |
| `criado_em` | TIMESTAMP | Auditoria — quando o registro foi inserido. |
| `atualizado_em` | TIMESTAMP | Auditoria — última escrita (correção rastreável). |

### Dezenas (tabela filha `concurso_dezena`)

As 6 dezenas sorteadas, normalizadas para garantir integridade e habilitar a futura
conferência via SQL.

| Campo | Tipo | Regras |
|-------|------|--------|
| `concurso_numero` | INT (FK → `concurso.numero`) | Obrigatório; ON DELETE CASCADE. |
| `dezena` | TINYINT | Entre 1 e 60 (CHECK). |
| | | UNIQUE (`concurso_numero`, `dezena`) — sem repetição. |

**Invariante**: cada `concurso` tem **exatamente 6** dezenas distintas entre 1 e 60
(FR-010). Validada na camada de aplicação antes de persistir e reforçada por
constraints (UNIQUE + CHECK).

## Entidade: SyncRun (Execução de Sincronização)

Trilha de auditoria e estado operacional de cada rodada de sincronização (Princípio V).

| Campo | Tipo | Regras |
|-------|------|--------|
| `id` | BIGINT (PK, auto) | Identificador da execução. |
| `origem` | ENUM(`AGENDADA`,`MANUAL`) | Como a sync foi disparada. |
| `status` | ENUM(`EM_EXECUCAO`,`SUCESSO`,`FALHA`,`PARCIAL`) | Estado atual. |
| `iniciado_em` | TIMESTAMP | Início da execução; obrigatório. |
| `finalizado_em` | TIMESTAMP | Fim; nulo enquanto `EM_EXECUCAO`. |
| `concurso_inicial` | INT | Menor concurso afetado nesta rodada; nulo se nada importado. |
| `concurso_final` | INT | Maior concurso afetado; nulo se nada importado. |
| `concursos_importados` | INT | Quantidade importada (default 0). |
| `tentativas` | INT | Número de tentativas até concluir/falhar. |
| `mensagem_erro` | TEXT | Detalhe do erro quando `FALHA`/`PARCIAL`; nulo caso contrário. |

### Transições de estado (SyncRun)

```text
EM_EXECUCAO ──► SUCESSO   (todos os concursos pendentes importados)
EM_EXECUCAO ──► PARCIAL   (alguns importados, depois falha/timeout)
EM_EXECUCAO ──► FALHA     (nenhum importado por indisponibilidade da fonte)
```

**Lock de execução única**: no máximo uma `SyncRun` em `EM_EXECUCAO` por vez. Uma nova
sync (agendada ou manual) que encontre uma execução em andamento é recusada, evitando
condição de corrida (edge case de concorrência).

## Regras derivadas dos requisitos

- **Idempotência (FR-006)**: importar um concurso usa upsert por `numero`; um concurso já
  confirmado não é recriado nem tem dezenas alteradas. Reprocessar produz 0 duplicatas
  (SC-004).
- **Sequência contínua (FR-014, SC-002)**: a sync importa todos os `numero` entre
  `max(numero armazenado)+1` e o número do último concurso da fonte, sem deixar lacunas.
- **Imutabilidade (FR-012)**: dezenas e número são imutáveis após confirmados;
  eventual correção da fonte é uma reingestão rastreável (registra `atualizado_em` e uma
  `SyncRun`).
- **Referência global (FR-013)**: nenhuma coluna `user_id` — concursos são compartilhados
  e somente leitura para usuários finais.

## Índices

- `concurso`: PK em `numero` (busca por concurso e cálculo do maior número são O(log n)).
- `concurso_dezena`: PK/UNIQUE (`concurso_numero`, `dezena`); índice por `dezena` para a
  futura conferência.
- `sync_run`: índice por `iniciado_em DESC` para recuperar rapidamente a última execução
  (SC-005); índice parcial/consulta por `status = 'EM_EXECUCAO'` para o lock.
