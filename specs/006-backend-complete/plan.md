# Plano de Implementação - Backend Completo

## Visão Geral

Este plano descreve a implementação do backend completo do Mega Sena Manager, consolidando as features 001-005 já implementadas em um serviço Spring Boot coeso.

## Arquitetura

### Stack Tecnológica

- **Framework**: Spring Boot 3.3.5
- **Linguagem**: Java 21
- **Banco de Dados**: MySQL 8 com Flyway
- **Segurança**: Spring Security + Firebase Authentication
- **Resilience**: Resilience4j (retry, timeout)
- **Testes**: Testcontainers + WireMock
- **Build**: Maven

### Pacotes Principais

```
com.megasena.sync
├── config              # Configurações (Security, Firebase, HttpClient)
├── admin              # Endpoints administrativos
├── concurso           # Sincronização e consulta de concursos
├── fonte              # Integração com API da Caixa
├── identidade         # Autenticação e gestão de usuários
├── moderacao          # Aprovação/reprovação de contas
├── jogo               # Cadastro e gestão de jogos
├── conferencia        # Conferência automática de jogos
└── sincronizacao      # Agendamento e execução de sync
```

## Fluxos Principais

### 1. Sincronização de Concursos

1. **Scheduler** executa via cron (configurável)
2. **SincronizacaoService** consulta API da Caixa via **CaixaSourceClient**
3. **ConcursoValidator** valida integridade dos dados (6 dezenas, 1-60)
4. **ConcursoMapper** converte resposta para entidade **Concurso**
5. **ConcursoRepository** persiste no MySQL (idempotente)
6. **SyncRun** registra execução (horário, resultado, concursos afetados)
7. Em falha, **Resilience4j** aplica retry com backoff

### 2. Autenticação de Usuário

1. Cliente obtém token JWT do Firebase
2. Requisição inclui `Authorization: Bearer <token>`
3. **IdentidadeTokenFilter** intercepta a requisição
4. **FirebaseVerificadorDeIdentidade** valida token com Firebase Admin SDK
5. Se válido, extrai `uid` e `email_verified`
6. **ResolvedorDeConta** busca ou cria conta em estado PENDENTE
7. **UsuarioAutenticado** é populado no SecurityContext
8. **SecurityConfig** autoriza acesso baseado em papel e estado

### 3. Moderação de Contas

1. Admin autenticado acessa `/api/admin/moderacao/pendentes`
2. **ModeracaoController** retorna contas em estado PENDENTE
3. Admin aprova via POST `/api/admin/moderacao/{id}/aprovar`
4. **ModeracaoService** transiciona estado para ATIVO
5. **DecisaoModeracao** registra auditoria (admin, conta, decisão, momento)
6. Ou admin reprova via POST `/api/admin/moderacao/{id}/reprovar` + motivo
7. Estado transiciona para REPROVADO, auditoria registrada com motivo

### 4. Cadastro de Jogos

1. Usuário autenticado (conta ATIVA) cadastra jogo
2. **JogoService** valida: quantidade 6-9, dezenas 1-60, sem repetição
3. Se modo gerado, **FonteAleatoriedade** gera dezenas aleatórias
4. **ConcursoService** determina próximo concurso em aberto
5. **Jogo** é criado com `concurso_numero` fixado imutavelmente
6. **JogoRepository** persiste vinculado ao usuário
7. **EventoJogo** registra auditoria

### 5. Conferência de Jogos

1. Usuário autenticado consulta `/api/conferencia/{jogoId}`
2. **ConferenciaService** verifica se concurso vinculado foi sorteado
3. Se sorteado, calcula interseção entre dezenas do jogo e sorteadas
4. Determina acertos (0-6), faixa (sena/quina/quadra/nenhuma), premiado (≥4)
5. Retorna **ConferenciaResponse** (computado sob demanda, determinístico)
6. Se primeira conferência, **EventoConferencia** registra auditoria

## Endpoints API

