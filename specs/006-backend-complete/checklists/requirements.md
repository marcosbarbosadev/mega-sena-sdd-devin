# Checklist de Requisitos - Backend Completo

## Requisitos Funcionais - Sincronização de Concursos (FR-001 a FR-012)

- [ ] **FR-001**: O sistema obtém da API oficial da Caixa os dados de cada concurso (número, data, dezenas, valor do prêmio)
- [ ] **FR-002**: O sistema armazena localmente os dados de cada concurso em MySQL de forma persistente
- [ ] **FR-003**: O sistema executa sincronização agendada via cron (configurável, default diário)
- [ ] **FR-004**: O sistema detecta e importa novos concursos assim que disponíveis na fonte
- [ ] **FR-005**: O sistema suporta carga inicial do histórico completo de forma retomável
- [ ] **FR-006**: A sincronização é idempotente — reprocessar o mesmo concurso não cria duplicatas
- [ ] **FR-007**: O sistema continua servindo dados válidos quando a fonte está indisponível
- [ ] **FR-008**: O sistema registra cada execução de sincronização com horário, resultado e concursos afetados
- [ ] **FR-009**: O sistema tenta novamente automaticamente após falhas transitórias (retry)
- [ ] **FR-010**: O sistema valida integridade dos dados (6 dezenas distintas entre 1-60) antes de persistir
- [ ] **FR-011**: O perfil admin pode consultar status e disparar sincronização manual
- [ ] **FR-012**: O sistema preenche lacunas de concursos para garantir sequência contínua

## Requisitos Funcionais - Identidade & Autenticação (FR-013 a FR-027)

- [ ] **FR-013**: O sistema permite auto-cadastro via Firebase Authentication (e-mail+senha ou Google)
- [ ] **FR-014**: O sistema delega autenticação e guarda de credenciais ao Firebase, NÃO armazenando senhas
- [ ] **FR-015**: Toda conta recém-criada nasce em estado **pendente**, sem acesso a recursos de usuário
- [ ] **FR-016**: O sistema permite autenticação via token JWT do Firebase
- [ ] **FR-017**: Em toda requisição autenticada, o sistema valida o token no servidor e deriva o identificador do usuário
- [ ] **FR-018**: O sistema concede acesso apenas a contas em estado **ativo**
- [ ] **FR-019**: O sistema registra para cada conta: identificador Firebase, e-mail, papel (usuário/admin) e estado
- [ ] **FR-020**: O sistema impede criação de contas duplicadas para o mesmo e-mail/identidade
- [ ] **FR-021**: O sistema distingue papel **administrador** de **usuário comum**
- [ ] **FR-022**: O sistema permite que um usuário encerre sua sessão
- [ ] **FR-023**: O sistema registra trilha de auditoria dos eventos de identidade (cadastro, autenticação, logout)
- [ ] **FR-024**: O sistema recusa autenticação inválida sem revelar se o e-mail corresponde a uma conta
- [ ] **FR-025**: O sistema degrada graciosamente quando o Firebase está indisponível
- [ ] **FR-026**: Quando identidade Google chegar com e-mail já vinculado, o sistema vincula à mesma conta se o Firebase indicar e-mail verificado
- [ ] **FR-027**: O sistema recusa criação de conta com e-mail não verificado pelo Firebase

## Requisitos Funcionais - Aprovação de Contas (FR-028 a FR-037)

- [ ] **FR-028**: O sistema permite que um **administrador** liste contas em estado **pendente**
- [ ] **FR-029**: O sistema restringe todas as ações de moderação ao papel **administrador**
- [ ] **FR-030**: O sistema permite que um administrador **aprove** uma conta pendente, transicionando-a para **ativo**
- [ ] **FR-031**: O sistema permite que um administrador **reprove** uma conta pendente, transicionando-a para **reprovado**
- [ ] **FR-032**: O sistema exige **motivo** ao reprovar; aprovação NÃO exige motivo
- [ ] **FR-033**: O sistema permite transições apenas a partir de estado **pendente**; ativo/reprovado são terminais
- [ ] **FR-034**: Após aprovação, a conta passa a ter acesso aos recursos de usuário
- [ ] **FR-035**: O sistema registra trilha de auditoria de cada decisão (admin, conta, decisão, momento, motivo)
- [ ] **FR-036**: O sistema garante que decisões concorrentes não produzam estados inconsistentes
- [ ] **FR-037**: O sistema deriva a identidade do administrador da sessão verificada no servidor

## Requisitos Funcionais - Cadastro de Jogos (FR-038 a FR-048)

