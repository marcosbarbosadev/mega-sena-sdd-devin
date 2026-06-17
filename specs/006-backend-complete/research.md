# Pesquisa e Decisões Técnicas - Backend Completo

## Stack Tecnológica Escolhida

### Spring Boot 3.3.5

**Justificativa**:
- Framework maduro e amplamente adotado no ecossistema Java
- Auto-configuração reduz boilerplate
- Ecossistema rico de starters (Data JPA, Security, Validation)
- Suporte nativo a Java 21 (records, pattern matching)
- Compatível com design cloud-native

**Alternativas consideradas**:
- Quarkus: tempo de startup menor, mas ecossistema menor
- Micronaut: bom desempenho, mas curva de aprendizado maior
- **Decisão**: Spring Boot pelo equilíbrio entre maturidade e produtividade

---

### Java 21

**Justificativa**:
- LTS (Long Term Support)
- Records para DTOs imutáveis
- Pattern matching para código mais limpo
- Virtual threads (preview) para concorrência
- Melhorias de performance em relação a Java 17/11

**Alternativas consideradas**:
- Java 17: LTS anterior, mas sem recursos mais recentes
- Java 11: LTS mais antigo, mas amplamente usado
- **Decisão**: Java 21 para aproveitar recursos modernos

---

### MySQL 8 com Flyway

**Justificativa**:
- MySQL 8: JSON suportado nativamente, melhor performance
- Flyway: migrações versionadas, integração nativa com Spring Boot
- Relacional adequado para o modelo de dados (não requer NoSQL)
- Amplamente suportado em cloud (AWS RDS, Google Cloud SQL)

**Alternativas consideradas**:
- PostgreSQL: recursos avançados, mas MySQL suficiente
- MongoDB: não necessário, modelo relacional é adequado
- H2: apenas para testes, MySQL para produção
- **Decisão**: MySQL 8 pela maturidade e suporte cloud

---

### Firebase Authentication

**Justificativa**:
- Provedor de identidade gerenciado (Princípio VII)
- Suporte nativo a e-mail+senha e Google
- SDKs robustos para Java (Admin SDK)
- Verificação de e-mail built-in
- Escala horizontal sem preocupação com sessões
- Custo baixo/atrativo para uso pessoal/restrito

**Alternativas consideradas**:
- AWS Cognito: similar, mas curva de aprendizado maior
- Auth0: mais caro, recursos excessivos para uso pessoal
- Keycloak: self-hosted, mas complexidade operacional
- Implementação própria: viola Princípio VII
- **Decisão**: Firebase pelo equilíbrio entre funcionalidade e simplicidade

---

### Resilience4j

**Justificativa**:
- Integração nativa com Spring Boot 3
- Retry com backoff exponencial
- Circuit breaker (para future enhancements)
- Timeout configurável
- Leve e sem dependências pesadas

**Alternativas consideradas**:
- Spring Retry: mais simples, mas menos recursos
- Hystrix: em manutenção, substituído por Resilience4j
- Implementação própria: mais trabalho, menos robusto
- **Decisão**: Resilience4j pela integração Spring Boot e recursos

---

### Testcontainers + WireMock

**Justificativa**:
- Testcontainers: MySQL real em containers, não mocks
- WireMock: API da Caixa dublada de forma realista
- Testes de integração confiáveis
-接近produção environment
- Fácil configuração com Spring Boot

**Alternativas consideradas**:
- H2 para todos os testes: menos realista para integração
- Mocks manuais: mais trabalho, menos confiável
- Testes apenas unitários: não cobrem integração
- **Decisão**: Testcontainers + WireMock para realismo

---

## Decisões de Arquitetura

### Autenticação Stateless via JWT

**Decisão**: Token JWT do Firebase, validado no servidor a cada requisição.

**Justificativa**:
- Stateless: escala horizontal sem shared state
- Firebase gerencia lifecycle do token
- Validação no servidor evita trust no cliente
- Idade do token configurável

**Trade-offs**:
- Revalidação a cada request adds overhead
- Mitigação: caching de validação por período curto

---

### Isolamento Multiusuário por Derivação no Servidor

**Decisão**: Identidade do usuário sempre derivada da sessão verificada no servidor, nunca de IDs enviados pelo cliente.

**Justificativa**:
- Garante isolamento inquebrável (Princípio I)
- Previne ataques de IDOR (Insecure Direct Object Reference)
- Auditoria confiável

**Implementação**:
- Filter intercepta requisição
- Firebase token validado
- `UsuarioAutenticado` populado no SecurityContext
- Controllers extraem identidade do SecurityContext

---

### Idempotência de Sincronização

**Decisão**: Sincronização é idempotente — reprocessar mesmo concurso não cria duplicatas.

**Justificativa**:
- Retries seguros
- Recuperação de falhas transparente
- Não precisa tracking complexo de "já processado"

**Implementação**:
- Concurso.numero é UNIQUE
- INSERT IGNORE ou upsert
- Validação prévia

---

### Computação Sob Demanda para Conferência

**Decisão**: Resultado de conferência é computado sob demanda, não materializado.

**Justificativa**:
- Determinístico: mesma entrada, mesma saída
- Sem risco de inconsistência
- Auditoria registra consultas, não resultado
- Se concurso for corrigido, conferência reflete dado vigente

**Trade-offs**:
- Computação a cada consulta
- Mitigação: performance aceitável para volume baixo

---

### Estado de Conta como Gate de Segurança

**Decisão**: Conta tem três estados (PENDENTE/ATIVO/REPROVADO); apenas ATIVO tem acesso.

