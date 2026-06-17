# Phase 0 — Research: Cadastro de Jogos

Resolve as decisões de desenho da feature 004. Não há `NEEDS CLARIFICATION` no
Technical Context; os pontos de maior incerteza da spec foram fechados na sessão de
clarificações (edição 6–9 mantendo o concurso, vínculo de concurso fixo, origem não
persistida). Esta pesquisa registra as decisões técnicas e os dois itens *Deferred*
da clarificação (representação do número e geração testável).

---

## R1 — Determinação do "próximo concurso em aberto"

**Decisão**: O próximo concurso em aberto é **`MAX(concurso.numero) + 1`**, lido da
tabela `concurso` da 001 no momento do cadastro. Se não houver nenhum concurso
sincronizado, o cadastro é recusado com `SemConcursoAbertoException` (FR-005 / edge
case).

**Rationale**: A 001 mantém os concursos **sorteados** (referência global somente
leitura). O próximo a sortear é, por definição, o número seguinte ao último
conhecido. É uma consulta O(log n) por PK, sem novo estado. Atende à decisão de
snapshot (R2): o número é capturado e gravado no cadastro.

**Alternativas consideradas**:
- **Manter uma entidade "concurso aberto" própria**: introduziria estado a
  sincronizar com a 001; YAGNI. Rejeitada.
- **Vínculo dinâmico ("sempre o próximo em aberto" em tempo de leitura)**:
  rejeitado na clarificação (o número é fixo no cadastro — R2).

---

## R2 — Representação do concurso vinculado (sem FK)

**Decisão**: `jogo.concurso_numero` é um **INT simples (NOT NULL)**, **não** uma
foreign key para `concurso`. O número é o snapshot imutável capturado no cadastro.

**Rationale**: No momento da aposta, o concurso-alvo (`max+1`) ainda **não foi
sorteado** e portanto **não existe** como linha em `concurso` (a 001 só grava
concursos confirmados). Uma FK falharia na inserção. Guardar o número solto é
correto e suficiente; a existência da linha `concurso` com aquele número é
justamente o sinal de que o concurso **já foi sorteado** (ver R3).

**Alternativas consideradas**:
- **FK para `concurso(numero)`**: impossível no cadastro (linha inexistente).
  Rejeitada.
- **Pré-criar a linha do concurso aberto**: violaria a imutabilidade/"somente
  confirmados" da 001 e acoplaria as features. Rejeitada.

---

## R3 — Janela de edição/exclusão (jogo "já sorteado")

**Decisão**: Um jogo é **editável/excluível** enquanto **não existir** um
`concurso` com `numero = jogo.concurso_numero`. Assim que a 001 ingere o resultado
daquele concurso (a linha passa a existir), o jogo torna-se **somente leitura**
(`JogoBloqueadoException` → 409 em editar/excluir). O campo `editavel` exposto na
leitura deriva dessa checagem.

**Rationale**: Reutiliza o estado real da 001 como fonte da verdade do "sorteado",
sem novo flag a manter sincronizado (Princípio VI). Coerente com R2 e com a
conferência (005), que opera sobre concursos já existentes.

**Alternativas consideradas**:
- **Flag `sorteado` no próprio jogo**: exigiria atualizar todos os jogos quando um
  concurso é sorteado; redundante com a existência da linha em `concurso`.
  Rejeitada.

---

## R4 — Geração automática de dezenas (testável)

**Decisão**: A geração é uma **porta** `GeradorDeDezenas.gerar(int quantidade)` que
devolve N dezenas distintas entre 1 e 60. O adaptador padrão usa uma **fonte de
aleatoriedade injetável** (`RandomGenerator`/`Random` recebido por construtor); nos
testes injeta-se uma fonte determinística (semente fixa ou dublê) para asserções
estáveis.

**Rationale**: Mantém a aleatoriedade fora do código de regra e torna o resultado
**determinístico nos testes** (Princípio III), sem depender de `Math.random()`
global (também alinhado ao Princípio VIII — sem estado global). A geração reusa a
mesma validação do cadastro manual (6–9, 1–60, sem repetição) por construção.

**Alternativas consideradas**:
- **`Math.random()`/`ThreadLocalRandom` direto no serviço**: não testável de forma
  determinística; estado global. Rejeitada.

---

## R5 — Modelagem da auditoria de operações de jogo

**Decisão**: Tabela dedicada **`evento_jogo`** (entidade `EventoJogo`) com:
`usuario_id` (ator/dono), `jogo_id` (alvo; pode referenciar um jogo já excluído via
valor histórico), `tipo` (`CADASTRO`/`EDICAO`/`EXCLUSAO`), `criado_em`. Gravada na
mesma transação da operação.

**Rationale**: O Princípio V exige trilha de auditoria das operações sobre apostas
(criação, edição, exclusão). Uma tabela própria mantém a auditoria consultável e
coesa, no mesmo padrão de `evento_identidade` (002) e `decisao_moderacao` (003), sem
sobrecarregar aquelas com semântica de jogos.

**Alternativas consideradas**:
- **Só logging estruturado**: não fica consultável/rastreável como exige o
  Princípio V (as demais features usaram tabela). Rejeitada.
- **Reusar `evento_identidade`**: semântica diferente (eventos de identidade do
  sujeito, não operações de aposta). Rejeitada.

---

## R6 — Forma da API e regra "tudo ou geração"

**Decisão**: Endpoints REST sob `/api/jogos`:
- `POST /api/jogos` — corpo com **`dezenas[]`** (cadastro informado) **ou**
  **`quantidade`** (geração). Informar os dois, ou nenhum, ou dezenas parciais →
  **400 `CADASTRO_PARCIAL`/`VALIDACAO`** (FR-003).
- `GET /api/jogos` — lista os jogos do próprio usuário.
- `GET /api/jogos/{id}` — obtém um jogo do próprio usuário (404 se não for dono).
- `PUT /api/jogos/{id}` — edita dezenas/quantidade (6–9), mantendo o concurso;
  bloqueado após o sorteio (409 `JOGO_BLOQUEADO`).
- `DELETE /api/jogos/{id}` — exclui; bloqueado após o sorteio (409).

Modelo de erro reusa o schema `Erro` da 002 (status, codigo, mensagem). Validação de
aposta → 400 `VALIDACAO`; sem concurso aberto → 409 `SEM_CONCURSO_ABERTO`.

**Rationale**: Um único `POST` com dois modos exclusivos reflete fielmente a regra
"todas as dezenas **ou** geração, nunca parcial". O isolamento por dono é resolvido
no servidor (id do path validado contra `usuarioId` do token). Mantém o estilo/erro
das features anteriores.

**Alternativas consideradas**:
- **Dois endpoints separados (`/jogos` e `/jogos/gerar`)**: duplicaria validação e
  contrato; o modo é apenas a presença de `dezenas[]` vs `quantidade`. Rejeitada.
- **`PATCH` para edição**: a edição substitui o conjunto de dezenas (e quantidade)
  inteiro; `PUT` do recurso é mais fiel que um patch parcial. Rejeitada.
