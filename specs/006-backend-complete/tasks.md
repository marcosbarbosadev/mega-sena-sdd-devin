# Tarefas de Implementação - Backend Completo

> Nota: Como esta feature consolida as features 001-005 já implementadas, as tarefas abaixo representam uma checklist de verificação do que foi implementado e pode ser usado como guia para review e validação.

## Bloco 1 - Infraestrutura e Configuração

### Tarefa 1.1 - Configurar Projeto Spring Boot
- [x] Criar projeto Spring Boot 3.3.5 com Java 21
- [x] Configurar dependências principais (Web, Data JPA, Security, Validation)
- [x] Configurar Maven wrapper
- [x] Configurar MySQL Connector e Flyway
- [x] Configurar Firebase Admin SDK
- [x] Configurar Resilience4j
- [x] Configurar testes (Testcontainers, WireMock, H2)

### Tarefa 1.2 - Configurar Banco de Dados
- [x] Configurar connection pooling (HikariCP)
- [x] Criar migrações Flyway iniciais
- [x] Configurar script de schema inicial
- [x] Configurar variáveis de ambiente para DB

### Tarefa 1.3 - Configurar Segurança
- [x] Configurar Spring Security
- [x] Criar IdentidadeTokenFilter para validação JWT
- [x] Configurar FirebaseVerificadorDeIdentidade
- [x] Configurar SecurityConfig com regras de autorização
- [x] Criar UsuarioAutenticado para SecurityContext
- [x] Configurar handlers de sucesso/falha de autenticação

### Tarefa 1.4 - Configurar Resilience
- [x] Configurar Retry via Resilience4j
- [x] Configurar Timeout para chamadas externas
- [x] Configurar Circuit Breaker (se aplicável)
- [x] Configurar HttpClient otimizado

---

## Bloco 2 - Feature 001: Sincronização de Concursos

### Tarefa 2.1 - Modelo de Dados de Concurso
- [x] Criar entidade Concurso
- [x] Criar entidade ConcursoDezena
- [x] Criar ConcursoRepository
- [x] Criar migração Flyway para tabelas de concurso
- [x] Adicionar índices (numero, etc.)

### Tarefa 2.2 - Integração com API da Caixa
- [x] Criar CaixaSourceClient (HTTP client)
- [x] Criar CaixaConcursoResponse (DTO)
- [x] Criar ConcursoMapper (converter resposta API → entity)
- [x] Criar ConcursoValidator (validar 6 dezenas, 1-60)
- [x] Configurar endpoint da Caixa
- [x] Configurar timeout e retry

### Tarefa 2.3 - Serviço de Sincronização
- [x] Criar SincronizacaoService
- [x] Implementar lógica de detecção de novos concursos
- [x] Implementar carga histórica retomável
- [x] Implementar idempotência (INSERT IGNORE/upsert)
- [x] Implementar preenchimento de lacunas

### Tarefa 2.4 - Agendamento e Status
- [x] Criar SincronizacaoScheduler (cron)
- [x] Criar entidade SyncRun
- [x] Criar SyncRunRepository
- [x] Implementar registro de execuções (horário, resultado, concursos)
- [x] Configurar cron via variável de ambiente

### Tarefa 2.5 - Endpoints Admin de Sync
- [x] Criar AdminSyncController
- [x] Implementar GET /api/admin/sync/status
- [x] Implementar POST /api/admin/sync/run
- [x] Criar SyncRunResponse (DTO)
- [x] Proteger endpoints com autenticação admin

### Tarefa 2.6 - Endpoints Públicos de Concurso
- [x] Criar ConcursoController
- [x] Implementar GET /api/concursos/latest
- [x] Implementar GET /api/concursos/{numero}
- [x] Criar ConcursoResponse (DTO)
- [x] Tornar endpoints públicos (sem autenticação)

### Tarefa 2.7 - Testes de Sincronização
- [x] Criar testes unitários de SincronizacaoService
- [x] Criar testes de integração com WireMock (API Caixa dublada)
- [x] Criar testes de idempotência
- [x] Criar testes de validação de concursos
- [x] Criar testes de agendamento

---

## Bloco 3 - Feature 002: Identidade & Autenticação

