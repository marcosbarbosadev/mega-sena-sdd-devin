# Checklist de Requisitos - Frontend Completo

## Requisitos Funcionais - Autenticação (FR-001 a FR-008)

- [ ] **FR-001**: A aplicação integra com Firebase Authentication para login via e-mail+senha e Google
- [ ] **FR-002**: A aplicação armazena o token JWT do Firebase localmente (localStorage/secure storage)
- [ ] **FR-003**: A aplicação envia o token JWT em todas as requisições autenticadas via header Authorization
- [ ] **FR-004**: A aplicação detecta token expirado/inválido e redireciona para login
- [ ] **FR-005**: Ao login bem-sucedido, a aplicação chama endpoint /api/contas para criar/atualizar conta local
- [ ] **FR-006**: A aplicação verifica estado da conta após login e mostra tela apropriada
- [ ] **FR-007**: A aplicação implementa logout chamando /api/logout e removendo token local
- [ ] **FR-008**: A aplicação protege rotas que exigem autenticação via guards Angular

## Requisitos Funcionais - Moderação (FR-009 a FR-014)

- [ ] **FR-009**: A aplicação permite que administradores listem contas pendentes via /api/admin/moderacao/pendentes
- [ ] **FR-010**: A aplicação permite aprovação via POST /api/admin/moderacao/{id}/aprovar
- [ ] **FR-011**: A aplicação permite reprovação via POST /api/admin/moderacao/{id}/reprovar com motivo obrigatório
- [ ] **FR-012**: A aplicação mostra formulário de motivo ao reprovar e valida campo preenchido
- [ ] **FR-013**: A aplicação atualiza lista após aprovação/reprovação sem recarregar página
- [ ] **FR-014**: A aplicação restringe acesso a rotas de admin a usuários com papel ADMIN

## Requisitos Funcionais - Jogos (FR-015 a FR-024)

- [ ] **FR-015**: A aplicação permite listagem de jogos do usuário via GET /api/jogos
- [ ] **FR-016**: A aplicação permite cadastro via POST /api/jogos com modo INFORMADO ou GERADO
- [ ] **FR-017**: A aplicação implementa interface para selecionar dezenas manualmente (6-9 números)
- [ ] **FR-018**: A aplicação implementa interface para gerar dezenas automaticamente informando quantidade
- [ ] **FR-019**: A aplicação valida entrada localmente antes de enviar (feedback imediato)
- [ ] **FR-020**: A aplicação mostra o concurso vinculado após cadastro bem-sucedido
- [ ] **FR-021**: A aplicação permite edição via PUT /api/jogos/{id} apenas se jogo editável
- [ ] **FR-022**: A aplicação permite exclusão via DELETE /api/jogos/{id} apenas se jogo editável
- [ ] **FR-023**: A aplicação desabilita edição/exclusão para jogos não editáveis
- [ ] **FR-024**: A aplicação mostra confirmação antes de excluir jogo

## Requisitos Funcionais - Conferência (FR-025 a FR-030)

- [ ] **FR-025**: A aplicação permite conferência de jogo específico via GET /api/conferencia/{jogoId}
- [ ] **FR-026**: A aplicação permite conferência de todos os jogos via GET /api/conferencia
- [ ] **FR-027**: A aplicação mostra acertos, faixa e indicador de premiado para jogos conferidos
- [ ] **FR-028**: A aplicação mostra "Aguardando sorteio" para jogos não conferidos
- [ ] **FR-029**: A aplicação destaca visualmente jogos premiados (sena/quina/quadra)
- [ ] **FR-030**: A aplicação implementa atualização automática (polling ou refresh) para conferências

## Requisitos Funcionais - Concursos (FR-031 a FR-033)

- [ ] **FR-031**: A aplicação exibe o último concurso sincronizado via GET /api/concursos/latest
- [ ] **FR-032**: A aplicação exibe detalhes de concurso específico via GET /api/concursos/{numero}
- [ ] **FR-033**: A aplicação mostra dezenas sorteadas formatadas visualmente

## Requisitos Funcionais - Sincronização (FR-034 a FR-037)

- [ ] **FR-034**: A aplicação permite consulta de status via GET /api/admin/sync/status
- [ ] **FR-035**: A aplicação permite disparo manual via POST /api/admin/sync/run
- [ ] **FR-036**: A aplicação mostra indicador de loading durante sincronização
- [ ] **FR-037**: A aplicação atualiza status automaticamente após sincronização completar

## Requisitos Funcionais - UX/UI Geral (FR-038 a FR-044)

- [ ] **FR-038**: A aplicação é responsiva e funciona em desktop e mobile
- [ ] **FR-039**: A aplicação mostra indicadores de loading em operações assíncronas
- [ ] **FR-040**: A aplicação trata erros de API com mensagens amigáveis ao usuário
- [ ] **FR-041**: A aplicação implementa navegação com Angular Router
- [ ] **FR-042**: A aplicação tem layout consistente com header, navegação e conteúdo principal
- [ ] **FR-043**: A aplicação implementa feedback visual para ações (sucesso/erro)
- [ ] **FR-044**: A aplicação mantém estado de navegação (back button funciona corretamente)

