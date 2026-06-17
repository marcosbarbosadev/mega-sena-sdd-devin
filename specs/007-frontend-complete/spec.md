# Feature Specification: Frontend Completo - Mega Sena Manager

**Feature Branch**: `007-frontend-complete`

**Created**: 2026-06-17

**Status**: Draft

**Input**: Criação de frontend completo para consumir todas as APIs do backend (features 001-005), fornecendo interface para sincronização de concursos, autenticação, moderação de contas, cadastro de jogos e conferência automática.

> Contexto: Esta feature implementa o **frontend completo** do Mega Sena Manager em Angular LTS, consumindo todas as APIs REST do backend (features 001-005). A aplicação fornece interface para usuários administrativos e usuários comuns, cobrindo todos os fluxos: autenticação via Firebase, aprovação de contas, cadastro de jogos e conferência automática. Ancorada nos princípios de UX, responsividade e integração seamless com o backend.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Tela de Login e Auto-cadastro (Priority: P1)

Um usuário não autenticado acessa a aplicação e vê a tela de login, onde pode autenticar-se via e-mail+senha ou conta Google. Após autenticar-se pela primeira vez, o sistema cria sua conta automaticamente e informa que ela está pendente de aprovação.

**Why this priority**: É o ponto de entrada da aplicação — sem login não há acesso a nenhuma funcionalidade.

**Independent Test**: Testar fluxo completo de login com e-mail+senha e Google, verificando que usuários novos são criados em estado pendente e usuários existentes logam corretamente.

**Acceptance Scenarios**:

1. **Given** um usuário não autenticado, **When** ele acessa a aplicação, **Then** ele vê a tela de login com opções de e-mail+senha e Google.
2. **Given** um usuário novo, **When** ele se autentica via Firebase pela primeira vez, **Then** uma conta é criada e ele vê mensagem de "conta pendente de aprovação".
3. **Given** um usuário existente com conta ativa, **When** ele se autentica, **Then** ele é redirecionado para o dashboard principal.
4. **Given** um usuário com conta pendente, **When** ele tenta acessar após login, **Then** ele vê tela informando que a conta aguarda aprovação.

---

### User Story 2 - Dashboard do Usuário (Priority: P1)

Um usuário autenticado com conta ativa acessa o dashboard principal e vê resumo de seus jogos, estatísticas básicas e acesso rápido às principais funcionalidades (cadastrar jogo, conferir jogos, ver perfil).

**Why this priority**: É a tela central do usuário — depois do login, é onde ele passa mais tempo.

**Independent Test**: Autenticar como usuário ativo com jogos cadastrados e verificar que o dashboard mostra resumo correto e links funcionais.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado com jogos, **When** ele acessa o dashboard, **Then** ele vê total de jogos, jogos aguardando sorteio, jogos já conferidos.
2. **Given** um usuário sem jogos, **When** ele acessa o dashboard, **Then** ele vê mensagem orientando a cadastrar o primeiro jogo e botão para ação.
3. **Given** o dashboard, **When** o usuário clica em "Cadastrar Jogo", **Then** ele é redirecionado para a tela de cadastro.
4. **Given** o dashboard, **When** o usuário clica em "Meus Jogos", **Then** ele é redirecionado para a lista de jogos.

---

### User Story 3 - Cadastro de Jogos (Priority: P1)

Um usuário acessa a tela de cadastro de jogos e pode criar uma aposta informando as dezenas manualmente ou solicitando geração automática. O sistema valida as dezenas, mostra o concurso vinculado e salva o jogo.

**Why this priority**: É o valor central do produto — sem cadastro de jogos não há o que conferir.

**Independent Test**: Testar cadastro manual com dezenas válidas e inválidas, e cadastro gerado com diferentes quantidades de dezenas.

**Acceptance Scenarios**:

1. **Given** um usuário na tela de cadastro, **When** ele seleciona "Manual" e informa 6 dezenas válidas, **Then** o jogo é salvo e o usuário vê confirmação com o concurso vinculado.
2. **Given** um usuário na tela de cadastro, **When** ele seleciona "Gerado" e escolhe 8 dezenas, **Then** o sistema gera 8 dezenas aleatórias, salva o jogo e mostra confirmação.
3. **Given** um usuário informando dezenas inválidas (repetidas ou fora de 1-60), **When** ele tenta salvar, **Then** o sistema mostra erro específico e não salva.
4. **Given** um usuário informando quantidade de dezenas fora de 6-9, **When** ele tenta salvar, **Then** o sistema mostra erro de validação.

