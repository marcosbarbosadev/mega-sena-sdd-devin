# Phase 0 — Research: Conferência Automática de Jogos

Resolve as decisões de desenho da feature 005. Não há `NEEDS CLARIFICATION` no
Technical Context; os pontos de maior impacto foram fechados na sessão de
clarificações (conferência computada sob demanda + auditoria única por jogo×concurso).

---

## R1 — Cálculo da conferência (acertos, faixa, premiado)

**Decisão**: `acertos = |dezenas(jogo) ∩ dezenas(concurso)|`, onde `dezenas(concurso)`
são as **6 dezenas sorteadas** (001). Faixa: **SENA** (6), **QUINA** (5), **QUADRA**
(4), **NENHUMA** (≤ 3). Premiado = `acertos ≥ 4`. Para jogos de 7–9 dezenas, a
interseção é naturalmente limitada a 6 (só há 6 sorteadas).

**Rationale**: Interseção de conjuntos é determinística e idempotente (Princípio II),
correta para qualquer quantidade de dezenas do jogo (6–9) e independe de valores de
prêmio (fora de escopo). O cálculo de acertos/faixa tem **cobertura de testes
obrigatória** pela constituição.

**Alternativas consideradas**:
- **Tabela de faixas configurável**: desnecessária para 3 faixas fixas; YAGNI.
  Rejeitada.

---

## R2 — Gatilho "concurso sorteado" e estado aguardando

**Decisão**: Um jogo é **conferível** quando existe o `concurso` com
`numero = jogo.concurso_numero` na base da 001 (mesmo critério da janela de edição da
004 — R3 daquela feature). Se o concurso ainda não existe, a conferência retorna
`status = AGUARDANDO_SORTEIO` (sem acertos/faixa/premiado).

**Rationale**: Reutiliza o estado real da 001 como verdade do "sorteado", sem flag
novo. Coerente com a 004 e com o Princípio VI.

**Alternativas consideradas**:
- **Comparar datas (data do sorteio vs hoje)**: a fonte da verdade é a existência do
  resultado, não o calendário; um sorteio pode atrasar a ingestão. Rejeitada.

---

## R3 — Determinismo sobre o dado vigente e correções

**Decisão**: A conferência é **sempre computada sobre o resultado atual** do concurso
(001). Se a 001 corrigir/reingerir um concurso, a próxima conferência reflete o novo
dado. **Não** há materialização do resultado de domínio (FR-011).

**Rationale**: Garante que o usuário veja o resultado correto vigente sem job de
recomputação. Idempotência vale para um dado fixo (mesmo concurso, mesmas dezenas →
mesmo resultado).

**Alternativas consideradas**:
- **Materializar o resultado (cache em tabela)**: exigiria invalidação/recomputação
  quando o concurso é corrigido; mais complexidade e risco de obsolescência.
  Rejeitada (clarificação 2026-06-12).

---

## R4 — Auditoria idempotente (uma por jogo×concurso)

**Decisão**: Tabela **`evento_conferencia`** com **`UNIQUE (jogo_id, concurso_numero)`**.
Na primeira conferência de um jogo (quando o concurso já foi sorteado), grava-se uma
linha com o resultado-snapshot (acertos/faixa/premiado); conferências repetidas
**não** criam novas linhas (insert idempotente / `INSERT ... ON DUPLICATE KEY` ou
check-then-insert tolerante à corrida).

**Rationale**: Satisfaz o Princípio V (trilha consultável) sem inflar a auditoria a
cada visualização (clarificação 2026-06-12); a unicidade torna o registro idempotente
e seguro sob concorrência. O snapshot guarda o que foi conferido na 1ª vez; correções
posteriores do concurso afetam a **computação ao vivo**, não o registro histórico.

**Alternativas consideradas**:
- **Registrar a cada visualização**: volumoso e ruidoso. Rejeitada.
- **Não persistir (só log)**: violaria o Princípio V (auditoria consultável).
  Rejeitada.

---

## R5 — Leitura das dezenas do concurso (dependência da 001)

**Decisão**: Adicionar uma leitura na 001 para obter as dezenas sorteadas por número
(ex.: `ConcursoDezenaRepository.findDezenasByConcursoNumero(int)` ou método
equivalente no repositório/serviço de concurso), além de `existsByNumero` (já
previsto na 004). O contexto `conferencia` consome essa leitura.

**Rationale**: A 001 normaliza as dezenas em `concurso_dezena`; a conferência precisa
do conjunto sorteado. Mantém a 001 como dona do dado (somente leitura para a 005).

**Alternativas consideradas**:
- **Duplicar/copiar as dezenas do concurso na 005**: violaria a fonte única da 001 e
  o Princípio II. Rejeitada.

---

## R6 — Forma da API

**Decisão**: Dois endpoints REST:
- `GET /api/jogos/{id}/conferencia` — confere um jogo específico do usuário
  (`CONFERIDO` com acertos/faixa/premiado, ou `AGUARDANDO_SORTEIO`); 404 se o jogo
  não for do usuário.
- `GET /api/conferencias` — lista a conferência de **todos** os jogos do usuário (cada
  item conferido ou aguardando).

Modelo de erro reusa o schema `Erro` da 002. Gate: `/api/jogos/**` já exige
`ROLE_USUARIO` (004); adicionar `/api/conferencias/**` ao mesmo gate.

**Rationale**: `GET /api/jogos/{id}/conferencia` é coeso com o recurso jogo (US1);
`GET /api/conferencias` cobre a visão consolidada (US2). Ambas são leitura sobre o
**domínio** (não alteram jogo nem concurso — FR-010).

**Sobre o efeito colateral de auditoria no `GET`** (decisão deliberada): a gravação
em `evento_conferencia` é um efeito **idempotente** (UNIQUE `jogo_id, concurso_numero`
— R4), de modo que prefetch, retries ou conferências repetidas produzem **no máximo
uma** linha — exatamente o registro desejado da primeira conferência. Aceita-se
conscientemente o desvio da semântica "safe" do `GET` em troca de uma API natural
(conferir é conceitualmente ler) e consistente entre o item e a lista. Não há
mutação de domínio; o registro é a própria trilha exigida pelo Princípio V.

**Alternativas consideradas**:
- **`POST /api/jogos/{id}/conferencia`** (manter `GET` puro e registrar via `POST`):
  tornaria a **lista** (`GET /api/conferencias`, naturalmente um `GET`) inconsistente
  — ela também registra — e duplicaria endpoints. Rejeitada em favor do `GET`
  idempotente documentado.
- **Materializar o resultado**: rejeitada na clarificação (FR-011).
