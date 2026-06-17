# Mega Sena Sync — Backend

Serviço Spring Boot que sincroniza dados de concursos da Mega Sena a partir da API oficial da Caixa.

## Pré-requisitos

- Java 21 (JDK)
- Maven 3.9+ (ou use o wrapper `./mvnw`)
- MySQL 8 (local ou via Docker)
- Docker (para testes de integração com Testcontainers)

## Configuração

| Variável de ambiente       | Propriedade                     | Default                                                                 |
|----------------------------|---------------------------------|-------------------------------------------------------------------------|
| `MYSQL_HOST`               | `spring.datasource.url`        | `localhost`                                                             |
| `MYSQL_PORT`               | `spring.datasource.url`        | `3306`                                                                  |
| `MYSQL_DB`                 | `spring.datasource.url`        | `megasena`                                                              |
| `MYSQL_USER`               | `spring.datasource.username`   | `megasena`                                                              |
| `MYSQL_PASSWORD`           | `spring.datasource.password`   | `megasena`                                                              |
| `MEGASENA_SOURCE_URL`      | `megasena.source.base-url`     | `https://servicebus2.caixa.gov.br/portaldeloterias/api/megasena`       |
| `MEGASENA_SYNC_CRON`       | `megasena.sync.cron`           | `0 0 22 * * *` (22h UTC)                                               |
| `MEGASENA_ADMIN_TOKEN`     | `megasena.admin.token`         | `changeme` (obrigatório alterar em produção)                            |

## Build e testes

```bash
cd backend
./mvnw clean verify   # compila + testes unitários + integração (Testcontainers + WireMock)
```

## Executar

```bash
cd backend
./mvnw spring-boot:run
```

O Flyway aplica as migrações automaticamente na primeira execução.

## Endpoints

### Públicos (leitura)

| Método | Rota                        | Descrição                        |
|--------|-----------------------------|----------------------------------|
| GET    | `/api/concursos/latest`     | Último concurso sincronizado     |
| GET    | `/api/concursos/{numero}`   | Concurso por número              |

### Admin (requer `Authorization: Bearer <token>`)

| Método | Rota                      | Descrição                                     |
|--------|---------------------------|-----------------------------------------------|
| GET    | `/api/admin/sync/status`  | Status da última sincronização                |
| POST   | `/api/admin/sync/run`     | Disparo manual de sincronização (202 / 409)   |

## Carga histórica

```bash
curl -X POST http://localhost:8080/api/admin/sync/run \
  -H "Authorization: Bearer $MEGASENA_ADMIN_TOKEN"
```

A carga é retomável: se interrompida, basta disparar novamente.
