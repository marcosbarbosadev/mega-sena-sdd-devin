# Feature Specification: Cadastro de Jogos

**Feature Branch**: `004-game-registration`

**Created**: 2026-06-11

**Status**: Draft

**Input**: User description: "004 Cadastro de jogos"

> Contexto: feature **004** do roadmap (`@draft/roadmap.md`). Permite ao usuário
> registrar suas apostas da Mega Sena. Depende da **002** (identidade que sustenta
> o isolamento por usuário e o gate de conta ATIVA) e da **001** (concursos
> sincronizados, para determinar o próximo concurso em aberto). É pré-requisito da
> **005** (conferência). Ancorada nos princípios **I (Isolamento Multiusuário)**,
> **II (Integridade dos Dados de Sorteio — validação de apostas)** e **V
> (Observabilidade & Auditoria)**.

## Clarifications

### Session 2026-06-11

- Q: O que a edição de um jogo (antes do sorteio) pode alterar? → A: As **dezenas e a quantidade** (6–9), mantendo o **mesmo concurso vinculado** (sem re-vincular).
- Q: Como o concurso vinculado é armazenado? → A: **Número do concurso fixado no cadastro** (snapshot imutável); a conferência usa esse número.
- Q: A "origem" (informado/gerado) precisa ser persistida? → A: **Não** — a geração automática é apenas conveniência de entrada; o jogo guarda só as dezenas, sem distinção de origem.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar um jogo informando as dezenas (Priority: P1)

Um usuário com conta ativa registra uma aposta escolhendo **quantas** dezenas quer
jogar (6, 7, 8 ou 9) e informando **exatamente** essa quantidade de dezenas
distintas entre 1 e 60. O jogo é salvo, vinculado ao próximo concurso em aberto, e
passa a pertencer somente a ele.

**Why this priority**: É o caminho central do produto — sem cadastrar jogos não há
o que conferir depois. Entrega valor imediato e isolado.

**Independent Test**: Autenticar como conta ATIVA, cadastrar um jogo com 6 dezenas
válidas e verificar que ele é salvo, vinculado ao próximo concurso em aberto e
visível apenas para o dono.

**Acceptance Scenarios**:

1. **Given** um usuário com conta **ativa**, **When** ele cadastra um jogo
   informando 6 (ou 7, 8, 9) dezenas distintas entre 1 e 60, **Then** o jogo é
   salvo, vinculado ao próximo concurso em aberto e atribuído ao usuário.
2. **Given** um cadastro com a quantidade escolhida (ex.: 7), **When** o número de
   dezenas informadas **não** corresponde à quantidade (ex.: 6 ou 8), **Then** o
   cadastro é recusado informando a divergência.
3. **Given** dezenas com repetição ou fora do intervalo 1–60, **When** o usuário
   tenta cadastrar, **Then** o cadastro é recusado com a regra violada.
4. **Given** uma quantidade fora de 6–9 (ex.: 5 ou 10), **When** o usuário tenta
   cadastrar, **Then** o cadastro é recusado.

---

### User Story 2 - Cadastrar um jogo com geração automática (Priority: P1)

Um usuário informa **apenas a quantidade** de dezenas (6, 7, 8 ou 9) e pede que o
sistema **gere** as dezenas. O sistema sorteia essa quantidade de dezenas distintas
entre 1 e 60 e salva o jogo como qualquer outro.

**Why this priority**: É o segundo modo de cadastro exigido e tão central quanto o
manual; muitos usuários preferem a geração automática.

**Independent Test**: Autenticar como conta ATIVA, pedir um jogo gerado de 8
dezenas e verificar que o jogo salvo tem exatamente 8 dezenas distintas válidas e
fica vinculado ao próximo concurso em aberto.

**Acceptance Scenarios**:

1. **Given** um usuário com conta ativa, **When** ele pede um jogo **gerado** de N
   dezenas (N ∈ {6,7,8,9}) sem informar dezenas, **Then** o sistema gera N dezenas
   distintas entre 1 e 60 e salva o jogo vinculado ao próximo concurso em aberto.
2. **Given** uma quantidade pedida fora de 6–9, **When** o usuário solicita a
   geração, **Then** a operação é recusada.
3. **Given** um pedido de geração, **When** o usuário **também** informa dezenas
   parciais, **Then** a operação é recusada (não há cadastro parcial — ver FR-003).

---

### User Story 3 - Consultar meus jogos (Priority: P2)

