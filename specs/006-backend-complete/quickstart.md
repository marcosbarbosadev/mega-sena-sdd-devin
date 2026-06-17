# Quickstart - Backend Completo

Guia rápido para configurar e executar o backend do Mega Sena Manager.

## Pré-requisitos

- Java 21 (JDK)
- Maven 3.9+ (ou use o wrapper `./mvnw`)
- MySQL 8 (local ou via Docker)
- Docker (para testes de integração)
- Projeto Firebase configurado (Authentication)

## Configuração do Firebase

1. Crie um projeto no [Firebase Console](https://console.firebase.google.com/)
2. Ative **Authentication** com:
   - **Email/Password**
   - **Google** sign-in
3. Vá em **Project Settings** > **Service accounts**
4. Gere uma chave privada (JSON)
5. Salve o JSON como `firebase-service-account.json` em local seguro

## Configuração do MySQL

### Via Docker (recomendado)

```bash
docker run -d \
  --name megasena-db \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=megasena \
  -e MYSQL_USER=megasena \
  -e MYSQL_PASSWORD=megasena \
  -p 3306:3306 \
  mysql:8
```

### Local

Instale MySQL 8 e crie o banco:

```sql
CREATE DATABASE megasena CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'megasena'@'localhost' IDENTIFIED BY 'megasena';
GRANT ALL PRIVILEGES ON megasena.* TO 'megasena'@'localhost';
FLUSH PRIVILEGES;
```

## Variáveis de Ambiente

Crie um arquivo `.env` ou exporte as variáveis:

```bash
# Banco de dados
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DB=megasena
export MYSQL_USER=megasena
export MYSQL_PASSWORD=megasena

# API da Caixa
export MEGASENA_SOURCE_URL=https://servicebus2.caixa.gov.br/portaldeloterias/api/megasena

# Sincronização
export MEGASENA_SYNC_CRON="0 0 22 * * *"  # 22h UTC diário

# Admin
export MEGASENA_ADMIN_TOKEN=seu-token-seguro-aqui

# Firebase
export FIREBASE_PROJECT_ID=seu-proyecto-id
# Firebase Admin SDK usa o arquivo JSON ou variáveis específicas
```

## Build e Execução

### Compilar e Testar

```bash
cd backend
./mvnw clean verify
```

Isso executa:
- Compilação
- Testes unitários
- Testes de integração (Testcontainers + WireMock)

### Executar em Desenvolvimento

```bash
cd backend
./mvnw spring-boot:run
```

O serviço estará disponível em `http://localhost:8080`

### Build para Produção

```bash
cd backend
./mvnw clean package
java -jar target/mega-sena-sync-0.0.1-SNAPSHOT.jar
```

## Endpoints Principais

### Concursos (Públicos)

```bash
# Último concurso
curl http://localhost:8080/api/concursos/latest

# Concurso por número
curl http://localhost:8080/api/concursos/2750
```

### Sincronização (Admin)

```bash
# Status da última sincronização
curl http://localhost:8080/api/admin/sync/status \
  -H "Authorization: Bearer $MEGASENA_ADMIN_TOKEN"

# Disparar sincronização manual
curl -X POST http://localhost:8080/api/admin/sync/run \
  -H "Authorization: Bearer $MEGASENA_ADMIN_TOKEN"
```

### Autenticação (Firebase)

Primeiro obtenha um token JWT do Firebase, depois:

```bash
# Auto-cadastro
curl -X POST http://localhost:8080/api/contas \
  -H "Authorization: Bearer $FIREBASE_TOKEN"

# Perfil
curl http://localhost:8080/api/perfil \
  -H "Authorization: Bearer $FIREBASE_TOKEN"

# Logout
curl -X POST http://localhost:8080/api/logout \
  -H "Authorization: Bearer $FIREBASE_TOKEN"
```

### Moderação (Admin)

```bash
# Listar pendentes
curl http://localhost:8080/api/admin/moderacao/pendentes \
  -H "Authorization: Bearer $ADMIN_FIREBASE_TOKEN"

# Aprovar conta
curl -X POST http://localhost:8080/api/admin/moderacao/123/aprovar \
  -H "Authorization: Bearer $ADMIN_FIREBASE_TOKEN"

# Reprovar conta
curl -X POST http://localhost:8080/api/admin/moderacao/123/reprovar \
  -H "Authorization: Bearer $ADMIN_FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"motivo": "Usuário não autorizado"}'
```

### Jogos (Usuário)

```bash
# Listar jogos
curl http://localhost:8080/api/jogos \
  -H "Authorization: Bearer $USER_FIREBASE_TOKEN"

# Cadastrar jogo (manual)
curl -X POST http://localhost:8080/api/jogos \
  -H "Authorization: Bearer $USER_FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tipo_selecao": "INFORMADO",
    "dezenas": [10, 20, 30, 40, 50, 60]
  }'

# Cadastrar jogo (gerado)
curl -X POST http://localhost:8080/api/jogos \
  -H "Authorization: Bearer $USER_FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tipo_selecao": "GERADO",
    "quantidade_dezenas": 7
  }'

# Editar jogo
curl -X PUT http://localhost:8080/api/jogos/123 \
  -H "Authorization: Bearer $USER_FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tipo_selecao": "INFORMADO",
    "dezenas": [11, 21, 31, 41, 51, 61]
  }'

# Excluir jogo
curl -X DELETE http://localhost:8080/api/jogos/123 \
  -H "Authorization: Bearer $USER_FIREBASE_TOKEN"
```

### Conferência (Usuário)

```bash
# Conferir jogo específico
curl http://localhost:8080/api/conferencia/123 \
  -H "Authorization: Bearer $USER_FIREBASE_TOKEN"

# Conferir todos os jogos
curl http://localhost:8080/api/conferencia \
  -H "Authorization: Bearer $USER_FIREBASE_TOKEN"
```

## Fluxo Completo de Teste

### 1. Configurar Admin Inicial

```bash
# O primeiro admin precisa ser criado via provisionamento
# Isso pode ser feito via script de seed ou direto no banco
```

### 2. Sincronizar Concursos

```bash
# Disparar carga histórica
curl -X POST http://localhost:8080/api/admin/sync/run \
  -H "Authorization: Bearer $MEGASENA_ADMIN_TOKEN"
```

### 3. Criar Usuário

```bash
# No Firebase Console ou via SDK, crie um usuário
# Obtenha o token JWT
FIREBASE_TOKEN=$(obter_token_firebase)

# Auto-cadastro no backend
curl -X POST http://localhost:8080/api/contas \
  -H "Authorization: Bearer $FIREBASE_TOKEN"
```

### 4. Aprovar Usuário

```bash
# Como admin, aprovar a conta
ADMIN_TOKEN=$(obter_token_admin_firebase)
CONTA_ID=123

curl -X POST http://localhost:8080/api/admin/moderacao/$CONTA_ID/aprovar \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 5. Cadastrar Jogos

```bash
# Agora com conta ativa, cadastrar jogos
curl -X POST http://localhost:8080/api/jogos \
  -H "Authorization: Bearer $FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tipo_selecao": "INFORMADO",
    "dezenas": [4, 12, 23, 34, 45, 56]
  }'
