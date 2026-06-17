# Feature Specification: Aprovação de Contas (Admin)

**Feature Branch**: `003-account-approval`

**Created**: 2026-06-11

**Status**: Draft

**Input**: User description: "003 Aprovação de contas"

> Contexto: feature **003** do roadmap (`@draft/roadmap.md`). Materializa a ação
> administrativa que falta para fechar a **porta de aprovação** estabelecida pela
> feature **002** (Identidade & Autenticação). A 002 cria contas em estado
> **pendente** e bloqueia o acesso; a 003 dá ao administrador os meios de
> **aprovar** ou **reprovar** essas contas. Ancorada nos princípios **IX (Acesso
> Somente Após Aprovação)**, **I (Isolamento Multiusuário)** e **V (Observabilidade
> & Auditoria)** da constituição. Reaproveita os três estados definidos na 002:
> **pendente / ativo / reprovado**.

## Clarifications

### Session 2026-06-11

- Q: A decisão de moderação captura um motivo/justificativa? → A: Motivo **obrigatório apenas na reprovação**; aprovação não exige motivo.
- Q: A área de moderação inclui consulta ao histórico de contas já decididas? → A: Não — **apenas a fila de pendentes + ações**; histórico fica fora desta feature (preservado só na trilha de auditoria).
- Q: Uma conta reprovada pode ser reaberta/reavaliada? → A: Não — **reprovado é terminal** nesta feature; ativo e reprovado são estados finais, só a partir de **pendente** há transição.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Listar e revisar contas pendentes (Priority: P1)

Um administrador acessa a área administrativa e vê a fila de contas que aguardam
aprovação, com as informações necessárias para decidir (e-mail, método de
cadastro, data do pedido). A partir dessa lista ele consegue revisar quem está
solicitando acesso.

**Why this priority**: Sem visibilidade das contas pendentes o administrador não
tem como exercer a moderação; é o ponto de partida de toda a feature.

**Independent Test**: Pode ser testado criando algumas contas pendentes e
verificando que apenas um administrador consegue listá-las, com os dados
necessários para a decisão, e que contas já ativas ou reprovadas não poluem a
fila de pendentes.

**Acceptance Scenarios**:

1. **Given** existem contas em estado **pendente**, **When** um administrador abre
   a fila de aprovação, **Then** ele vê todas as contas pendentes com e-mail,
   método de cadastro (e-mail+senha ou Google) e data do pedido.
2. **Given** não há contas pendentes, **When** o administrador abre a fila,
   **Then** ele vê uma fila vazia com indicação clara de que não há pendências.
3. **Given** um usuário **comum** (não administrador), **When** ele tenta acessar
   a fila de aprovação, **Then** o acesso é negado.

---

### User Story 2 - Aprovar uma conta pendente (Priority: P1)

Um administrador aprova uma conta pendente. A conta passa a **ativa** e, a partir
daí, o usuário consegue autenticar-se e acessar as áreas de usuário. A decisão
fica registrada na trilha de auditoria.

**Why this priority**: É o coração da feature — converter um pedido em acesso
efetivo. Sem isso nenhum usuário criado pela 002 consegue usar o sistema.

**Independent Test**: Pode ser testado aprovando uma conta pendente e confirmando
que (a) o estado muda para ativo, (b) o usuário passa a ter acesso e (c) um
registro de auditoria com quem aprovou, quem foi aprovado e quando é gerado.

**Acceptance Scenarios**:

1. **Given** uma conta **pendente**, **When** um administrador a aprova, **Then**
   a conta passa a **ativa** e o usuário correspondente ganha acesso às áreas de
   usuário.
2. **Given** uma conta recém-aprovada, **When** a aprovação é concluída, **Then**
   um registro de auditoria é criado com o administrador responsável, a conta
   afetada e o momento da decisão.
3. **Given** uma conta que **não** está pendente (já ativa ou reprovada), **When**
   um administrador tenta aprová-la, **Then** a operação é recusada com indicação
   de que o estado não permite a transição.