### Públicos (Leitura de Concursos)

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/concursos/latest` | Último concurso sincronizado |
| GET | `/api/concursos/{numero}` | Concurso por número |

### Autenticação (Usuários)

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/contas` | Auto-cadastro (via Firebase token) |
| GET | `/api/perfil` | Dados da própria conta |
| POST | `/api/logout` | Encerrar sessão |

### Moderação (Admin)

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/admin/moderacao/pendentes` | Listar contas pendentes |
| POST | `/api/admin/moderacao/{id}/aprovar` | Aprovar conta |
| POST | `/api/admin/moderacao/{id}/reprovar` | Reprovar conta (com motivo) |

### Jogos (Usuários)

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/jogos` | Listar jogos do usuário |
| POST | `/api/jogos` | Cadastrar jogo |
| PUT | `/api/jogos/{id}` | Editar jogo (se editável) |
| DELETE | `/api/jogos/{id}` | Excluir jogo (se editável) |

### Conferência (Usuários)

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/conferencia/{jogoId}` | Conferir jogo específico |
| GET | `/api/conferencia` | Conferir todos os jogos do usuário |

### Sincronização (Admin)

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/admin/sync/status` | Status da última sincronização |
| POST | `/api/admin/sync/run` | Disparar sincronização manual |

## Modelo de Dados

### Tabelas Principais

- **concurso**: dados de concursos da Mega Sena (global, somente leitura)
- **concurso_dezena**: dezenas sorteadas de cada concurso
- **usuario**: contas de usuários com estado e papel
- **evento_identidade**: auditoria de eventos de identidade
- **decisao_moderacao**: decisões de aprovação/reprovação
- **jogo**: apostas dos usuários (isoladas por usuário)
- **jogo_dezena**: dezenas de cada jogo
- **evento_jogo**: auditoria de operações de jogos
- **evento_conferencia**: auditoria de conferências realizadas
- **sync_run**: execuções de sincronização de concursos

## Segurança

### Autenticação

- Token JWT do Firebase em header `Authorization: Bearer <token>`
- Validação no servidor via Firebase Admin SDK
- Identidade derivada de `uid` do token, nunca de cliente

### Autorização

- **ADMIN**: acesso a endpoints de moderação e sincronização
- **USUARIO**: acesso a jogos e conferência próprios
- **Gate de estado**: contas PENDENTE/REPROVADO bloqueadas mesmo com token válido
- **Isolamento**: usuários só acessam próprios dados (derivação no servidor)

### Auditoria

- Todas as operações sensíveis geram registros de auditoria
- Registros incluem: quem, o quê, quando, e contexto adicional
- Auditoria preservada mesmo após exclusão de dados

## Resilience

### Chamadas Externas

- **API da Caixa**: retry com backoff, timeout, fallback para último dado válido
- **Firebase Authentication**: tratamento de indisponibilidade, degradação graciosa

### Idempotência

- Sincronização: reprocessar concurso não cria duplicatas
- Auditoria de conferência: uma vez por jogo×concurso
- Criação de conta: impede duplicatas por e-mail/identidade

## Testes

### Estratégia

- **Unitários**: lógica de negócio isolada com H2
- **Integração**: API completa com Testcontainers (MySQL) + WireMock (API Caixa dublê)
- **Resilience**: simulação de falhas externas

### Cobertura

- Controllers, Services, Repositories
- Fluxos de autenticação e autorização
- Casos de edge (falhas externas, decisões concorrentes)
- Validações de integridade de dados

## Deploy

### Configuração

Todas as configurações via variáveis de ambiente:

- `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DB`, `MYSQL_USER`, `MYSQL_PASSWORD`
- `MEGASENA_SOURCE_URL` (API da Caixa)
- `MEGASENA_SYNC_CRON` (agendamento)
- `MEGASENA_ADMIN_TOKEN` (token admin para sync manual)
- `FIREBASE_PROJECT_ID`, `FIREBASE_PRIVATE_KEY` (credenciais Firebase)

### Cloud-Native

- Imagem Docker otimizada (multi-stage build)
- Health checks (/actuator/health)
- Metrics (/actuator/prometheus)
- Configuração externa (Config Server ou env vars)
- Logging estruturado (JSON)