### Tarefa 3.1 - Modelo de Dados de Usuário
- [x] Criar entidade Usuario
- [x] Criar enum EstadoConta (PENDENTE, ATIVO, REPROVADO)
- [x] Criar enum Papel (USUARIO, ADMIN)
- [x] Criar enum MetodoLogin (EMAIL_SENHA, GOOGLE)
- [x] Criar UsuarioRepository
- [x] Criar migração Flyway para tabela usuario
- [x] Adicionar índices (firebase_uid, email, estado)

### Tarefa 3.2 - Auditoria de Identidade
- [x] Criar entidade EventoIdentidade
- [x] Criar enum TipoEvento (CADASTRO, AUTH_SUCCESS, AUTH_FAILED, LOGOUT)
- [x] Criar EventoIdentidadeRepository
- [x] Criar migração Flyway para tabela evento_identidade
- [x] Criar AuditoriaIdentidadeService

### Tarefa 3.3 - Verificação de Identidade Firebase
- [x] Criar interface VerificadorDeIdentidade
- [x] Criar FirebaseVerificadorDeIdentidade (implementação)
- [x] Implementar validação de token JWT
- [x] Implementar extração de uid e email_verified
- [x] Criar IdentidadeVerificada (DTO)
- [x] Configurar Firebase Admin SDK

### Tarefa 3.4 - Resolução de Conta
- [x] Criar interface ResolvedorDeConta
- [x] Criar ResolvedorDeContaImpl (implementação)
- [x] Implementar busca ou criação de conta
- [x] Implementar auto-vinculação de Google à conta existente
- [x] Implementar recusa de e-mail não verificado
- [x] Implementar bloqueio de duplicatas

### Tarefa 3.5 - Serviço de Provisionamento
- [x] Criar ProvisionamentoService
- [x] Implementar criação de admin inicial
- [x] Implementar ativação de contas fora de banda

### Tarefa 3.6 - Endpoints de Autenticação
- [x] Criar ContaController
- [x] Implementar POST /api/contas (auto-cadastro)
- [x] Implementar GET /api/perfil
- [x] Criar PerfilController
- [x] Implementar POST /api/logout
- [x] Criar ContaResponse (DTO)
- [x] Proteger endpoints com autenticação

### Tarefa 3.7 - Gate de Estado de Conta
- [x] Modificar IdentidadeTokenFilter para verificar estado
- [x] Bloquear contas PENDENTE/REPROVADO
- [x] Retornar 403 com estado da conta
- [x] Registrar tentativas de acesso na auditoria

### Tarefa 3.8 - Testes de Identidade
- [x] Criar testes unitários de ResolvedorDeConta
- [x] Criar testes de FirebaseVerificadorDeIdentidade (com dublê)
- [x] Criar testes de integração de autenticação
- [x] Criar testes de gate de estado
- [x] Criar testes de auditoria de identidade

---

## Bloco 4 - Feature 003: Aprovação de Contas

### Tarefa 4.1 - Modelo de Dados de Moderação
- [x] Criar entidade DecisaoModeracao
- [x] Criar enum Decisao (APROVACAO, REPROVACAO)
- [x] Criar DecisaoModeracaoRepository
- [x] Criar migração Flyway para tabela decisao_moderacao
- [x] Adicionar índices (conta_id, admin_id)

### Tarefa 4.2 - Serviço de Moderação
- [x] Criar ModeracaoService
- [x] Implementar listagem de contas pendentes
- [x] Implementar aprovação (transição PENDENTE → ATIVO)
- [x] Implementar reprovação (transição PENDENTE → REPROVADO)
- [x] Implementar exigência de motivo na reprovação
- [x] Implementar bloqueio de transições inválidas
- [x] Implementar prevenção de decisões concorrentes

### Tarefa 4.3 - Endpoints de Moderação
- [x] Criar ModeracaoController
- [x] Implementar GET /api/admin/moderacao/pendentes
- [x] Implementar POST /api/admin/moderacao/{id}/aprovar
- [x] Implementar POST /api/admin/moderacao/{id}/reprovar
- [x] Criar ContaPendenteResponse (DTO)
- [x] Criar ReprovacaoRequest (DTO)
- [x] Proteger endpoints com autenticação admin