**Justificativa**:
- Princípio IX (Acesso Somente Após Aprovação)
- Simples, sem estados complexos
- Transições controladas via feature 003

**Implementação**:
- Filter verifica estado após validação de token
- 403 para não-ATIVO
- Auditoria registra tentativas de acesso

---

## Integrações Externas

### API da Caixa (Mega Sena)

**Endpoint**: `https://servicebus2.caixa.gov.br/portaldeloterias/api/megasena`

**Estratégia de Integração**:
- Polling via cron (configurável)
- HTTP client com timeout
- Retry com backoff (Resilience4j)
- Cache de último dado válido
- Validação rigorosa de dados

**Fallback**:
- Em indisponibilidade, servir último dado válido
- Registrar falha para retry
- Alerta para operação

---

### Firebase Admin SDK

**Estratégia de Integração**:
- Service account JSON (configurada via env vars)
- Verificação de token JWT
- Extração de uid e email_verified
- Cache de chaves públicas do Firebase

**Tratamento de Erros**:
- Firebase indisponível: degradação graciosa
- Token inválido/expirado: 401
- Email não verificado: recusa cadastro

---

## Padrões de Projeto

### Repository Pattern

**Implementação**: Spring Data JPA Repositories

**Justificativa**:
- Abstração sobre persistence
- Testabilidade (facilita mocks)
- Spring Data fornece implementação automaticamente
- Queries derivadas e @Query customizadas

---

### Service Layer

**Implementação**: Classes @Service com lógica de negócio

**Justificativa**:
- Separação de concerns (controllers não têm lógica)
- Reutilização entre controllers
- Testabilidade unitária
- Transaction management declarativo (@Transactional)

---

### DTO Pattern

**Implementação**: Request/Response objects separados de entities

**Justificativa**:
- Entities não expostas via API
- Controle sobre o que é serializado
- Validação via Bean Validation
- Isolamento de mudanças de schema

---

### Filter Chain

**Implementação**: IdentidadeTokenFilter + SecurityConfig

**Justificativa**:
- Centraliza lógica de autenticação/autorização
- Intercepta todas as requisições
- Pré-processamento antes de controllers
- Integração nativa com Spring Security

---

## Performance Considerations

### Índices de Banco de Dados

**Estratégia**:
- Índices em FKs (joins eficientes)
- Índices em colunas de busca (firebase_uid, estado)
- Índices compostos quando necessário (usuario_id + jogo_id)

**Monitoramento**:
- EXPLAIN QUERY em queries lentas
- Slow query log do MySQL
- Monitoramento via Spring Actuator

---

### Caching

**Decisão**: Caching limitado nesta versão.

**Justificativa**:
- Volume baixo (uso pessoal)
- Dados de concursos mudam raramente
- Cache de validação de token (short-lived)

**Futuro**:
- Redis para cache distribuído
- Cache de resultados de conferência
- Cache de dados de concursos

---

### Connection Pooling

**Implementação**: HikariCP (default no Spring Boot 2+)

**Configuração**:
- Pool size ajustável via env vars
- Timeout configurável
- Validation query

---

## Segurança

### HTTPS em Produção

**Requisito**: Obrigatório em produção

**Implementação**:
- Reverse proxy (nginx, AWS ALB) com TLS
- Spring Boot com redirect HTTP→HTTPS
- HSTS headers

---

### CORS

**Decisão**: Configurar CORS para frontend

**Implementação**:
- @CrossOrigin em controllers ou global
- Whitelist de origins permitidas
- Headers necessários expostos

---

### Rate Limiting

**Status**: Não implementado nesta versão

**Futuro**:
- Bucket4j ou Spring Cloud Gateway
- Limites por endpoint
- Proteção contra abuse

---

## Monitoramento e Observabilidade

### Logging

**Estratégia**:
- Structured logging (JSON em produção)
- Níveis configuráveis
- Context propagation (MDC)
- Sensitive data redacted

### Metrics

**Decisão**: Spring Actuator com Prometheus

**Métricas**:
- JVM metrics (memory, GC)
- HTTP metrics (requests, latency)
- Database metrics (connection pool, query time)
- Custom metrics (sync runs, auth failures)

### Tracing

**Status**: Não implementado nesta versão

**Futuro**:
- OpenTelemetry
- Distributed tracing
- Correlation IDs

---

## Deployment

### Dockerização

**Estratégia**: Multi-stage build

**Justificativa**:
- Imagem final pequena (sem build tools)
- Layers otimizadas para cache
- Reprodutibilidade

### Cloud-Native

**Características**:
- Configuração externa (env vars)
- Health checks (/actuator/health)
- Graceful shutdown
- Stateless (exceto banco de dados)

### Platform Considerations

**Apropriado para**:
- AWS ECS/Fargate
- Google Cloud Run
- Azure Container Instances
- Kubernetes (se necessário)

**Não requer**:
- Servidores dedicados
- Complexidade de k8s para uso pessoal

---

## Lições Aprendidas

### Decisões Acertadas

1. **Firebase Authentication**: Removeu complexidade de gestão de credenciais
2. **Testcontainers**: Testes de integração confiáveis e realistas
3. **Idempotência**: Simplificou recovery de falhas
4. **Isolamento no servidor**: Eliminou risco de IDOR

### Decisões que Revisaria

1. **Caching limitado**: Poderia ter implementado Redis desde início
2. **Rate limiting**: Deveria ter sido prioridade desde MVP
3. **Distributed tracing**: Valeria a pena desde início para debug

### Technical Debt Conhecido

1. Sem cache distribuído
2. Sem rate limiting
3. Sem distributed tracing
4. Hardcoding de alguns valores (configurável no futuro)
5. Testes de carga não implementados