# Quickstart — Aprovação de Contas (Admin)

Como configurar, rodar e verificar a feature 003. Pré-requisito: a feature 002
(Identidade & Autenticação) já está aplicada — tabelas `usuario` e
`evento_identidade`, cadeia de segurança com `VerificadorDeIdentidade` e papel
`ROLE_ADMINISTRADOR`.

## 1. Pré-requisitos

- Backend da 001/002 buildando (`backend/`, Java 21, Maven).
- MySQL acessível (mesma instância das features anteriores).
- Pelo menos **um administrador ativo** existente. Conforme a 002, o admin inicial
  é provisionado fora de banda via `megasena.identidade.admins-bootstrap` (e-mail do
  admin nasce `ATIVO + ADMINISTRADOR`). Sem isso, não há quem opere a fila.
- Para exercitar a API manualmente, um **ID token** de uma conta admin (obtido do
  provedor / emulador Firebase, como na 002).

## 2. Migração

Ao subir a aplicação, o Flyway aplica `V5__create_decisao_moderacao.sql`
(idempotente em base nova), criando a tabela de auditoria das decisões. Nenhuma
alteração em `usuario`.

```bash
cd backend && ./mvnw flyway:info   # deve listar V5 como pendente/aplicada
```

## 3. Subir o backend

```bash
cd backend && ./mvnw spring-boot:run
```

## 4. Verificação manual (curl)

> Substitua `$ADMIN_TOKEN` por um ID token de conta **ADMINISTRADOR/ATIVO** e
> `$USER_TOKEN` por um de conta **comum**. `$ID` é o uuid de uma conta PENDENTE
> (crie uma fazendo `GET /api/auth/me` com um token novo, da 002).

Listar a fila de pendentes:
```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
     http://localhost:8080/api/admin/contas/pendentes
# 200 + array com { id, email, metodoLogin, criadoEm }
```

Não-admin é barrado (FR-002):
```bash
curl -i -H "Authorization: Bearer $USER_TOKEN" \
     http://localhost:8080/api/admin/contas/pendentes
# 403 ACESSO_NEGADO
```

Aprovar (PENDENTE → ATIVO):
```bash
curl -i -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
     http://localhost:8080/api/admin/contas/$ID/aprovar
# 204 — a conta agora passa o gate de acesso da 002
```

Reprovar sem motivo é recusado (FR-004a):
```bash
curl -i -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
     -H "Content-Type: application/json" -d '{}' \
     http://localhost:8080/api/admin/contas/$ID/reprovar
# 400 MOTIVO_OBRIGATORIO
```

Reprovar com motivo (PENDENTE → REPROVADO):
```bash
curl -i -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"motivo":"Cadastro não reconhecido"}' \
     http://localhost:8080/api/admin/contas/$ID/reprovar
# 204 — decisão + motivo gravados em decisao_moderacao
```

Transição inválida (conta já decidida) é recusada (FR-005/FR-008):
```bash
curl -i -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
     http://localhost:8080/api/admin/contas/$ID/aprovar
# 409 TRANSICAO_INVALIDA (a conta não está mais PENDENTE)
```

## 5. Conferir a auditoria

```sql
SELECT usuario_id, admin_id, decisao, motivo, criado_em
FROM decisao_moderacao ORDER BY criado_em DESC;
-- cada aprovação/reprovação gera exatamente uma linha (FR-007)
```

## 6. O que validar (mapeado aos critérios de sucesso)

- **SC-002 / FR-002**: não-admin recebe 403 em todas as rotas `/api/admin/**`.
- **SC-003 / FR-007**: cada decisão gera uma linha em `decisao_moderacao`.
- **SC-004 / FR-005**: aprovar/reprovar uma conta não-pendente retorna 409 sem
  alterar o estado.
- **SC-005 / FR-006**: imediatamente após aprovar, a conta passa no gate (`GET
  /api/perfil` da 002 deixa de retornar 403); após reprovar, continua bloqueada.
- **SC-006 / FR-008**: duas decisões concorrentes sobre a mesma conta — só a 1ª
  vence (a 2ª recebe 409); teste de integração com Testcontainers exercita a
  corrida via UPDATE condicionado.
- **FR-004a**: reprovação sem motivo → 400.

## 7. Testes automatizados

```bash
cd backend && ./mvnw test
```

Cobertura obrigatória (Princípio III): guarda de transição (só de PENDENTE),
exigência de motivo na reprovação, autorização admin-only (admin vs. usuário comum
com verificador de identidade dublê), concorrência (UPDATE condicionado) e gravação
da decisão na auditoria.