---

### User Story 4 - Lista e Gestão de Jogos (Priority: P2)

Um usuário acessa a lista de seus jogos, onde vê todas as apostas cadastradas com dezenas, concurso vinculado e status (aguardando sorteio ou conferido). A partir da lista, pode editar ou excluir jogos que ainda não foram sorteados.

**Why this priority**: Necessária para o usuário acompanhar o que registrou, mas o valor principal (cadastrar e conferir) já existe sem ela.

**Independent Test**: Criar jogos em concursos sorteados e não sorteados, verificar lista correta e testar edição/exclusão.

**Acceptance Scenarios**:

1. **Given** um usuário com jogos cadastrados, **When** ele acessa "Meus Jogos", **Then** ele vê todos os jogos com dezenas, concurso vinculado e status.
2. **Given** um jogo cujo concurso não foi sorteado, **When** o usuário clica em editar, **Then** ele pode alterar as dezenas e salvar.
3. **Given** um jogo cujo concurso não foi sorteio, **When** o usuário clica em excluir, **Then** o jogo é removido após confirmação.
4. **Given** um jogo cujo concurso já foi sorteado, **When** o usuário tenta editar/excluir, **Then** os botões estão desabilitados e há indicação "somente leitura".

---

### User Story 5 - Conferência de Jogos (Priority: P1)

Um usuário acessa a tela de conferência e vê o resultado de seus jogos: acertos, faixa (sena/quina/quadra) e indicador de premiado. Jogos ainda não sorteados aparecem como "aguardando sorteio".

**Why this priority**: É o benefício principal para o usuário — saber o resultado sem conferência manual.

**Independent Test**: Criar jogos em concursos já sorteados e não sorteados, verificar que conferência mostra resultados corretos.

**Acceptance Scenarios**:

1. **Given** um usuário com jogos em concursos sorteados, **When** ele acessa a conferência, **Then** cada jogo mostra acertos, faixa e se foi premiado.
2. **Given** um jogo que acertou sena (6 acertos), **When** conferido, **Then** mostra destaque visual com faixa "Sena" e indicador de premiado.
3. **Given** um jogo que acertou quina (5) ou quadra (4), **When** conferido, **Then** mostra a faixa correspondente e indicador de premiado.
4. **Given** um jogo cujo concurso não foi sorteado, **When** conferido, **Then** mostra "Aguardando sorteio" sem resultado.

---

### User Story 6 - Dashboard do Administrador (Priority: P1)

Um administrador autenticado acessa o dashboard admin, onde vê resumo do sistema (total de usuários, contas pendentes, status da última sincronização) e acesso rápido às funções administrativas (moderação de contas, sincronização manual).

**Why this priority**: É o centro de controle administrativo — sem isso admins não conseguem gerenciar o sistema.

**Independent Test**: Autenticar como admin e verificar que dashboard mostra resumo correto e links para funções admin.

**Acceptance Scenarios**:

1. **Given** um administrador autenticado, **When** ele acessa o dashboard admin, **Then** ele vê total de usuários, contas pendentes e status da última sincronização.
2. **Given** o dashboard admin, **When** há contas pendentes, **Then** há indicador visual com contagem e link para moderação.
3. **Given** o dashboard admin, **When** o admin clica em "Sincronizar Agora", **Then** ele é redirecionado para a tela de sincronização.

---

### User Story 7 - Moderação de Contas (Priority: P1)

Um administrador acessa a lista de contas pendentes, onde vê e-mail, método de cadastro e data do pedido. A partir da lista, pode aprovar ou reprovar contas, com reprovação exigindo motivo obrigatório.

**Why this priority**: É a função principal do admin — sem isso não há como liberar acesso a novos usuários.

**Independent Test**: Criar contas pendentes e testar aprovação e reprovação com e sem motivo.

**Acceptance Scenarios**:

1. **Given** um administrador na tela de moderação, **When** ele acessa a lista de pendentes, **Then** ele vê todas as contas pendentes com e-mail, método e data.
2. **Given** uma conta pendente, **When** o admin clica em "Aprovar", **Then** a conta é aprovada e removida da lista de pendentes.
3. **Given** uma conta pendente, **When** o admin clica em "Reprovar" e informa motivo, **Then** a conta é reprovada e removida da lista.
4. **Given** uma conta pendente, **When** o admin tenta reprovar sem motivo, **Then** o sistema exige o motivo e não conclui a operação.

