# Feature Specification: Identidade & Autenticação

**Feature Branch**: `002-identity-auth`

**Created**: 2026-05-31

**Status**: Draft

**Input**: User description: "002 Identidade & autenticação"

> Contexto: feature **002** do roadmap (`@draft/roadmap.md`). É a fundação de
> identidade que sustenta o isolamento multiusuário de todas as features de
> usuário. Ancorada nos princípios **VII (Identidade Gerenciada Externamente)**,
> **VIII (Design Cloud-Native)**, **IX (Acesso Somente Após Aprovação)** e
> **I (Isolamento Multiusuário)** da constituição.

## Clarifications

### Session 2026-06-01

- Q: Mesma pessoa com e-mail+senha e Google no mesmo e-mail — como tratar o segundo método? → A: Auto-vincular o login Google à conta existente, somente quando o provedor confirmar que o e-mail do Google é verificado.
- Q: Cadastro Google novo com e-mail não verificado pelo provedor — como tratar? → A: Recusar o cadastro/login até o e-mail estar verificado, com mensagem orientando a verificar no provedor.
- Q: "Bloqueada" é um estado distinto de "reprovada"? → A: Não — três estados apenas (pendente/ativo/reprovado); "bloqueada" é sinônimo de sem-acesso, sem novo estado nesta feature.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Auto-cadastro de nova conta (Priority: P1)

Uma pessoa que ainda não usa o sistema cria sua própria conta, escolhendo entre
informar e-mail e senha ou usar uma conta Google. Ao concluir, a conta é criada
em estado **pendente de aprovação** — sem acesso aos recursos de usuário — e a
pessoa é informada de que precisa aguardar a confirmação de um administrador.

**Why this priority**: Sem a porta de entrada não há usuários no sistema. É o
primeiro passo do ciclo de vida da conta e pré-requisito de tudo que vem depois.

**Independent Test**: Pode ser testado isoladamente registrando uma nova conta
(por e-mail+senha e por Google) e verificando que a conta passa a existir em
estado pendente, sem que nenhuma credencial de senha seja armazenada pela
aplicação.

**Acceptance Scenarios**:

1. **Given** um visitante não cadastrado, **When** ele se cadastra informando
   e-mail e senha válidos, **Then** uma conta é criada em estado **pendente** e
   ele recebe a mensagem de que o acesso depende de aprovação de um administrador.
2. **Given** um visitante não cadastrado, **When** ele se cadastra usando uma
   conta Google, **Then** uma conta é criada em estado **pendente** vinculada
   àquela identidade Google, sem solicitar senha.
3. **Given** um e-mail já vinculado a uma conta existente, **When** alguém tenta
   se cadastrar novamente com o mesmo e-mail, **Then** o sistema recusa a criação
   duplicada e não altera a conta existente.

---

### User Story 2 - Autenticar e acessar conforme o estado da conta (Priority: P1)

Uma pessoa com conta autentica-se (por e-mail+senha ou Google). Se a conta
estiver **ativa**, o sistema estabelece sua identidade isolada e libera o acesso
às áreas de usuário. Se estiver **pendente** ou **reprovada**, o sistema nega o
acesso aos recursos de usuário e informa o estado, mesmo que a autenticação de
identidade tenha ocorrido.

**Why this priority**: É o coração da feature — transformar uma identidade
verificada na identidade isolada que protege os dados de cada usuário e aplicar a
porta de segurança da aprovação.

**Independent Test**: Pode ser testado autenticando uma conta ativa e
confirmando que as requisições subsequentes resolvem para exatamente aquele
usuário; e autenticando uma conta pendente e confirmando que o acesso a recursos
de usuário é bloqueado.

**Acceptance Scenarios**:

1. **Given** uma conta **ativa**, **When** o usuário se autentica com credenciais
   válidas, **Then** o sistema estabelece sua identidade e toda requisição
   autenticada subsequente é atribuída exclusivamente a ele.
2. **Given** uma conta **pendente**, **When** o usuário se autentica com
   credenciais válidas, **Then** o sistema nega o acesso aos recursos de usuário e
   informa que a conta aguarda aprovação.
3. **Given** uma conta **reprovada** (sem acesso), **When** o usuário tenta
   acessar, **Then** o acesso é negado e o estado é comunicado.
4. **Given** credenciais inválidas ou identidade não verificável, **When** a
   autenticação é tentada, **Then** o acesso é negado sem revelar se o e-mail
   existe.