---

### User Story 3 - Reprovar uma conta pendente (Priority: P1)

Um administrador reprova uma conta pendente que não deve ter acesso. A conta passa
a **reprovada** e permanece sem acesso. A decisão é auditada.

**Why this priority**: É a outra metade da porta de segurança (Princípio IX):
barrar quem não deve entrar. Tem a mesma criticidade que aprovar.

**Independent Test**: Pode ser testado reprovando uma conta pendente e confirmando
que o estado muda para reprovado, que o usuário continua sem acesso e que a
decisão é registrada na auditoria.

**Acceptance Scenarios**:

1. **Given** uma conta **pendente**, **When** um administrador a reprova
   informando um **motivo**, **Then** a conta passa a **reprovada**, continua sem
   acesso aos recursos de usuário e o motivo é guardado na trilha de auditoria.
2. **Given** uma reprovação **sem motivo informado**, **When** o administrador
   tenta concluí-la, **Then** a operação é recusada exigindo a justificativa.
3. **Given** uma conta reprovada, **When** a reprovação é concluída, **Then** um
   registro de auditoria é criado com o administrador responsável, a conta afetada
   e o momento da decisão.
4. **Given** uma conta que **não** está pendente, **When** um administrador tenta
   reprová-la, **Then** a operação é recusada com indicação de que o estado não
   permite a transição.

---

### Edge Cases

- **Decisão concorrente**: dois administradores tentam decidir sobre a mesma conta
  pendente ao mesmo tempo — apenas a primeira decisão vale; a segunda é recusada
  por estado já alterado.
- **Aprovação durante sessão pendente**: um usuário que está com a tela de "aguarde
  aprovação" aberta é aprovado — ao tentar novamente, passa a ter acesso sem
  recadastrar (comportamento herdado da 002).
- **Reprovar e recadastrar**: uma pessoa reprovada tenta criar nova conta com o
  mesmo e-mail — a unicidade de identidade da 002 impede burlar a reprovação.
- **Conta pendente excluída/ausente**: o administrador tenta decidir sobre uma
  conta que não existe mais — a operação é recusada com mensagem clara.
- **Nenhum administrador disponível**: se não houver administrador ativo, contas
  pendentes nunca são aprovadas (resolvido pelo admin inicial provisionado fora de
  banda, assumido pela 002).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir que um **administrador** liste as contas em
  estado **pendente**, exibindo ao menos e-mail, método de cadastro e data do
  pedido.
- **FR-002**: O sistema MUST restringir todas as ações de moderação (listar
  pendentes, aprovar, reprovar) ao papel **administrador**; usuários comuns MUST
  ser negados (Princípio I).
- **FR-003**: O sistema MUST permitir que um administrador **aprove** uma conta
  pendente, transicionando-a para **ativo**.
- **FR-004**: O sistema MUST permitir que um administrador **reprove** uma conta
  pendente, transicionando-a para **reprovado**.
- **FR-004a**: O sistema MUST exigir um **motivo** ao reprovar uma conta e recusar
  a reprovação sem justificativa; a **aprovação** NÃO exige motivo.
- **FR-005**: O sistema MUST permitir as transições apenas a partir do estado
  **pendente**; tentativas de aprovar/reprovar contas já ativas ou reprovadas MUST
  ser recusadas, informando que o estado atual não permite a transição. Os estados
  **ativo** e **reprovado** são **terminais** nesta feature (sem reabertura).
- **FR-006**: Após a aprovação, a conta MUST passar a ter acesso aos recursos de
  usuário; após a reprovação, MUST permanecer sem acesso (coerente com a 002 e o
  Princípio IX).
- **FR-007**: O sistema MUST registrar, para cada decisão de aprovação ou
  reprovação, uma trilha de auditoria contendo o administrador responsável, a
  conta afetada, a decisão e o momento (Princípio V).