## Requisitos Não-Funcionais (NFR-001 a NFR-010)

- [ ] **NFR-001**: A aplicação usa Angular LTS (versão estável mais recente)
- [ ] **NFR-002**: A aplicação usa TypeScript para type safety
- [ ] **NFR-003**: A aplicação segue Angular style guide oficial
- [ ] **NFR-004**: A aplicação tem build otimizado para produção (lazy loading, tree shaking)
- [ ] **NFR-005**: A aplicação implementa lazy loading de rotas
- [ ] **NFR-006**: A aplicação tem teste unitários para componentes e services
- [ ] **NFR-007**: A aplicação tem testes E2E para fluxos principais
- [ ] **NFR-008**: A aplicação é acessível (WCAG 2.1 AA onde aplicável)
- [ ] **NFR-009**: A aplicação tem performance > 90 no Lighthouse
- [ ] **NFR-010**: A aplicação suporta browsers modernos (Chrome, Firefox, Safari, Edge)

## Critérios de Sucesso (SC-001 a SC-010)

### Autenticação e Performance

- [ ] **SC-001**: Um usuário consegue completar o fluxo de login e chegar ao dashboard em menos de 10 segundos
- [ ] **SC-002**: Um usuário consegue cadastrar um jogo (informado ou gerado) em menos de 30 segundos
- [ ] **SC-003**: Um administrador consegue aprovar/reprovar uma conta em menos de 15 segundos
- [ ] **SC-004**: A aplicação carrega a página inicial em menos de 2 segundos (em conexão 3G)

### Integração e UX

- [ ] **SC-005**: 100% das requisições ao backend incluem token JWT válido quando autenticado
- [ ] **SC-006**: 100% dos erros de API são mostrados ao usuário com mensagem amigável
- [ ] **SC-007**: A aplicação funciona corretamente em mobile (viewport 375x667px)

### Testes e Performance

- [ ] **SC-008**: A aplicação tem 90%+ de cobertura de código em testes unitários
- [ ] **SC-009**: Os principais fluxos (login, cadastro, conferência) têm testes E2E passando
- [ ] **SC-010**: A aplicação tem score > 90 no Lighthouse (performance, accessibility, best practices)

## User Stories - Aceite

### US1 - Tela de Login e Auto-cadastro

- [ ] **US1.1**: Usuário não autenticado vê tela de login com opções de e-mail+senha e Google
- [ ] **US1.2**: Usuário novo criado em estado pendente após primeiro login
- [ ] **US1.3**: Usuário existente com conta ativa redirecionado para dashboard após login
- [ ] **US1.4**: Usuário com conta pendente vê tela informando que aguarda aprovação

### US2 - Dashboard do Usuário

- [ ] **US2.1**: Dashboard mostra resumo de jogos (total, aguardando, conferidos)
- [ ] **US2.2**: Usuário sem jogos vê mensagem orientando a cadastrar primeiro jogo
- [ ] **US2.3**: Links para cadastrar jogo e ver meus jogos funcionam

### US3 - Cadastro de Jogos

- [ ] **US3.1**: Cadastro manual com dezenas válidas funciona
- [ ] **US3.2**: Cadastro gerado com diferentes quantidades funciona
- [ ] **US3.3**: Validação local de dezenas inválidas funciona
- [ ] **US3.4**: Validação de quantidade fora de 6-9 funciona

### US4 - Lista e Gestão de Jogos

- [ ] **US4.1**: Lista mostra todos os jogos com dezenas, concurso e status
- [ ] **US4.2**: Edição de jogos não sorteados funciona
- [ ] **US4.3**: Exclusão de jogos não sorteados funciona
- [ ] **US4.4**: Edição/exclusão de jogos sorteados está desabilitada

### US5 - Conferência de Jogos

- [ ] **US5.1**: Jogos sorteados mostram acertos, faixa e premiado
- [ ] **US5.2**: Sena tem destaque visual especial
- [ ] **US5.3**: Quina/quadra têm destaque visual
- [ ] **US5.4**: Jogos não sorteados mostram "Aguardando sorteio"

### US6 - Dashboard do Administrador

- [ ] **US6.1**: Dashboard admin mostra resumo do sistema
- [ ] **US6.2**: Indicador visual para contas pendentes
- [ ] **US6.3**: Link para sincronização funciona

### US7 - Moderação de Contas

- [ ] **US7.1**: Lista de contas pendentes mostra e-mail, método e data
- [ ] **US7.2**: Aprovação de conta funciona
- [ ] **US7.3**: Reprovação com motivo funciona
- [ ] **US7.4**: Reprovação sem motivo é bloqueada

### US8 - Sincronização Manual

- [ ] **US8.1**: Status da última sincronização é exibido
- [ ] **US8.2**: Disparo manual de sincronização funciona
- [ ] **US8.3**: Indicador de loading é mostrado durante sincronização

