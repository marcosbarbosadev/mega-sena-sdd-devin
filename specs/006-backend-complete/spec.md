# Feature Specification: Backend Completo - Mega Sena Manager

**Feature Branch**: `006-backend-complete`

**Created**: 2026-06-17

**Status**: Draft

**Input**: Consolidação de todas as features de backend implementadas (001-005): Sincronização de concursos, Identidade & Autenticação, Aprovação de Contas, Cadastro de Jogos e Conferência Automática.

> Contexto: Esta feature representa o **backend completo** do Mega Sena Manager, consolidando todas as funcionalidades implementadas nas features 001-005. É um serviço Spring Boot que fornece APIs REST para gerenciamento de concursos da Mega Sena, autenticação de usuários via Firebase, moderação de contas, cadastro de jogos e conferência automática de apostas. Ancorado nos princípios de isolamento multiusuário, integridade de dados, observabilidade e design cloud-native.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sincronização automática de concursos (Priority: P1)

O sistema mantém os dados dos concursos da Mega Sena atualizados automaticamente, sincronizando com a API oficial da Caixa de forma agendada (diariamente) e permitindo sincronização manual por administradores.

**Why this priority**: É a base de dados do sistema — sem concursos atualizados, nenhuma outra funcionalidade (cadastro de jogos, conferência) funciona corretamente.

**Acceptance Scenarios**:

1. **Given** a fonte oficial publicou um concurso mais novo, **When** a sincronização agendada executa, **Then** o novo concurso é armazenado localmente com número, data, dezenas sorteadas e valor do prêmio.
2. **Given** o concurso mais recente já está armazenado, **When** a sincronização executa, **Then** nenhum dado é duplicado (operação idempotente).
3. **Given** a fonte oficial está indisponível, **When** a sincronização falha, **Then** o sistema continua servindo os últimos dados válidos e registra a falha para retry posterior.

---

### User Story 2 - Autenticação e auto-cadastro de usuários (Priority: P1)

Uma pessoa cria sua conta usando e-mail+senha ou conta Google via Firebase Authentication. A conta é criada em estado pendente e aguarda aprovação administrativa.

**Why this priority**: Sem autenticação não há isolamento multiusuário — é a fundação de segurança do sistema.

**Acceptance Scenarios**:

1. **Given** um visitante não cadastrado, **When** ele se cadastra via Firebase, **Then** uma conta é criada em estado **pendente** com o identificador do Firebase e e-mail verificado.
2. **Given** uma conta **ativa**, **When** o usuário se autentica com token válido, **Then** o sistema estabelece sua identidade e toda requisição subsequente é atribuída exclusivamente a ele.
3. **Given** uma conta **pendente** ou **reprovada**, **When** o usuário tenta acessar recursos protegidos, **Then** o acesso é negado com indicação do estado da conta.

---

### User Story 3 - Moderação de contas por administrador (Priority: P1)

Um administrador lista contas pendentes e aprova ou reprova o acesso. Aprovações ativam a conta; reprovações (com motivo obrigatório) bloqueiam o acesso definitivamente.

**Why this priority**: É a porta de segurança que controla quem pode usar o sistema, cumprindo o princípio de acesso somente após aprovação.

**Acceptance Scenarios**:

1. **Given** existem contas em estado **pendente**, **When** um administrador acessa a fila de aprovação, **Then** ele vê todas as contas pendentes com e-mail, método de cadastro e data do pedido.
2. **Given** uma conta **pendente**, **When** um administrador a aprova, **Then** a conta passa a **ativo** e o usuário ganha acesso às áreas de usuário.
3. **Given** uma conta **pendente**, **When** um administrador a reprova informando motivo, **Then** a conta passa a **reprovada** e permanece sem acesso.

---

### User Story 4 - Cadastro de jogos por usuários (Priority: P1)

Um usuário com conta ativa registra apostas de 6 a 9 dezenas, informando todas as dezenas ou pedindo geração automática. O jogo é vinculado ao próximo concurso em aberto e isolado por usuário.

**Why this priority**: É o valor central do produto — sem jogos não há o que conferir.

**Acceptance Scenarios**:

1. **Given** um usuário com conta **ativa**, **When** ele cadastra um jogo informando N dezenas (N ∈ {6,7,8,9}), **Then** o jogo é salvo, vinculado ao próximo concurso em aberto e atribuído ao usuário.
2. **Given** um usuário com conta ativa, **When** ele pede um jogo **gerado** de N dezenas, **Then** o sistema gera N dezenas distintas entre 1 e 60 e salva o jogo.
3. **Given** um jogo cujo concurso vinculado **não** foi sorteado, **When** o dono edita as dezenas, **Then** o jogo é atualizado mantendo o mesmo concurso vinculado.
4. **Given** um usuário lista seus jogos, **Then** ele vê **apenas** os próprios jogos (isolamento garantido).