---

### User Story 8 - Sincronização Manual de Concursos (Priority: P2)

Um administrador acessa a tela de sincronização, onde vê o status da última sincronização (horário, resultado, concursos afetados) e pode disparar uma sincronização manual sob demanda.

**Why this priority**: Aumenta a capacidade operacional, mas a sincronização automática já mantém o sistema funcionando.

**Independent Test**: Verificar status da última sincronização e testar disparo manual.

**Acceptance Scenarios**:

1. **Given** um administrador na tela de sincronização, **When** ele acessa, **Then** ele vê horário, resultado e concursos afetados da última execução.
2. **Given** a tela de sincronização, **When** o admin clica em "Sincronizar Agora", **Then** a sincronização é disparada e o status é atualizado.
3. **Given** uma sincronização em andamento, **When** o admin tenta disparar outra, **Then** o sistema impede com mensagem de "já em andamento".

---

### User Story 9 - Perfil e Logout (Priority: P3)

Um usuário acessa seu perfil, onde vê seus dados (e-mail, papel, estado) e pode encerrar a sessão.

**Why this priority**: Higiene de segurança esperada, mas não bloqueia o uso inicial.

**Independent Test**: Acessar perfil e verificar dados corretos; testar logout e redirecionamento.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado, **When** ele acessa o perfil, **Then** ele vê seu e-mail, papel e estado da conta.
2. **Given** a tela de perfil, **When** o usuário clica em "Sair", **Then** a sessão é encerrada e ele é redirecionado para o login.

---

### Edge Cases

- **Backend indisponível**: aplicação mostra mensagem amigável e indica para tentar mais tarde
- **Firebase indisponível**: tela de login mostra mensagem de erro temporário
- **Sessão expirada**: usuário é redirecionado para login com mensagem de "sessão expirada"
- **Conta reprovada**: após login, usuário vê tela informando que acesso foi negado permanentemente
- **Sem concursos sincronizados**: tela de cadastro de jogos mostra mensagem de "serviço indisponível temporariamente"
- **Jogos com mesmo concurso**: lista mostra agrupamento ou indicador visual
- **Tela pequena (mobile)**: layout responsivo se adapta a telas pequenas
- **Navegação não autenticada**: usuário não logado é sempre redirecionado para login

## Requirements *(mandatory)*

### Functional Requirements

#### Autenticação (Consumo da Feature 002)

- **FR-001**: A aplicação MUST integrar com Firebase Authentication para login via e-mail+senha e Google.
- **FR-002**: A aplicação MUST armazenar o token JWT do Firebase localmente (localStorage/secure storage) para requisições autenticadas.
- **FR-003**: A aplicação MUST enviar o token JWT em todas as requisições autenticadas via header `Authorization: Bearer <token>`.
- **FR-004**: A aplicação MUST detectar token expirado/inválido e redirecionar para login.
- **FR-005**: Ao login bem-sucedido, a aplicação MUST chamar endpoint `/api/contas` para criar/atualizar conta local.
- **FR-006**: A aplicação MUST verificar estado da conta após login e mostrar tela apropriada (pendente/ativo/reprovado).
- **FR-007**: A aplicação MUST implementar logout chamando `/api/logout` e removendo token local.
- **FR-008**: A aplicação MUST proteger rotas que exigem autenticação via guards Angular.

#### Moderação (Consumo da Feature 003)

- **FR-009**: A aplicação MUST permitir que administradores listem contas pendentes via `/api/admin/moderacao/pendentes`.
- **FR-010**: A aplicação MUST permitir aprovação via POST `/api/admin/moderacao/{id}/aprovar`.
- **FR-011**: A aplicação MUST permitir reprovação via POST `/api/admin/moderacao/{id}/reprovar` com motivo obrigatório.
- **FR-012**: A aplicação MUST mostrar formulário de motivo ao reprovar e validar campo preenchido.
- **FR-013**: A aplicação MUST atualizar lista após aprovação/reprovação sem recarregar página.
- **FR-014**: A aplicação MUST restringir acesso a rotas de admin a usuários com papel ADMIN.

#### Jogos (Consumo da Feature 004)