- [ ] **FR-038**: O sistema permite que usuário com conta **ativa** cadastre jogo escolhendo quantidade de dezenas (6–9)
- [ ] **FR-039**: O sistema permite cadastro informando todas as dezenas OU pedindo geração automática
- [ ] **FR-040**: O sistema aceita todas as dezenas OU nenhuma — nunca subconjunto parcial
- [ ] **FR-041**: O sistema valida toda aposta: quantidade ∈ {6,7,8,9}, dezenas ∈ [1,60], sem repetição
- [ ] **FR-042**: No cadastro, o sistema vincula o jogo ao **próximo concurso em aberto**, gravando o número imutavelmente
- [ ] **FR-043**: Todo jogo pertence ao usuário autenticado; operações são restritas ao dono
- [ ] **FR-044**: O sistema permite que o usuário liste os próprios jogos
- [ ] **FR-045**: O sistema permite editar/excluir jogo enquanto o concurso vinculado não foi sorteado
- [ ] **FR-046**: Após sorteio, o jogo é **somente leitura** (edição/exclusão bloqueadas)
- [ ] **FR-047**: O acesso a operações de jogos exige conta **ativa**
- [ ] **FR-048**: O sistema registra trilha de auditoria das operações de jogo

## Requisitos Funcionais - Conferência Automática (FR-049 a FR-059)

- [ ] **FR-049**: Quando existir resultado do concurso vinculado, o sistema confere o jogo comparando dezenas
- [ ] **FR-050**: A conferência informa a **quantidade de acertos** (0–6)
- [ ] **FR-051**: A conferência classifica a **faixa**: sena (6), quina (5), quadra (4); abaixo de 4, nenhuma
- [ ] **FR-052**: A conferência indica se o jogo foi **premiado** (≥ 4 acertos), sem valores monetários
- [ ] **FR-053**: A conferência só está disponível quando o concurso vinculado foi sorteado
- [ ] **FR-054**: A conferência é determinística e idempotente
- [ ] **FR-055**: A conferência é isolada por usuário — usuário vê apenas próprios jogos
- [ ] **FR-056**: O acesso à conferência exige conta **ativa**
- [ ] **FR-057**: O usuário pode conferir jogo específico E consultar conferência do conjunto
- [ ] **FR-058**: A conferência é somente leitura sobre jogo e concurso
- [ ] **FR-059**: O resultado é computado sob demanda e gera trilha de auditoria (uma vez por jogo×concurso)

## Requisitos Não-Funcionais (NFR-001 a NFR-010)

- [ ] **NFR-001**: O sistema usa Spring Boot 3.3.5 com Java 21
- [ ] **NFR-002**: O sistema persiste dados em MySQL 8 com migrações versionadas via Flyway
- [ ] **NFR-003**: O sistema implementa segurança via Spring Security com validação de tokens JWT do Firebase
- [ ] **NFR-004**: O sistema garante isolamento multiusuário completo entre dados de diferentes usuários
- [ ] **NFR-005**: O sistema implementa retry/resilience via Resilience4j para chamadas externas
- [ ] **NFR-006**: O sistema tem testes de integração usando Testcontainers e WireMock
- [ ] **NFR-007**: O sistema é cloud-native e configurável via variáveis de ambiente
- [ ] **NFR-008**: O sistema registra trilha de auditoria para todas as operações sensíveis
- [ ] **NFR-009**: O sistema trata dados de concursos como globais e somente leitura para usuários
- [ ] **NFR-010**: O sistema degrada graciosamente em caso de indisponibilidade de serviços externos

## Critérios de Sucesso (SC-001 a SC-015)

### Sincronização

- [ ] **SC-001**: Um novo concurso oficial fica disponível em até 24 horas após divulgação
- [ ] **SC-002**: Após carga inicial, 100% dos concursos sorteados estão disponíveis localmente
- [ ] **SC-014**: Durante indisponibilidade da fonte, 100% das consultas a dados sincronizados funcionam

### Identidade & Autenticação

- [ ] **SC-003**: 100% das requisições autenticadas resolvem para exatamente uma conta (isolamento garantido)
- [ ] **SC-004**: 100% das tentativas de acesso por contas pendentes/reprovadas são bloqueadas
- [ ] **SC-005**: A aplicação não armazena senhas — apenas identificadores do Firebase

### Moderação

- [ ] **SC-006**: Um admin consegue revisar e decidir (aprovar/reprovar) conta em menos de 1 minuto
- [ ] **SC-007**: 100% das decisões de moderação geram registro de auditoria rastreável

### Jogos

- [ ] **SC-008**: Um usuário consegue cadastrar um jogo em menos de 1 minuto
- [ ] **SC-009**: 100% dos jogos pertencem e são visíveis apenas ao dono
- [ ] **SC-010**: 100% das tentativas de editar/excluir jogo pós-sorteio são bloqueadas

### Conferência

- [ ] **SC-011**: Para jogo com concurso sorteado, acertos correspondem exatamente à interseção dezenas
- [ ] **SC-012**: A conferência é idempotente — repetição retorna sempre o mesmo resultado
- [ ] **SC-013**: 100% das conferências respeitam isolamento — usuário nunca vê jogo de outro

