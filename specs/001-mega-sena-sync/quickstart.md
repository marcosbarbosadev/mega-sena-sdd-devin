# Quickstart: Sincronização com a API da Mega Sena

**Feature**: 001-mega-sena-sync | **Date**: 2026-05-31

Como rodar, configurar e verificar o backend desta feature.

## Pré-requisitos

- Java 21 (JDK)
- Maven 3.9+
- MySQL 8 (local ou via Docker)
- Docker (para os testes de integração com Testcontainers)

## Configuração

Variáveis/propriedades principais (`backend/src/main/resources/application.yml`,
sobrescritíveis por ambiente):

| Propriedade | Descrição | Default sugerido |
|-------------|-----------|------------------|
| `spring.datasource.url` | URL do MySQL | `jdbc:mysql://localhost:3306/megasena` |
| `spring.datasource.username` / `password` | Credenciais do banco | — |
| `megasena.source.base-url` | Base URL da fonte oficial | `https://servicebus2.caixa.gov.br/portaldeloterias/api` |
| `megasena.source.user-agent` | User-Agent enviado à fonte | string de navegador |
| `megasena.source.timeout-ms` | Timeout de conexão/leitura | `10000` |
| `megasena.sync.cron` | Expressão cron da sync diária | `0 0 21 * * *` (21h) |
| `megasena.sync.retry.max-attempts` | Tentativas no retry | `3` |
| `megasena.sync.retry.backoff-ms` | Backoff inicial | `2000` |
| `megasena.admin.token` | Segredo do perfil admin | — (obrigatório) |

## Build e testes

```bash
cd backend
mvn clean verify        # compila + roda testes (unidade, WireMock, Testcontainers)
```

## Executar

```bash
cd backend
mvn spring-boot:run
```

Na primeira execução, o Flyway aplica as migrações (cria `concurso`,
`concurso_dezena`, `sync_run`).

## Carga histórica inicial

A carga histórica é uma sincronização que preenche todos os concursos faltantes (de 1
até o último da fonte). Dispare-a manualmente como admin após subir o app:

```bash
curl -X POST http://localhost:8080/api/admin/sync/run \
  -H "Authorization: Bearer $MEGASENA_ADMIN_TOKEN"
```

Ela é **retomável**: se interromper, basta disparar novamente — só os concursos faltantes
são importados (idempotência).

## Verificação (mapeada aos critérios de sucesso)

1. **Status da sincronização (SC-005)** — admin:
   ```bash
   curl http://localhost:8080/api/admin/sync/status \
     -H "Authorization: Bearer $MEGASENA_ADMIN_TOKEN"
   # Espera-se: status SUCESSO, iniciadoEm/finalizadoEm, concursosImportados, faixa importada
   ```

2. **Concurso mais recente (US1)**:
   ```bash
   curl http://localhost:8080/api/concursos/latest
   # Espera-se: numero, dataSorteio, 6 dezenas (1..60), valorPremio
   ```

3. **Sequência contínua sem lacunas (SC-002)**: contar concursos armazenados e conferir
   que vão de 1 até o número mais recente sem buracos.

4. **Idempotência (SC-004)**: disparar a sync duas vezes seguidas e confirmar 0
   duplicatas e nenhum dado de concurso confirmado alterado.

5. **Degradação graciosa (SC-003)**: com a fonte indisponível (ou base-url apontando para
   um destino fora do ar), confirmar que `GET /api/concursos/{numero}` já sincronizados
   continua respondendo 200, e que a `SyncRun` registra FALHA.

## Como os testes cobrem os caminhos críticos

- **WireMock**: sucesso, timeout/5xx e payload malformado da fonte (FR-007/009/010).
- **Testcontainers (MySQL)**: migrações Flyway, idempotência e sequência sem lacunas.
- **Unidade**: parse/validação das dezenas e mapeamento do valor do prêmio.