- **FR-015**: A aplicação MUST permitir listagem de jogos do usuário via GET `/api/jogos`.
- **FR-016**: A aplicação MUST permitir cadastro via POST `/api/jogos` com modo INFORMADO ou GERADO.
- **FR-017**: A aplicação MUST implementar interface para selecionar dezenas manualmente (6-9 números).
- **FR-018**: A aplicação MUST implementar interface para gerar dezenas automaticamente informando quantidade.
- **FR-019**: A aplicação MUST validar entrada localmente antes de enviar (feedback imediato).
- **FR-020**: A aplicação MUST mostrar o concurso vinculado após cadastro bem-sucedido.
- **FR-021**: A aplicação MUST permitir edição via PUT `/api/jogos/{id}` apenas se jogo editável.
- **FR-022**: A aplicação MUST permitir exclusão via DELETE `/api/jogos/{id}` apenas se jogo editável.
- **FR-023**: A aplicação MUST desabilitar edição/exclusão para jogos não editáveis (concurso já sorteado).
- **FR-024**: A aplicação MUST mostrar confirmação antes de excluir jogo.

#### Conferência (Consumo da Feature 005)

- **FR-025**: A aplicação MUST permitir conferência de jogo específico via GET `/api/conferencia/{jogoId}`.
- **FR-026**: A aplicação MUST permitir conferência de todos os jogos via GET `/api/conferencia`.
- **FR-027**: A aplicação MUST mostrar acertos, faixa e indicador de premiado para jogos conferidos.
- **FR-028**: A aplicação MUST mostrar "Aguardando sorteio" para jogos não conferidos.
- **FR-029**: A aplicação MUST destacar visualmente jogos premiados (sena/quina/quadra).
- **FR-030**: A aplicação MUST implementar atualização automática (polling ou refresh) para conferências.

#### Concursos (Consumo da Feature 001)

- **FR-031**: A aplicação MUST exibir o último concurso sincronizado via GET `/api/concursos/latest`.
- **FR-032**: A aplicação MUST exibir detalhes de concurso específico via GET `/api/concursos/{numero}`.
- **FR-033**: A aplicação MUST mostrar dezenas sorteadas formatadas visualmente (ex: em bolinhas numeradas).

#### Sincronização (Consumo da Feature 001 - Admin)

- **FR-034**: A aplicação MUST permitir consulta de status via GET `/api/admin/sync/status`.
- **FR-035**: A aplicação MUST permitir disparo manual via POST `/api/admin/sync/run`.
- **FR-036**: A aplicação MUST mostrar indicador de loading durante sincronização.
- **FR-037**: A aplicação MUST atualizar status automaticamente após sincronização completar.

#### UX/UI Geral

- **FR-038**: A aplicação MUST ser responsiva e funcionar em desktop e mobile.
- **FR-039**: A aplicação MUST mostrar indicadores de loading em operações assíncronas.
- **FR-040**: A aplicação MUST tratar erros de API com mensagens amigáveis ao usuário.
- **FR-041**: A aplicação MUST implementar navegação com Angular Router.
- **FR-042**: A aplicação MUST ter layout consistente com header, navegação e conteúdo principal.
- **FR-043**: A aplicação MUST implementar feedback visual para ações (sucesso/erro).
- **FR-044**: A aplicação MUST manter estado de navegação (back button funciona corretamente).

### Non-Functional Requirements

- **NFR-001**: A aplicação MUST usar Angular LTS (versão estável mais recente).
- **NFR-002**: A aplicação MUST usar TypeScript para type safety.
- **NFR-003**: A aplicação MUST seguir Angular style guide oficial.
- **NFR-004**: A aplicação MUST ter build otimizado para produção (lazy loading, tree shaking).
- **NFR-005**: A aplicação MUST implementar lazy loading de rotas.
- **NFR-006**: A aplicação MUST ter teste unitários para componentes e services.
- **NFR-007**: A aplicação MUST ter testes E2E para fluxos principais.
- **NFR-008**: A aplicação MUST ser acessível (WCAG 2.1 AA onde aplicável).
- **NFR-009**: A aplicação MUST ter performance > 90 no Lighthouse.
- **NFR-010**: A aplicação MUST suportar browsers modernos (Chrome, Firefox, Safari, Edge).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um usuário consegue completar o fluxo de login e chegar ao dashboard em menos de 10 segundos.
- **SC-002**: Um usuário consegue cadastrar um jogo (informado ou gerado) em men