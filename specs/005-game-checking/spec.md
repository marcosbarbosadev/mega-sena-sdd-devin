# Feature Specification: Conferência Automática de Jogos

**Feature Branch**: `005-game-checking`

**Created**: 2026-06-12

**Status**: Draft

**Input**: User description: "005 Conferência automática de jogos"

> Contexto: feature **005** do roadmap (`@draft/roadmap.md`) — a última da versão
> atual. Confere os jogos cadastrados (004) contra o resultado oficial do concurso
> vinculado (001), mostrando acertos, faixa (sena/quina/quadra) e se o jogo foi
> premiado — **sem valores monetários**. Depende da **004** (jogos isolados por
> usuário) e da **001** (concursos sorteados); reutiliza a identidade/gate da
> **002**. Ancorada nos princípios **I (Isolamento)**, **II (Integridade da
> conferência — determinística e idempotente)** e **IX (gate de conta ativa)**.

## Clarifications

### Session 2026-06-12

- Q: Como tratar persistência e auditoria do resultado de conferência (Princípio V)? → A: **Computar sob demanda** (determinístico sobre o dado vigente) **e** registrar uma **trilha de auditoria** de cada conferência realizada (quem/jogo/concurso/resultado/quando); o resultado **não** é materializado como dado de domínio.
- Q: Com que granularidade registrar a auditoria da conferência? → A: **Uma vez por jogo×concurso** (na primeira conferência); repetir a consulta não gera novas linhas (idempotente).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Conferir um jogo cujo concurso foi sorteado (Priority: P1)

Um usuário com conta ativa vê a conferência de um jogo seu cujo concurso vinculado
**já foi sorteado**: quantas dezenas acertou, a faixa correspondente (sena, quina
ou quadra) e se o jogo foi premiado.

**Why this priority**: É o valor central da feature — saber o resultado da aposta
sem conferência manual. Sem isso, a feature não entrega nada.

**Independent Test**: Com um concurso sorteado e um jogo do usuário vinculado a
ele, solicitar a conferência e verificar que os acertos, a faixa e o indicador de
premiado correspondem exatamente à interseção entre as dezenas do jogo e as
sorteadas.

**Acceptance Scenarios**:

1. **Given** um jogo com 6 dezenas cujo concurso foi sorteado, **When** o usuário
   confere, **Then** o sistema mostra o número de acertos (dezenas sorteadas
   presentes no jogo), a faixa e se foi premiado.
2. **Given** um jogo que acertou **6** dezenas, **When** conferido, **Then** a
   faixa é **sena** e o jogo consta como **premiado**.
3. **Given** um jogo que acertou **5** (quina) ou **4** (quadra) dezenas, **When**
   conferido, **Then** a faixa correspondente é mostrada e o jogo consta como
   **premiado**.
4. **Given** um jogo que acertou **3 ou menos** dezenas, **When** conferido,
   **Then** mostra os acertos, **nenhuma faixa** e **não premiado**.

---

### User Story 2 - Consultar a conferência dos meus jogos (Priority: P2)

Um usuário lista seus jogos com a respectiva conferência: os já sorteados mostram o
resultado; os ainda não sorteados aparecem como **aguardando sorteio**. Ele vê
**apenas** os próprios jogos.

**Why this priority**: Dá visão consolidada do desempenho das apostas; útil, mas o
valor essencial (conferir um jogo) já existe na US1.

**Independent Test**: Com jogos do usuário em concursos sorteados e não sorteados,
listar e verificar que cada jogo mostra resultado ou "aguardando sorteio", e que
jogos de outro usuário não aparecem.

**Acceptance Scenarios**:

1. **Given** jogos do usuário em concursos sorteados e não sorteados, **When** ele
   consulta suas conferências, **Then** cada jogo mostra o resultado (acertos/
   faixa/premiado) ou o estado **aguardando sorteio**.
2. **Given** jogos de outro usuário no sistema, **When** ele consulta, **Then** os
   jogos do outro usuário **não** aparecem.

---

### Edge Cases

- **Jogo com 7–9 dezenas**: os acertos contam quantas das **6 dezenas sorteadas**
  estão presentes no jogo (máximo 6); a faixa segue o número de acertos (4/5/6).
- **Concurso ainda não sorteado**: a conferência do jogo fica como **aguardando
  sorteio**, nunca apresenta resultado.
- **Resultado corrigido na fonte**: se o concurso for reingerido/corrigido pela
  001, a conferência reflete **sempre o dado vigente** (determinística sobre o
  resultado atual).
- **Conta não-ativa**: usuário PENDENTE/REPROVADO que tenta conferir → bloqueado
  pelo gate (403).
- **Acesso cruzado**: tentativa de conferir/ver o jogo de outro usuário → negado
  (isolamento, identidade derivada do servidor).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Quando existir resultado do concurso vinculado a um jogo, o sistema
  MUST conferir o jogo comparando suas dezenas com as **dezenas sorteadas** do
  concurso.
- **FR-002**: A conferência MUST informar a **quantidade de acertos** — o número de
  dezenas sorteadas presentes no jogo (0 a 6).
- **FR-003**: A conferência MUST classificar a **faixa**: **sena** (6 acertos),
  **quina** (5), **quadra** (4); abaixo de 4 acertos, **nenhuma faixa**.
