# Quickstart — Identidade & Autenticação (feature 002)

Como configurar, rodar e verificar o backend de identidade localmente. Pré-requisito:
backend da feature 001 já compila (Java 21, Maven, MySQL via Docker).

---

## 1. Configuração (via ambiente — nada de segredo no código)

| Variável | Exemplo | Para quê |
|----------|---------|----------|
| `IDENTIDADE_PROJECT_ID` | `meu-projeto-megasena` | Project id do Identity Platform (emissor/audiência do token). |
| `GOOGLE_APPLICATION_CREDENTIALS` | `/run/secrets/sa.json` | Credencial de serviço do Firebase Admin SDK. |
| `IDENTIDADE_ADMINS_BOOTSTRAP` | `admin@exemplo.com` | E-mails provisionados como ADMINISTRADOR + ATIVO (admin inicial). |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/megasena` | Banco (reuso da 001). |

Bloco correspondente em `application.yml`:

```yaml
megasena:
  identidade:
    project-id: ${IDENTIDADE_PROJECT_ID:}
    admins-bootstrap: ${IDENTIDADE_ADMINS_BOOTSTRAP:}
```

No provedor (console do Identity Platform), habilite os métodos **E-mail/senha** e
**Google**, e ative **"uma conta por e-mail"** (account linking) para que o vínculo
Google↔e-mail+senha emita o mesmo `uid` (FR-014).

---

## 2. Subir o banco e aplicar migrações

```bash
# MySQL (se ainda não estiver de pé pela 001)
docker run --name megasena-mysql -e MYSQL_DATABASE=megasena \
  -e MYSQL_USER=megasena -e MYSQL_PASSWORD=megasena \
  -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 -d mysql:8

# Backend (Flyway aplica V1..V4 no boot)
cd backend && ./mvnw spring-boot:run
```

Confirme que `V3__create_usuario.sql` e `V4__create_evento_identidade.sql` aplicaram:

```bash
docker exec -it megasena-mysql \
  mysql -umegasena -pmegasena megasena -e "show tables; describe usuario;"
```

---

## 3. Obter um ID token para testar (emulador)

Use o **Firebase Auth Emulator** para gerar tokens sem provedor real:

```bash
firebase emulators:start --only auth
# crie um usuário e obtenha o idToken via REST do emulador:
curl -s "http://localhost:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake" \
  -H 'Content-Type: application/json' \
  -d '{"email":"teste@exemplo.com","password":"senha123","returnSecureToken":true}' \
  | jq -r .idToken
```

(Para apontar o Admin SDK ao emulador, exporte `FIREBASE_AUTH_EMULATOR_HOST=localhost:9099`.)

---

## 4. Verificar os fluxos (Acceptance Scenarios)

```bash
TOKEN="<idToken do passo 3>"

# US1 — auto-cadastro JIT: primeira chamada cria a conta em PENDENTE
curl -s -H "Authorization: Bearer $TOKEN" localhost:8080/api/auth/me | jq
# => { "estado": "PENDENTE", "papel": "USUARIO", ... }

# US2 — conta pendente é barrada em recurso de usuário (gate de estado)
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" localhost:8080/api/exemplo/recurso-de-usuario
# => 403 (CONTA_PENDENTE)

# US2 — sem token: 401 genérico (não revela existência de conta — SC-006)
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/api/auth/me
# => 401

# Admin bootstrap: cadastre com o e-mail de IDENTIDADE_ADMINS_BOOTSTRAP
# => /api/auth/me retorna estado ATIVO e papel ADMINISTRADOR

# US3 — logout revoga sessões
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  -H "Authorization: Bearer $TOKEN" localhost:8080/api/auth/logout
# => 204; chamadas subsequentes exigem novo login
```

---

## 5. Critérios de aceite (mapeамento rápido)

| Verificação | Requisito / SC |
|-------------|----------------|
| Conta nasce PENDENTE no 1º acesso | FR-003, IX, US1 |
| Nenhuma senha persistida (inspecione `usuario`: não há coluna de senha) | SC-004, VII |
| Identidade derivada do token, não do cliente | FR-005, I, VII |
| Pendente/reprovada barrada em recurso de usuário | FR-006, IX, SC-003 |
| Google com e-mail não verificado é recusado | FR-015 |
| Duas vias com mesmo e-mail = uma conta (mesmo `uid`) | FR-014, FR-008 |
| Erro de auth não revela existência de e-mail | FR-012, SC-006 |
| Eventos em `evento_identidade`, sem credencial/PII em claro | FR-011, V |
| Logout exige nova autenticação | FR-010, US3 |

---

## 6. Testes automatizados

```bash
cd backend && ./mvnw verify
```

Cobertura crítica (Princípio III): provisionamento JIT, gate de estado, unicidade
(FR-008), e-mail verificado/vínculo (FR-014/FR-015) e derivação de identidade — todos com
**verificador de identidade dublê** (sem rede nem credencial real).