5. **Given** uma requisição autenticada, **When** ela é processada, **Then** a
   identidade usada para autorizar e filtrar dados é derivada da sessão verificada
   no servidor, nunca de um identificador enviado pelo cliente.

---

### User Story 3 - Encerrar a sessão (Priority: P3)

Um usuário autenticado encerra sua sessão e, a partir daí, precisa autenticar-se
novamente para acessar áreas protegidas.

**Why this priority**: Higiene de segurança esperada, mas não bloqueia o uso
inicial do sistema.

**Independent Test**: Autenticar, encerrar a sessão e confirmar que uma nova
tentativa de acesso a área protegida exige autenticação novamente.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado, **When** ele encerra a sessão, **Then**
   acessos subsequentes a áreas protegidas exigem nova autenticação.

---

### Edge Cases

- **Cadastro Google sem e-mail verificado**: o sistema recusa o cadastro/login
  enquanto o provedor não indicar o e-mail como verificado, exibindo mensagem que
  orienta a verificar o e-mail no provedor; nenhuma conta é criada nesse caso (ver
  FR-015).
- **Mesma pessoa, dois métodos**: alguém se cadastra por e-mail+senha e depois
  tenta entrar com Google usando o mesmo e-mail — o sistema deve evitar duas
  contas distintas para a mesma pessoa (ver Assumptions).
- **Aprovação ocorre durante a sessão**: uma conta pendente que é aprovada por um
  admin passa a ter acesso sem precisar recadastrar.
- **Conta reprovada que tenta recadastrar**: o sistema não deve permitir burlar a
  reprovação criando uma nova conta com o mesmo e-mail.
- **Indisponibilidade do provedor de identidade**: tentativa de cadastro/login
  enquanto o provedor está fora — o sistema degrada com mensagem clara, sem criar
  contas inconsistentes.
- **Primeiro administrador**: como existe um admin inicial se ninguém pode aprovar
  o primeiro? (ver Assumptions — provisionamento fora de banda).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir que um visitante crie sua própria conta
  informando e-mail e senha **ou** usando uma conta Google.
- **FR-002**: O sistema MUST delegar a autenticação e a guarda de credenciais a um
  provedor de identidade gerenciado, NÃO armazenando senhas na aplicação
  (Princípio VII).
- **FR-003**: Toda conta recém-criada MUST nascer em estado **pendente**, sem
  acesso a nenhum recurso de usuário, até ser aprovada (Princípio IX).
- **FR-004**: O sistema MUST permitir que um usuário se autentique por e-mail+senha
  ou por conta Google.
- **FR-005**: Em toda requisição autenticada, o sistema MUST validar a identidade
  no servidor e derivar dela o identificador do usuário, nunca confiando em
  identificador enviado pelo cliente (Princípios I e VII).
- **FR-006**: O sistema MUST conceder acesso aos recursos de usuário apenas a
  contas em estado **ativo**; contas pendentes ou reprovadas MUST ser bloqueadas
  com mensagem informando o estado (Princípio IX).
- **FR-007**: O sistema MUST registrar, para cada conta, ao menos: identificador
  da identidade no provedor, e-mail, papel (usuário/administrador) e estado
  (pendente/ativo/reprovado).
- **FR-008**: O sistema MUST impedir a criação de contas duplicadas para o mesmo
  e-mail/identidade.
- **FR-009**: O sistema MUST distinguir o papel **administrador** do papel
  **usuário comum**, para que features posteriores restrinjam ações
  administrativas (a moderação de contas em si é a feature 003).
- **FR-010**: O sistema MUST permitir que um usuário autenticado encerre sua
  sessão, exigindo nova autenticação para acessos posteriores.
- **FR-011**: O sistema MUST registrar trilha de auditoria dos eventos de
  identidade (cadastro, autenticação bem-sucedida/negada, encerramento de sessão),
  sem expor credenciais ou dados pessoais sensíveis em texto claro (Princípio V).
- **FR-012**: O sistema MUST recusar autenticação inválida sem revelar se o e-mail
  informado corresponde a uma conta existente.
- **FR-013**: O sistema MUST degradar graciosamente quando o provedor de
  identidade estiver indisponível, comunicando o erro sem criar contas em estado
  inconsistente.
- **FR-014**: Quando uma identidade Google chegar com um e-mail já vinculado a uma
  conta existente, o sistema MUST vincular esse método de login à mesma conta
  (evitando conta duplicada) **somente se** o provedor indicar o e-mail do Google
  como verificado; caso contrário, o vínculo automático NÃO ocorre.