---

### User Story 5 - Conferência automática de jogos (Priority: P1)

Quando o concurso vinculado a um jogo é sorteado, o sistema confere automaticamente a aposta, mostrando acertos, faixa (sena/quina/quadra) e se foi premiado, sem valores monetários.

**Why this priority**: É o benefício principal para o usuário — saber o resultado sem conferência manual.

**Acceptance Scenarios**:

1. **Given** um jogo cujo concurso foi sorteado, **When** o usuário confere, **Then** o sistema mostra o número de acertos, a faixa e se foi premiado.
2. **Given** um jogo que acertou 6 dezenas, **When** conferido, **Then** a faixa é **sena** e o jogo consta como **premiado**.
3. **Given** um jogo que acertou 5 (quina) ou 4 (quadra) dezenas, **When** conferido, **Then** a faixa correspondente é mostrada e o jogo consta como **premiado**.
4. **Given** um jogo cujo concurso ainda **não** foi sorteado, **When** consultado, **Then** aparece como **aguardando sorteio**.

---

### Edge Cases

- **Fonte oficial indisponível**: sistema degrada graciosamente, servindo últimos dados válidos
- **Firebase indisponível**: autenticação degrada com mensagem clara, sem criar contas inconsistentes
- **Decisão concorrente**: dois admins tentando decidir a mesma conta — apenas a primeira decisão vale
- **Sorteio entre cadastro e edição**: jogo vira somente leitura quando o concurso é sorteado
- **Resultado corrigido na fonte**: conferência reflete sempre o dado vigente (determinística)
- **Concurso ainda não sorteado**: conferência mostra "aguardando sorteio", nunca resultado
- **Acesso cruzado**: tentativa de acessar dados de outro usuário é sempre negada (isolamento)

## Requirements *(mandatory)*

### Functional Requirements

#### Sincronização de Concursos (Feature 001)

- **FR-001**: O sistema MUST obter da API oficial da Caixa os dados de cada concurso: número, data, dezenas sorteadas e valor do prêmio.
- **FR-002**: O sistema MUST armazenar localmente os dados de cada concurso de forma persistente em MySQL.
- **FR-003**: O sistema MUST executar sincronização agendada via cron (configurável, default diário).
- **FR-004**: O sistema MUST detectar e importar novos concursos assim que disponíveis na fonte.
- **FR-005**: O sistema MUST suportar carga inicial do histórico completo de forma retomável.
- **FR-006**: A sincronização MUST ser idempotente — reprocessar o mesmo concurso não cria duplicatas.
- **FR-007**: O sistema MUST continuar servindo dados válidos quando a fonte estiver indisponível.
- **FR-008**: O sistema MUST registrar cada execução de sincronização com horário, resultado e concursos afetados.
- **FR-009**: O sistema MUST tentar novamente automaticamente após falhas transitórias (retry).
- **FR-010**: O sistema MUST validar integridade dos dados (6 dezenas distintas entre 1-60) antes de persistir.
- **FR-011**: O perfil admin MUST poder consultar status e disparar sincronização manual.
- **FR-012**: O sistema MUST preencher lacunas de concursos para garantir sequência contínua.

#### Identidade & Autenticação (Feature 002)

- **FR-013**: O sistema MUST permitir auto-cadastro via Firebase Authentication (e-mail+senha ou Google).
- **FR-014**: O sistema MUST delegar autenticação e guarda de credenciais ao Firebase, NÃO armazenando senhas.
- **FR-015**: Toda conta recém-criada MUST nascer em estado **pendente**, sem acesso a recursos de usuário.
- **FR-016**: O sistema MUST permitir autenticação via token JWT do Firebase.
- **FR-017**: Em toda requisição autenticada, o sistema MUST validar o token no servidor e derivar o identificador do usuário.
- **FR-018**: O sistema MUST conceder acesso apenas a contas em estado **ativo**.
- **FR-019**: O sistema MUST registrar para cada conta: identificador Firebase, e-mail, papel (usuário/admin) e estado.
- **FR-020**: O sistema MUST impedir criação de contas duplicadas para o mesmo e-mail/identidade.
- **FR-021**: O sistema MUST distinguir papel **administrador** de **usuário comum**.
- **FR-022**: O sistema MUST permitir que um usuário encerre sua sessão.
- **FR-023**: O sistema MUST registrar trilha de auditoria dos eventos de identidade (cadastro, autenticação, logout).
- **FR-024**: O sistema MUST recusar autenticação inválida sem revelar se o e-mail corresponde a uma conta.
- **FR-025**: O sistema MUST degradar graciosamente quando o Firebase estiver indisponível.
- **FR-026**: Quando identidade Google chegar com e-mail já vinculado, o sistema MUST vincular à mesma conta se o Firebase indicar e-mail verificado.
- **FR-027**: O sistema MUST recusar criação de conta com e-mail não verificado pelo Firebase.