Um usuário lista os jogos que cadastrou, vendo as dezenas e o concurso vinculado.
Ele vê **apenas** os próprios jogos.

**Why this priority**: Necessária para o usuário acompanhar o que registrou e base
para a conferência (005), mas o valor de cadastrar já existe sem ela.

**Independent Test**: Com jogos de dois usuários distintos no sistema, listar como
um deles e confirmar que só aparecem os jogos do próprio usuário.

**Acceptance Scenarios**:

1. **Given** um usuário com jogos cadastrados, **When** ele lista seus jogos,
   **Then** vê todos os seus jogos com dezenas e concurso vinculado.
2. **Given** jogos de outro usuário no sistema, **When** ele lista, **Then** os
   jogos do outro usuário **não** aparecem.

---

### User Story 4 - Editar ou excluir um jogo antes do sorteio (Priority: P3)

Enquanto o concurso vinculado ainda **não** foi sorteado, o usuário pode editar as
dezenas de um jogo (respeitando as mesmas regras) ou excluí-lo. Após o sorteio do
concurso vinculado, o jogo torna-se somente leitura.

**Why this priority**: Conveniência de correção antes do sorteio; o cadastro tem
valor mesmo sem edição/exclusão.

**Independent Test**: Cadastrar um jogo vinculado a um concurso ainda não sorteado,
editá-lo e excluí-lo com sucesso; depois, sobre um jogo cujo concurso já foi
sorteado, confirmar que editar/excluir é bloqueado.

**Acceptance Scenarios**:

1. **Given** um jogo cujo concurso vinculado **não** foi sorteado, **When** o dono
   edita as dezenas — podendo também **alterar a quantidade** para outro valor de
   6–9 — com um conjunto válido, **Then** o jogo é atualizado mantendo o **mesmo
   concurso vinculado**.
2. **Given** o mesmo jogo, **When** o dono o exclui, **Then** o jogo deixa de
   existir para ele.
3. **Given** um jogo cujo concurso vinculado **já foi sorteado**, **When** o dono
   tenta editar ou excluir, **Then** a operação é bloqueada (somente leitura).
4. **Given** um jogo de outro usuário, **When** alguém que não é o dono tenta
   editar/excluir, **Then** a operação é negada.

---

### Edge Cases

- **Sem concurso em aberto determinável**: se ainda não há concursos sincronizados
  (001) para inferir o próximo em aberto, o cadastro é recusado com mensagem clara.
- **Sorteio entre cadastro e edição**: o concurso vinculado é sorteado depois do
  cadastro; uma tentativa posterior de editar/excluir é bloqueada (vira somente
  leitura).
- **Conta não-ativa**: usuário PENDENTE/REPROVADO tenta cadastrar/listar jogos → o
  gate da 002 bloqueia (403).
- **Acesso cruzado**: usuário tenta ler/editar/excluir um jogo informando o id de
  outro usuário → negado (isolamento, identidade derivada do servidor).
- **Geração impossível**: pedido de geração com quantidade fora de 6–9 → recusado.
- **Cadastro parcial**: usuário informa algumas (não todas) dezenas → recusado
  (FR-003).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir que um usuário com conta **ativa** cadastre
  um jogo escolhendo a quantidade de dezenas (**6, 7, 8 ou 9**) e informando
  exatamente essa quantidade de dezenas **distintas** entre **1 e 60**.
- **FR-002**: O sistema MUST permitir, alternativamente, que o usuário informe
  **apenas a quantidade** (6–9) e peça a **geração automática**; o sistema gera
  essa quantidade de dezenas distintas entre 1 e 60.
- **FR-003**: O sistema MUST aceitar **ou** todas as dezenas informadas **ou**
  nenhuma (geração) — **nunca um subconjunto parcial**. Pedido com dezenas parciais
  é recusado.
- **FR-004**: O sistema MUST validar toda aposta antes de salvar: quantidade ∈
  {6,7,8,9}, dezenas no intervalo 1–60, sem repetição (Princípio II).
- **FR-005**: No momento do cadastro, o sistema MUST vincular o jogo ao **próximo
  concurso em aberto** (o próximo concurso ainda não sorteado), gravando o **número
  do concurso de forma fixa (snapshot imutável)**; esse número não muda depois e é
  o usado pela conferência (005).