- **FR-004**: A conferência MUST indicar se o jogo foi **premiado** (≥ 4 acertos),
  **sem** qualquer valor monetário.
- **FR-005**: A conferência só MUST estar disponível quando o concurso vinculado
  **foi sorteado**; enquanto não houver resultado, o jogo MUST aparecer como
  **aguardando sorteio**.
- **FR-006**: A conferência MUST ser **determinística e idempotente**: conferir o
  mesmo jogo contra o mesmo concurso produz sempre o mesmo resultado, sem efeitos
  colaterais (Princípio II).
- **FR-007**: A conferência MUST ser **isolada por usuário**: o usuário só confere
  e vê os **próprios** jogos; a identidade do dono é derivada da sessão verificada
  no servidor, nunca de um id enviado pelo cliente (Princípio I).
- **FR-008**: O acesso à conferência MUST exigir conta **ativa** (gate herdado da
  002); contas PENDENTE/REPROVADO são bloqueadas.
- **FR-009**: O usuário MUST poder ver a conferência de um jogo específico **e**
  consultar a conferência do conjunto dos seus jogos.
- **FR-010**: A conferência MUST ser **somente leitura** sobre o jogo e o concurso —
  não altera dezenas, vínculo nem resultado.
- **FR-011**: O resultado de conferência MUST ser **computado sob demanda** (não
  materializado como dado de domínio) e a conferência de um jogo MUST gerar uma
  **trilha de auditoria** com quem conferiu, o jogo, o concurso, o resultado
  (acertos/faixa/premiado) e quando — **uma vez por jogo×concurso** (a primeira
  conferência); repetições não geram novos registros (Princípios V e II).

### Key Entities *(include if feature involves data)*

- **Resultado de Conferência**: para um jogo cujo concurso foi sorteado — referência
  ao **jogo** e ao **concurso**, **acertos** (0–6), **faixa** (sena/quina/quadra ou
  nenhuma) e **premiado** (sim/não). É **derivado** de forma determinística do jogo
  e do resultado do concurso, **computado sob demanda** (não introduz nem materializa
  dado novo de domínio).
- **Evento de Conferência** (auditoria): registro da conferência de um jogo — quem
  (usuário), jogo, concurso, resultado (acertos/faixa/premiado) e quando. **Único
  por jogo×concurso** (gravado na primeira conferência; repetições não criam novos
  registros). Compõe a trilha de auditoria exigida pelo Princípio V (FR-011).
- **Jogo** (da feature 004, referência): as dezenas apostadas e o `concurso_numero`
  vinculado; sujeito da conferência, isolado por usuário.
- **Concurso** (da feature 001, referência somente leitura): fornece as dezenas
  sorteadas; sua existência indica que o concurso foi sorteado (gatilho da
  conferência).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Para um jogo cujo concurso foi sorteado, os acertos exibidos
  correspondem **exatamente** à interseção entre as dezenas do jogo e as sorteadas
  (100% de correção).
- **SC-002**: A conferência é **idempotente**: repetir a conferência do mesmo jogo
  contra o mesmo concurso retorna sempre o mesmo resultado.
- **SC-003**: A faixa é **sena** para 6 acertos, **quina** para 5, **quadra** para
  4 e **nenhuma** para ≤ 3, e "premiado" é verdadeiro **se e somente se** acertos
  ≥ 4 — em 100% dos casos.
- **SC-004**: 100% dos jogos cujo concurso ainda **não** foi sorteado aparecem como
  **aguardando sorteio**, nunca com resultado.
- **SC-005**: 100% das conferências respeitam o isolamento — em nenhum cenário um
  usuário vê a conferência do jogo de outro.

## Assumptions

- **Conferência derivada, computada sob demanda**: o resultado é calculado de forma
  determinística a partir do jogo (004) e do concurso (001), **sob demanda**, sem
  ser materializado como dado de domínio; cada conferência realizada gera uma trilha
  de auditoria (FR-011, clarificação 2026-06-12).
- **Acertos = interseção com as 6 dezenas sorteadas**: para jogos de 6 a 9 dezenas,
  conta-se quantas das 6 sorteadas estão no jogo (máximo 6).
- **Premiado = quadra ou acima (≥ 4 acertos)**: limiar definido na visão; sem
  valores de prêmio (a fonte da 001 traz apenas número do concurso e dezenas).
- **Gatilho pela existência do concurso**: "foi sorteado" = existe o concurso
  vinculado na base da 001 (mesmo critério da janela de edição da 004).
- **Determinismo sobre o dado vigente**: se a 001 corrigir um resultado, a
  conferência passa a refletir o novo dado; não há histórico de conferências
  anteriores nesta versão.
- **Backend primeiro**: a apresentação (UI Angular) é incremento posterior
  (precedente 001/002/003/004); a spec descreve o comportamento, não a tela.

## Out of Scope

- **Valores de prêmio, rateio e acúmulo** (a 001 não fornece valores; fora do
  produto).
- **Notificação/aviso** ao usuário quando um concurso é sorteado (e-mail/push).
- **Conferência de jogos de terceiros** ou rankings entre usuários.
- **Histórico de conferências** anteriores a uma correção de resultado.
- **Reprocessamento/sincronização** dos concursos (responsabilidade da 001) e
  cadastro/edição de jogos (responsabilidade da 004).
- **Faixas/prêmios abaixo da quadra** além de informar a contagem de acertos.