#### Aprovação de Contas (Feature 003)

- **FR-028**: O sistema MUST permitir que um **administrador** liste contas em estado **pendente**.
- **FR-029**: O sistema MUST restringir todas as ações de moderação ao papel **administrador**.
- **FR-030**: O sistema MUST permitir que um administrador **aprove** uma conta pendente, transicionando-a para **ativo**.
- **FR-031**: O sistema MUST permitir que um administrador **reprove** uma conta pendente, transicionando-a para **reprovado**.
- **FR-032**: O sistema MUST exigir **motivo** ao reprovar; aprovação NÃO exige motivo.
- **FR-033**: O sistema MUST permitir transições apenas a partir de estado **pendente**; ativo/reprovado são terminais.
- **FR-034**: Após aprovação, a conta MUST passar a ter acesso aos recursos de usuário.
- **FR-035**: O sistema MUST registrar trilha de auditoria de cada decisão (admin, conta, decisão, momento, motivo).
- **FR-036**: O sistema MUST garantir que decisões concorrentes não produzam estados inconsistentes.
- **FR-037**: O sistema MUST derivar a identidade do administrador da sessão verificada no servidor.

#### Cadastro de Jogos (Feature 004)

- **FR-038**: O sistema MUST permitir que usuário com conta **ativa** cadastre jogo escolhendo quantidade de dezenas (6–9).
- **FR-039**: O sistema MUST permitir cadastro informando todas as dezenas OU pedindo geração automática.
- **FR-040**: O sistema MUST aceitar todas as dezenas OU nenhuma — nunca subconjunto parcial.
- **FR-041**: O sistema MUST validar toda aposta: quantidade ∈ {6,7,8,9}, dezenas ∈ [1,60], sem repetição.
- **FR-042**: No cadastro, o sistema MUST vincular o jogo ao **próximo concurso em aberto**, gravando o número imutavelmente.
- **FR-043**: Todo jogo MUST pertencer ao usuário autenticado; operações MUST ser restritas ao dono.
- **FR-044**: O sistema MUST permitir que o usuário liste os próprios jogos.
- **FR-045**: O sistema MUST permitir editar/excluir jogo enquanto o concurso vinculado não foi sorteado.
- **FR-046**: Após sorteio, o jogo é **somente leitura** (edição/exclusão bloqueadas).
- **FR-047**: O acesso a operações de jogos MUST exigir conta **ativa**.
- **FR-048**: O sistema MUST registrar trilha de auditoria das operações de jogo.

#### Conferência Automática (Feature 005)

- **FR-049**: Quando existir resultado do concurso vinculado, o sistema MUST conferir o jogo comparando dezenas.
- **FR-050**: A conferência MUST informar a **quantidade de acertos** (0–6).
- **FR-051**: A conferência MUST classificar a **faixa**: sena (6), quina (5), quadra (4); abaixo de 4, nenhuma.
- **FR-052**: A conferência MUST indicar se o jogo foi **premiado** (≥ 4 acertos), sem valores monetários.
- **FR-053**: A conferência só MUST estar disponível quando o concurso vinculado foi sorteado.
- **FR-054**: A conferência MUST ser determinística e idempotente.
- **FR-055**: A conferência MUST ser isolada por usuário — usuário vê apenas próprios jogos.
- **FR-056**: O acesso à conferência MUST exigir conta **ativa**.
- **FR-057**: O usuário MUST poder conferir jogo específico E consultar conferência do conjunto.
- **FR-058**: A conferência MUST ser somente leitura sobre jogo e concurso.
- **FR-059**: O resultado MUST ser computado sob demanda e gerar trilha de auditoria (uma vez por jogo×concurso).

### Non-Functional Requirements

- **NFR-001**: O sistema MUST usar Spring Boot 3.3.5 com Java 21.
- **NFR-002**: O sistema MUST persistir dados em MySQL 8 com migrações versionadas via Flyway.
- **NFR-003**: O sistema MUST implementar segurança via Spring Security com validação de tokens JWT do Firebase.
- **NFR-004**: O sistema MUST garantir isolamento multiusuário completo entre dados de diferentes usuários.
- **NFR-005**: O sistema MUST implementar retry/resilience via Resilience4j para chamadas externas.
- **NFR-006**: O sistema MUST ter testes de integração usando Testcontainers e WireMock.
- **NFR-007**: O sistema MUST ser cloud-native e configurável via variáveis de ambiente.
- **NFR-008**: O sistema MUST registrar trilha de auditoria para todas as operações sensíveis.
- **NFR-009**: O sistema MUST tratar dados de concursos como globais e somente leitura para usuários.
- **NFR-010**: O sistema MUST degradar graciosamente em caso de indisponibilidade de serviços externos.