### Geral

- [ ] **SC-015**: 100% das operações sensíveis geram trilha de auditoria completa

## Edge Cases

### Sincronização

- [ ] Fonte oficial indisponível ou lenta (timeout) — sistema degrada graciosamente
- [ ] Resposta malformada ou incompleta da fonte — dados inválidos rejeitados
- [ ] Concurso ainda não sorteado / data futura — nenhum resultado criado
- [ ] Lacuna de concursos — preenchida para manter sequência contínua
- [ ] Sincronização agendada e manual simultâneas — evita processamento duplicado
- [ ] Republicação/correção de concurso pela fonte — tratada como nova ingestão
- [ ] Falha transitória durante carga histórica — carga é retomável

### Identidade & Autenticação

- [ ] Cadastro Google sem e-mail verificado — recusado com mensagem orientadora
- [ ] Mesma pessoa, dois métodos (e-mail+senha e Google) — vínculo automático se e-mail verificado
- [ ] Aprovação ocorre durante sessão — usuário passa a ter acesso sem recadastrar
- [ ] Conta reprovada tenta recadastrar — bloqueado por unicidade de identidade
- [ ] Indisponibilidade do provedor de identidade — degrada com mensagem clara
- [ ] Primeiro administrador — provisionado fora de banda

### Moderação

- [ ] Decisão concorrente — apenas a primeira decisão vale; segunda recusada
- [ ] Aprovação durante sessão pendente — usuário ganha acesso sem recadastrar
- [ ] Reprovar e recadastrar — bloqueado por unicidade de identidade
- [ ] Conta pendente excluída/ausente — operação recusada com mensagem clara
- [ ] Nenhum administrador disponível — contas nunca aprovadas (resolvido por admin inicial)

### Jogos

- [ ] Sem concurso em aberto determinável — cadastro recusado com mensagem clara
- [ ] Sorteio entre cadastro e edição — bloqueado (vira somente leitura)
- [ ] Conta não-ativa — bloqueado pelo gate (403)
- [ ] Acesso cruzado — negado (isolamento, identidade derivada do servidor)
- [ ] Geração impossível (quantidade fora de 6–9) — recusado
- [ ] Cadastro parcial (algumas dezenas) — recusado

### Conferência

- [ ] Jogo com 7–9 dezenas — acertos contam quantas das 6 sorteadas estão no jogo (máximo 6)
- [ ] Concurso ainda não sorteado — conferência mostra "aguardando sorteio"
- [ ] Resultado corrigido na fonte — conferência reflete sempre o dado vigente
- [ ] Conta não-ativa — bloqueado pelo gate (403)
- [ ] Acesso cruzado — negado (isolamento, identidade derivada do servidor)

## Segurança

- [ ] Autenticação via token JWT validado no servidor
- [ ] Identidade derivada da sessão, nunca enviada pelo cliente
- [ ] Autorização por papel (ADMIN vs USUARIO)
- [ ] Gate de estado de conta (apenas ATIVO tem acesso)
- [ ] Isolamento multiusuário em todas as operações
- [ ] Auditoria de todas as operações sensíveis
- [ ] Rate limiting (se aplicável)
- [ ] HTTPS obrigatório em produção
- [ ] CORS configurado para frontend
- [ ] Senhas NÃO armazenadas (apenas identificadores Firebase)

## Performance

- [ ] Tempo de resposta de endpoints < 200ms (p95)
- [ ] Queries de banco eficientes (índices adequados)
- [ ] Connection pooling configurado corretamente
- [ ] Sem memory leaks
- [ ] Cache de validação de token (short-lived)
- [ ] Paginação em listagens longas (se necessário)

## Testes

- [ ] Testes unitários de todos os Services
- [ ] Testes unitários de todos os Repositories
- [ ] Testes de integração com Testcontainers (MySQL)
- [ ] Testes de integração com WireMock (API Caixa dublada)
- [ ] Testes de autenticação e autorização
- [ ] Testes de isolamento multiusuário
- [ ] Testes de edge cases e failure scenarios
- [ ] Cobertura de código > 80%
- [ ] Todos os testes passando consistentemente

## Documentação

- [ ] README.md completo e atualizado
- [ ] Variáveis de ambiente documentadas
- [ ] Endpoints documentados
- [ ] Diagrama de arquitetura (se aplicável)
- [ ] Guia de troubleshooting
- [ ] Comentários em código onde necessário
- [ ] Javadoc em classes públicas

## Deploy

- [ ] Dockerfile otimizado (multi-stage build)
- [ ] Health checks configurados
- [ ] Graceful shutdown configurado
- [ ] Logging configurado para produção
- [ ] Configuração externalizada (env vars)
- [ ] Testado em ambiente de staging