- **FR-006**: Todo jogo MUST pertencer ao usuário autenticado; leitura, edição e
  exclusão MUST ser restritas ao dono, com a identidade derivada da sessão
  verificada no servidor, nunca de um id enviado pelo cliente (Princípio I).
- **FR-007**: O sistema MUST permitir que o usuário **liste os próprios jogos**,
  com dezenas e concurso vinculado.
- **FR-008**: O sistema MUST permitir **editar** e **excluir** um jogo **enquanto o
  concurso vinculado não foi sorteado**; após o sorteio, o jogo é **somente
  leitura** (edição/exclusão bloqueadas). A edição pode alterar as **dezenas e a
  quantidade** (mantendo 6–9 e revalidando), porém **mantém o mesmo concurso
  vinculado** (não re-vincula).
- **FR-009**: O acesso a qualquer operação de jogos MUST exigir conta **ativa**
  (gate herdado da 002); contas PENDENTE/REPROVADO são bloqueadas.
- **FR-010**: O sistema MUST registrar trilha de auditoria das operações de jogo
  (cadastro, edição, exclusão) com quem, o quê e quando (Princípio V).

### Key Entities *(include if feature involves data)*

- **Jogo (Aposta)**: representa uma aposta do usuário. Atributos essenciais:
  identificador; **dono** (referência ao usuário — chave de isolamento, Princípio
  I); **concurso vinculado** (número do concurso ao qual a aposta concorre,
  **fixado no cadastro** e imutável); **dezenas** (conjunto de 6 a 9 números
  distintos de 1 a 60); marcos temporais (criação, atualização).
- **Concurso** (da feature 001, referência somente leitura): usado para determinar
  o próximo concurso em aberto no cadastro e para saber se o concurso vinculado já
  foi sorteado (governa a janela de edição/exclusão).
- **Usuário/Conta** (da feature 002, referência): dono do jogo e sujeito do gate de
  conta ativa.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um usuário consegue cadastrar um jogo (informado ou gerado) em menos
  de 1 minuto.
- **SC-002**: 100% dos jogos criados pertencem e são visíveis **apenas** ao dono;
  em nenhum cenário um usuário acessa o jogo de outro.
- **SC-003**: 100% das tentativas de cadastro **parcial** (subconjunto de dezenas)
  são recusadas.
- **SC-004**: 100% dos jogos gerados têm exatamente a quantidade pedida de dezenas
  distintas, todas entre 1 e 60.
- **SC-005**: 100% das tentativas de editar/excluir um jogo cujo concurso vinculado
  já foi sorteado são bloqueadas.
- **SC-006**: 100% das apostas com quantidade fora de 6–9, dezenas fora de 1–60 ou
  com repetição são recusadas na validação.

## Assumptions

- **Próximo concurso em aberto**: definido como o próximo concurso ainda não
  sorteado, inferido a partir dos concursos sincronizados pela 001 (na prática, o
  número imediatamente após o último concurso sorteado conhecido). Depende de a 001
  ter dados; sem dados, o cadastro é recusado.
- **Janela de edição/exclusão**: um jogo só pode ser editado/excluído enquanto o
  concurso vinculado não foi sorteado; após o sorteio torna-se somente leitura
  (preserva integridade para a conferência da 005).
- **Sem limite de jogos por usuário**: volume baixo (uso pessoal); não há teto de
  quantidade de apostas por usuário nesta versão.
- **Duplicatas permitidas**: o usuário pode cadastrar mais de um jogo com o mesmo
  conjunto de dezenas (aposta repetida é uma escolha legítima).
- **Geração uniforme**: a geração automática sorteia dezenas distintas de forma
  aleatória uniforme entre 1 e 60.
- **Escopo 6–9 dezenas**: embora a regra oficial permita apostas de 6 a 15 dezenas,
  este produto restringe deliberadamente a 6, 7, 8 ou 9 (decisão de produto da
  descrição original). É uma validação **mais restritiva**, ainda dentro das regras
  oficiais.

## Out of Scope

- **Conferência** dos jogos contra o resultado do concurso — feature **005**.
- Cálculo de prêmios, valores e acúmulo.
- Apostas com **seleção parcial** de dezenas (sempre todas ou geração).
- Apostas com **mais de 9** dezenas (10–15), bolão/cotas e apostas compartilhadas.
- Escolha manual de **qual** concurso apostar (o vínculo é sempre ao próximo em
  aberto).
- Pagamento/registro oficial da aposta na Caixa (o sistema apenas gerencia os jogos
  do usuário).