### Key Entities

#### Concurso (Feature 001)
- Representa um sorteio oficial da Mega Sena
- Atributos: número, data, dezenas sorteadas (6 números), valor do prêmio
- É dado de referência global e imutável após confirmado

#### Usuário/Conta (Feature 002)
- Representa a pessoa no sistema
- Atributos: identificador Firebase, e-mail, papel (usuário/admin), estado (pendente/ativo/reprovado)
- É a entidade dona para isolamento multiusuário

#### Decisão de Moderação (Feature 003)
- Registro de aprovação/reprovação de conta
- Atributos: admin responsável, conta afetada, decisão, momento, motivo (obrigatório na reprovação)

#### Jogo/Aposta (Feature 004)
- Representa uma aposta do usuário
- Atributos: dono (usuário), concurso vinculado (número fixo), dezenas (6–9 números)
- Isolado por usuário, editável até sorteio do concurso vinculado

#### Evento de Conferência (Feature 005)
- Registro de conferência de jogo
- Atributos: usuário, jogo, concurso, resultado (acertos/faixa/premiado), momento
- Único por jogo×concurso (idempotente)

#### Eventos de Auditoria (Transversal)
- EventoIdentidade: cadastro, autenticação, logout
- EventoJogo: cadastro, edição, exclusão de jogos
- SyncRun: execuções de sincronização de concursos

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um novo concurso oficial fica disponível em até 24 horas após divulgação.
- **SC-002**: Após carga inicial, 100% dos concursos sorteados estão disponíveis localmente.
- **SC-003**: 100% das requisições autenticadas resolvem para exatamente uma conta (isolamento garantido).
- **SC-004**: 100% das tentativas de acesso por contas pendentes/reprovadas são bloqueadas.
- **SC-005**: A aplicação não armazena senhas — apenas identificadores do Firebase.
- **SC-006**: Um admin consegue revisar e decidir (aprovar/reprovar) conta em menos de 1 minuto.
- **SC-007**: 100% das decisões de moderação geram registro de auditoria rastreável.
- **SC-008**: Um usuário consegue cadastrar um jogo em menos de 1 minuto.
- **SC-009**: 100% dos jogos pertencem e são visíveis apenas ao dono.
- **SC-010**: 100% das tentativas de editar/excluir jogo pós-sorteio são bloqueadas.
- **SC-011**: Para jogo com concurso sorteado, acertos correspondem exatamente à interseção dezenas.
- **SC-012**: A conferência é idempotente — repetição retorna sempre o mesmo resultado.
- **SC-013**: 100% das conferências respeitam isolamento — usuário nunca vê jogo de outro.
- **SC-014**: Durante indisponibilidade da fonte, 100% das consultas a dados sincronizados funcionam.
- **SC-015**: 100% das operações sensíveis geram trilha de auditoria completa.

## Assumptions

- **Provedor de identidade**: Firebase Authentication é o provedor gerenciado concreto escolhido.
- **Verificação de e-mail**: Firebase é responsável pela verificação de e-mail; aprovação admin é etapa adicional.
- **Admin inicial**: Primeiro administrador é criado via provisionamento fora de banda (seed/infra).
- **Identidade única**: Uma conta por e-mail; vínculo automático de Google à conta existente se e-mail verificado.
- **Login social restrito**: Apenas Google é suportado nesta versão.
- **Próximo concurso em aberto**: Inferido como o próximo ainda não sorteado após o último conhecido.
- **Janela de edição**: Jogo editável apenas até sorteio do concurso vinculado.
- **Volume baixo**: Sistema para uso pessoal/restrito, sem requisitos especiais de escala.
- **Duplicatas permitidas**: Usuário pode ter múltiplos jogos com mesmas dezenas.
- **Geração uniforme**: Geração automática usa aleatoriedade uniforme entre 1–60.
- **Escopo 6–9 dezenas**: Restrição deliberada de produto (vs. 6–15 oficial).
- **Premiado = ≥ 4 acertos**: Limiar definido na visão; sem valores de prêmio.
- **Fora de escopo**: Valores de prêmio, premiação detalhada, acúmulo, notificações, apostas compartilhadas.

## Out of Scope

- Frontend Angular (esta spec cobre apenas o backend)
- Valores de prêmio, premiação detalhada e acúmulo financeiro
- Apostas com seleção parcial de dezenas
- Apostas com mais de 9 dezenas (10–15), bolão/cotas
- Escolha manual de qual concurso apostar (sempre próximo em aberto)
- Pagamento/registro oficial na Caixa
- Notificações ao usuário (e-mail/push)
- Reabertura de contas reprovadas
- Histórico de conferências anteriores a correção de resultado
- Gestão avançada de papéis (promover/rebaixar admins)
- Exclusão de dados (LGPD)
- Outros provedores sociais além do Google