- **FR-015**: O sistema MUST recusar a criação de conta a partir de uma identidade
  Google cujo e-mail não esteja verificado pelo provedor, sem persistir conta, e
  MUST orientar a pessoa a verificar o e-mail no provedor.

### Key Entities *(include if feature involves data)*

- **Conta de usuário**: representa a pessoa no sistema. Atributos essenciais:
  referência à identidade no provedor gerenciado, e-mail, papel
  (usuário/administrador), estado (pendente/ativo/reprovado) e marcos temporais
  (criação, última autenticação). É a entidade dona à qual os dados das demais
  features (jogos, conferências) serão vinculados para garantir isolamento.
- **Identidade do provedor** (externa): a credencial e o método de login
  (e-mail+senha ou Google) vivem no provedor de identidade gerenciado; a aplicação
  apenas referencia o identificador emitido por ele — não guarda senhas.
- **Evento de identidade** (auditoria): registro de cadastro, autenticação e
  encerramento de sessão, com quem, o quê e quando, para a trilha de auditoria.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Uma pessoa consegue concluir o auto-cadastro (por qualquer um dos
  dois métodos) em menos de 2 minutos.
- **SC-002**: 100% das requisições autenticadas resolvem para exatamente uma
  conta; em nenhum cenário uma requisição acessa dados de outro usuário.
- **SC-003**: 100% das tentativas de acesso a recursos de usuário por contas
  pendentes ou reprovadas são bloqueadas.
- **SC-004**: A aplicação não armazena nenhuma senha — uma inspeção dos dados
  persistidos pela aplicação não revela material de credencial.
- **SC-005**: Um usuário com conta ativa consegue autenticar-se e chegar à área
  protegida em poucos segundos, em uso típico.
- **SC-006**: Nenhuma mensagem de erro de autenticação revela a existência de uma
  conta a partir do e-mail informado.

## Assumptions

- **Provedor de identidade a definir**: o provedor gerenciado concreto (ex.: AWS
  Cognito ou Google Identity Platform) ainda não foi escolhido; a especificação
  trata a identidade de forma abstrata, conforme o Princípio VII. A escolha é
  decisão de plano/constituição, não desta spec.
- **Verificação de e-mail pelo provedor**: assume-se que a verificação de e-mail
  (no cadastro por e-mail+senha) é responsabilidade do provedor de identidade; a
  aprovação por administrador (Princípio IX / feature 003) é uma etapa adicional e
  independente da verificação de e-mail.
- **Aprovação de contas é a feature 003**: esta feature cria a conta em estado
  pendente e bloqueia o acesso enquanto não estiver ativa, mas a interface e o
  fluxo de o administrador aprovar/reprovar pertencem à feature 003. Até a 003
  existir, contas só se tornam ativas por provisionamento fora de banda.
- **Administrador inicial provisionado fora de banda**: o primeiro administrador é
  criado/ativado por provisionamento administrativo (seed/infra), já que não há
  outro admin para aprová-lo.
- **Identidade única por pessoa**: assume-se uma conta por e-mail. Se a mesma
  pessoa usar e-mail+senha e Google com o mesmo e-mail, o sistema vincula
  automaticamente o login Google à conta existente (uma conta, dois métodos),
  **desde que o provedor confirme que o e-mail do Google é verificado**; sem essa
  verificação, o vínculo automático não ocorre (ver FR-014).
- **Login social restrito ao Google**: nesta versão, o único provedor social
  suportado é o Google; outros provedores estão fora de escopo.
- **Sem auto-registro livre de acesso**: por desenho (Princípio IX), cadastrar-se
  não concede acesso; o cadastro apenas enfileira a conta para aprovação.
- **Primeiro recurso de usuário**: o recurso de usuário concreto desta feature é
  `GET /api/perfil` (dados da própria conta), usado para validar o gate de estado e o
  isolamento; os recursos de jogos/conferências chegam nas features 004/005 sob o mesmo
  gate.

## Out of Scope

- Fluxo de o administrador **aprovar/reprovar** contas (interface e ações) — é a
  feature **003 Aprovação de contas (admin)**.
- Cadastro e gestão de **jogos/apostas** — feature **004**.
- Conferência de jogos — feature **005**.
- Recuperação/redefinição de senha e gestão de credenciais, que são tratadas pelo
  provedor de identidade gerenciado, não pela aplicação.
- Escolha e configuração do provedor de identidade concreto (decisão de plano).