### Tarefa 4.4 - Auditoria de Moderação
- [x] Registrar decisões em DecisaoModeracao
- [x] Incluir motivo (obrigatório na reprovação)
- [x] Incluir admin responsável, conta afetada, momento
- [x] Registrar em EventoIdentidade

### Tarefa 4.5 - Testes de Moderação
- [x] Criar testes unitários de ModeracaoService
- [x] Criar testes de integração de moderação
- [x] Criar testes de decisões concorrentes
- [x] Criar testes de autorização (apenas admin)
- [x] Criar testes de auditoria de decisões

---

## Bloco 5 - Feature 004: Cadastro de Jogos

### Tarefa 5.1 - Modelo de Dados de Jogo
- [x] Criar entidade Jogo
- [x] Criar entidade JogoDezena
- [x] Criar enum TipoSelecao (INFORMADO, GERADO)
- [x] Criar enum TipoEventoJogo (CADASTRO, EDICAO, EXCLUSAO)
- [x] Criar JogoRepository
- [x] Criar migração Flyway para tabelas jogo e jogo_dezena
- [x] Adicionar índices (usuario_id, concurso_numero)

### Tarefa 5.2 - Auditoria de Jogos
- [x] Criar entidade EventoJogo
- [x] Criar EventoJogoRepository
- [x] Criar migração Flyway para tabela evento_jogo
- [x] Criar AuditoriaJogoService

### Tarefa 5.3 - Geração de Dezenas
- [x] Criar interface FonteAleatoriedade
- [x] Criar FonteAleatoriedadeImpl (implementação)
- [x] Implementar geração de N dezenas distintas (1-60)
- [x] Implementar aleatoriedade uniforme

### Tarefa 5.4 - Serviço de Jogos
- [x] Criar JogoService
- [x] Implementar validação de apostas (quantidade 6-9, dezenas 1-60, sem repetição)
- [x] Implementar cadastro manual (todas as dezenas informadas)
- [x] Implementar cadastro gerado (gerar dezenas automaticamente)
- [x] Implementar bloqueio de cadastro parcial
- [x] Implementar determinação do próximo concurso em aberto
- [x] Implementar vinculação imutável ao concurso
- [x] Implementar edição (enquanto não sorteado)
- [x] Implementar exclusão (enquanto não sorteado)
- [x] Implementar bloqueio pós-sorteio (somente leitura)
- [x] Implementar listagem (apenas jogos do usuário)

### Tarefa 5.5 - Endpoints de Jogos
- [x] Criar JogoController
- [x] Implementar GET /api/jogos (listar)
- [x] Implementar POST /api/jogos (criar)
- [x] Implementar PUT /api/jogos/{id} (editar)
- [x] Implementar DELETE /api/jogos/{id} (excluir)
- [x] Criar JogoRequest (DTO)
- [x] Criar JogoResponse (DTO)
- [x] Proteger endpoints com autenticação e estado ATIVO
- [x] Implementar isolamento (apenas dono)

### Tarefa 5.6 - Tratamento de Erros
- [x] Criar JogoValidacaoException
- [x] Criar JogoNaoEditavelException
- [x] Criar ContaNaoEncontradaException
- [x] Criar GlobalExceptionHandler para tratar exceções

### Tarefa 5.7 - Testes de Jogos
- [x] Criar testes unitários de JogoService
- [x] Criar testes de validação de apostas
- [x] Criar testes de geração de dezenas
- [x] Criar testes de integração de jogos
- [x] Criar testes de edição/exclusão (pré/pós sorteio)
- [x] Criar testes de isolamento multiusuário
- [x] Criar testes de auditoria de jogos

---

## Bloco 6 - Feature 005: Conferência Automática

### Tarefa 6.1 - Modelo de Dados de Conferência
- [x] Criar enum Faixa (SENA, QUINA, QUADRA, NENHUMA)
- [x] Criar entidade EventoConferencia
- [x] Criar EventoConferenciaRepository
- [x] Criar migração Flyway para tabela evento_conferencia
- [x] Adicionar índices (jogo_id, usuario_id, momento)
- [x] Adicionar índice único (usuario_id, jogo_id) para idempotência