```

### 6. Conferir Jogos

```bash
# Quando o concurso for sorteado, conferir
curl http://localhost:8080/api/conferencia \
  -H "Authorization: Bearer $FIREBASE_TOKEN"
```

## Troubleshooting

### Banco de Dados Não Conecta

Verifique se MySQL está rodando:

```bash
docker ps | grep megasena-db
```

Teste conexão:

```bash
mysql -h localhost -P 3306 -u megasena -p megasena
```

### Firebase Authentication Falha

Verifique se:
- Projeto Firebase está correto
- Chave privada JSON está configurada
- Token JWT é válido e não expirou
- Email está verificado no Firebase

### Sincronização Falha

Verifique:
- API da Caixa está acessível
- Token de admin está correto
- Logs da aplicação para detalhes do erro

### Testes Falham

```bash
# Verifique se Docker está rodando
docker ps

# Execute testes com logs detalhados
./mvnw clean verify -X
```

## Logs e Monitoramento

### Logs em Desenvolvimento

```bash
# Logs aparecem no console ao executar ./mvnw spring-boot:run
```

### Logs em Produção

```bash
# Configure logging em application.yml
# Ou use variáveis de ambiente
export LOGGING_LEVEL_ROOT=INFO
export LOGGING_LEVEL_COM_MEGASENA=DEBUG
```

### Actuator Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics (se habilitado)
curl http://localhost:8080/actuator/prometheus
```

## Próximos Passos

- [ ] Configurar CI/CD (GitHub Actions, Jenkins, etc.)
- [ ] Configurar monitoramento (Prometheus, Grafana)
- [ ] Configurar alertas (falhas de sync, erros de autenticação)
- [ ] Otimizar performance (cache, índices adicionais)
- [ ] Implementar rate limiting
- [ ] Adicionar testes de carga