- **FR-008**: O sistema MUST garantir que decisões concorrentes sobre a mesma conta
  não produzam estados inconsistentes: apenas a primeira decisão vale; a segunda é
  recusada por estado já alterado.
- **FR-009**: O sistema MUST derivar a identidade do administrador que decide a
  partir da sessão verificada no servidor, nunca de identificador enviado pelo
  cliente (Princípios I e VII).
- **FR-010**: O sistema NÃO PODE permitir que um administrador altere o estado de
  acesso por qualquer caminho que não seja aprovar/reprovar uma conta pendente
  (sem "ativação direta" fora do fluxo auditado).

### Key Entities *(include if feature involves data)*

- **Conta de usuário** (definida na 002): aqui é o objeto da moderação; os atributos
  relevantes são o **estado** (pendente/ativo/reprovado), o **e-mail**, o **método
  de cadastro** e a **data do pedido**. A 003 lê a fila de pendentes e altera o
  estado.
- **Decisão de moderação** (auditoria): registro de cada aprovação/reprovação, com
  administrador responsável, conta afetada, tipo de decisão, momento e **motivo**
  (obrigatório quando a decisão é reprovação; ausente/não exigido na aprovação).
  Compõe a trilha de auditoria exigida pelo Princípio V.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um administrador consegue revisar a fila de pendentes e decidir
  (aprovar ou reprovar) uma conta em menos de 1 minuto por conta.
- **SC-002**: 100% das ações de moderação executadas por não-administradores são
  bloqueadas.
- **SC-003**: 100% das aprovações e reprovações geram um registro de auditoria
  rastreável (quem, quem, o quê, quando).
- **SC-004**: 100% das tentativas de transição a partir de um estado que não seja
  **pendente** são recusadas, sem alterar o estado da conta.
- **SC-005**: Imediatamente após a aprovação, o usuário correspondente passa a
  conseguir acessar as áreas de usuário; imediatamente após a reprovação,
  permanece sem acesso.
- **SC-006**: Em decisões concorrentes sobre a mesma conta, o sistema nunca produz
  estado inconsistente — exatamente uma decisão prevalece.

## Assumptions

- **Estados herdados da 002**: a 003 usa os três estados já definidos
  (pendente/ativo/reprovado); não introduz novos estados (ex.: "bloqueado" é
  sinônimo de sem-acesso, não um estado distinto).
- **Admin inicial provisionado fora de banda**: existe ao menos um administrador
  ativo para operar a fila, conforme assumido pela 002.
- **Reprovação não é exclusão**: reprovar mantém a conta registrada (em estado
  reprovado) para impedir recadastro e preservar a trilha; a exclusão de dados
  (LGPD) é tratada à parte, fora desta feature.
- **Sem notificação ao usuário**: avisar o usuário por e-mail/push sobre a decisão
  está fora de escopo nesta feature; o usuário percebe o resultado ao tentar
  acessar novamente.
- **Sem reabertura de decisão**: nesta versão, uma conta reprovada não é reaberta
  para nova decisão pela interface de moderação; eventual reversão é tratada como
  evolução futura.
- **Volume baixo**: a fila de pendentes é pequena (uso pessoal/restrito), então não
  há requisitos especiais de paginação/escala além do padrão.

## Out of Scope

- Cadastro, autenticação e o ciclo de vida da identidade do usuário — feature
  **002 Identidade & Autenticação** (a 003 apenas decide sobre contas já criadas
  por ela).
- Cadastro/gestão de **jogos** (004) e **conferência** (005).
- Notificações ao usuário sobre a decisão (e-mail/push).
- Consulta/listagem de contas **já decididas** (histórico de aprovadas/reprovadas);
  esta feature mostra apenas a fila de pendentes. O histórico permanece preservado
  na trilha de auditoria (Princípio V) para consulta por outros meios.
- Reabertura/reversão de contas reprovadas e gestão avançada de papéis (promover/
  rebaixar administradores).
- Exclusão de dados do usuário (tratamento LGPD), que é responsabilidade
  transversal fora desta feature.