### Tarefa 6.2 - Serviço de Conferência
- [x] Criar ConferenciaService
- [x] Implementar cálculo de acertos (interseção dezenas)
- [x] Implementar classificação de faixa (sena/quina/quadra)
- [x] Implementar indicador de premiado (≥4 acertos)
- [x] Implementar verificação se concurso foi sorteado
- [x] Implementar "aguardando sorteio" para concursos não sorteados
- [x] Implementar computação sob demanda (não materializada)
- [x] Implementar idempotência de auditoria (uma vez por jogo×concurso)
- [x] Implementar conferência individual e conjunto

### Tarefa 6.3 - Endpoints de Conferência
- [x] Criar ConferenciaController
- [x] Implementar GET /api/conferencia/{jogoId}
- [x] Implementar GET /api/conferencia
- [x] Criar ConferenciaResponse (DTO)
- [x] Proteger endpoints com autenticação e estado ATIVO
- [x] Implementar isolamento (apenas jogos do usuário)

### Tarefa 6.4 - Testes de Conferência
- [x] Criar testes unitários de ConferenciaService
- [x] Criar testes de cálculo de acertos e faixas
- [x] Criar testes de conferência pré/pós sorteio
- [x] Criar testes de determinismo (mesma entrada = mesma saída)
- [x] Criar testes de integração de conferência
- [x] Criar testes de isolamento multiusuário
- [x] Criar testes de auditoria de conferência

---

## Bloco 7 - Configurações e Finalizações

### Tarefa 7.1 - Configuração de Aplicação
- [x] Criar application.yml com configurações padrão
- [x] Configurar todas as propriedades externizáveis
- [x] Documentar variáveis de ambiente
- [x] Configurar logging (nível, formato)
- [x] Configurar actuator endpoints

### Tarefa 7.2 - Documentação
- [x] Criar README.md do backend
- [x] Documentar pré-requisitos
- [x] Documentar configuração
- [x] Documentar endpoints
- [x] Documentar build e execução
- [x] Documentar variáveis de ambiente

### Tarefa 7.3 - Testes Finais
- [x] Executar suite completa de testes unitários
- [x] Executar suite completa de testes de integração
- [x] Verificar cobertura de código
- [x] Testar cenários de edge cases
- [x] Testar failure scenarios (serviços externos indisponíveis)

### Tarefa 7.4 - Build e Deploy
- [x] Criar Dockerfile (multi-stage build)
- [x] Testar build de Docker image
- [x] Configurar health checks
- [x] Testar execução em container
- [x] Documentar processo de deploy

### Tarefa 7.5 - Limpeza e Refinamento
- [x] Remover código comentado/não usado
- [x] Padronizar formatação de código
- [x] Verificar nomes de classes/métodos (clareza)
- [x] Remover dependências não utilizadas
- [x] Otimizar imports
- [x] Adicionar Javadoc onde necessário

---

## Critérios de Aceite do Backend Completo

### Funcionalidade

- [x] Sincronização de concursos funciona (agendada e manual)
- [x] Autenticação via Firebase funciona (e-mail+senha e Google)
- [x] Moderação de contas funciona (listar, aprovar, reprovar)
- [x] Cadastro de jogos funciona (manual e gerado)
- [x] Conferência de jogos funciona (individual e conjunto)
- [x] Isolamento multiusuário está garantido
- [x] Gate de estado de conta funciona
- [x] Auditoria é registrada para todas as operações sensíveis

### Qualidade

- [x] Todos os testes passam (unitários e integração)
- [x] Cobertura de código adequada (>80%)
- [x] Sem vulnerabilidades de segurança conhecidas
- [x] Código segue boas práticas de Java/Spring
- [x] Logging adequado para debug e monitoramento

### Performance

- [x] Tempo de resposta de endpoints < 200ms (p95)
- [x] Queries de banco eficientes (índices adequados)
- [x] Sem memory leaks
- [x] Connection pooling configurado corretamente

### Resilience

- [x] Sistema degrada graciosamente quando API Caixa está indisponível
- [x] Sistema degrada graciosamente quando Firebase está indisponível
- [x] Retry funciona corretamente em falhas transitórias
- [x] Idempotência funciona (sincronização, auditoria)

### Documentação

- [x] README.md completo e atualizado
- [x] Variáveis de ambiente documentadas
- [x] Endpoints documentados
- [x] Código com comentários onde necessário