### US9 - Perfil e Logout

- [ ] **US9.1**: Perfil mostra e-mail, papel e estado da conta
- [ ] **US9.2**: Logout funciona e redireciona para login

## Edge Cases

### Backend Indisponível

- [ ] Aplicação mostra mensagem amigável quando backend está indisponível
- [ ] Usuário é orientado a tentar mais tarde
- [ ] Aplicação não crasha

### Firebase Indisponível

- [ ] Tela de login mostra mensagem de erro temporário
- [ ] Usuário pode tentar novamente
- [ ] Aplicação não crasha

### Sessão Expirada

- [ ] Token expirado é detectado
- [ ] Usuário é redirecionado para login
- [ ] Mensagem de "sessão expirada" é mostrada

### Conta Reprovada

- [ ] Após login, usuário vê tela informando acesso negado
- [ ] Mensagem é clara e respeitosa
- [ ] Logout é permitido

### Sem Concursos Sincronizados

- [ ] Tela de cadastro mostra mensagem de "serviço indisponível temporariamente"
- [ ] Usuário é orientado a tentar mais tarde
- [ ] Funcionalidade é desabilitada

### Telas Pequenas (Mobile)

- [ ] Layout se adapta a viewports pequenos (375x667px)
- [ ] Todos os elementos são acessíveis
- [ ] Navegação funciona em mobile

### Navegação Não Autenticada

- [ ] Usuário não logado é sempre redirecionado para login
- [ ] Guards funcionam corretamente
- [ ] URL original é preservada após login (opcional)

## Segurança

- [ ] Token JWT é enviado em todas as requisições autenticadas
- [ ] Token é armazenado de forma segura (localStorage ou melhor)
- [ ] Guards protegem rotas sensíveis
- [ ] AdminGuard protege rotas administrativas
- [ ] XSS é mitigado (sanitização do Angular)
- [ ] Conteúdo não confiável nunca é renderizado como HTML
- [ ] Logout remove token e limpa estado
- [ ] Sessão expirada é detectada e tratada

## Performance

- [ ] Build de produção é otimizado (tree shaking, minification)
- [ ] Lazy loading está configurado para módulos de features
- [ ] Bundle inicial é pequeno (< 200KB gzipped)
- [ ] Imagens são otimizadas e lazy carregadas
- [ ] Lighthouse score > 90 em performance
- [ ] First Contentful Paint < 1.5s
- [ ] Largest Contentful Paint < 2.5s
- [ ] Time to Interactive < 3.5s
- [ ] Cumulative Layout Shift < 0.1

## Responsividade

- [ ] Layout funciona em desktop (> 1024px)
- [ ] Layout funciona em tablet (768px - 1024px)
- [ ] Layout funciona em mobile (< 768px)
- [ ] Layout funciona em mobile pequeno (375px)
- [ ] Navegação é touch-friendly
- [ ] Texto é legível em telas pequenas
- [ ] Botões têm tamanho adequado para touch

## Acessibilidade

- [ ] Contraste de texto é mínimo 4.5:1
- [ ] Navegação por teclado funciona
- [ ] Focus indicators são visíveis
- [ ] ARIA labels estão presentes em componentes interativos
- [ ] Imagens têm alt text apropriado
- [ ] Forms têm labels associadas
- [ ] Erros de validação são associados a campos
- [ ] Screen readers conseguem navegar (básico)

## Testes

- [ ] Testes unitários cobrem Services
- [ ] Testes unitários cobrem Components
- [ ] Testes unitários cobrem Guards
- [ ] Testes unitários cobrem Interceptors
- [ ] Testes E2E cobrem fluxo de login
- [ ] Testes E2E cobrem cadastro de jogos
- [ ] Testes E2E cobrem conferência
- [ ] Testes E2E cobrem moderação (admin)
- [ ] Cobertura de código > 90%
- [ ] Todos os testes passam consistentemente

## Browser Support

- [ ] Aplicação funciona no Chrome (últimas 2 versões)
- [ ] Aplicação funciona no Firefox (últimas 2 versões)
- [ ] Aplicação funciona no Safari (últimas 2 versões)
- [ ] Aplicação funciona no Edge (últimas 2 versões)
- [ ] Polyfills são usados quando necessário

## Internacionalização

- [ ] Interface está em português brasileiro
- [ ] Textos são claros e naturais
- [ ] Formatação de datas está em pt-BR
- [ ] Formatação de números está em pt-BR
- [ ] Estrutura está preparada para i18n futura (opcional)

## Deploy

- [ ] Build de produção funciona sem erros
- [ ] Deploy em ambiente de staging funciona
- [ ] Deploy em produção funciona
- [ ] Variáveis de ambiente estão configuradas corretamente
- [ ] CORS está configurado no backend
- [ ] Assets estáticos são servidos corretamente
- [ ] Roteamento funciona em SPA (refresh da página)
- [ ] CI/CD está configurado (opcional)