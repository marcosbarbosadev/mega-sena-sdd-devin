# Phase 1 — Data Model: Identidade & Autenticação

Deriva as entidades da spec (Key Entities + Functional Requirements). Schema aplicado por
migrações Flyway versionadas (`V3__`, `V4__`), continuando `V1`/`V2` da feature 001.

---

## Entidade: `Usuario` (tabela `usuario`)

Representa a pessoa no sistema. **`id` é a chave de isolamento** (Princípio I) à qual as
features seguintes (jogos, conferências) farão FK. Nenhuma senha é armazenada (Princípio
VII).

| Campo | Tipo (MySQL) | Regras |
|-------|--------------|--------|
| `id` | `BINARY(16)` (UUID) | PK. Identificador local estável, **independente do provedor** (suporta a abstração do Princípio VII). |
| `provider_uid` | `VARCHAR(128)` | **ÚNICO**, NOT NULL. Identificador emitido pelo provedor (Firebase `uid`). Chave de resolução da conta a cada requisição. |
| `email` | `VARCHAR(320)` | **ÚNICO**, NOT NULL. Mínimo necessário (LGPD). |
| `papel` | `ENUM('USUARIO','ADMINISTRADOR')` | NOT NULL, default `USUARIO` (FR-009). |
| `estado` | `ENUM('PENDENTE','ATIVO','REPROVADO')` | NOT NULL, default `PENDENTE` (FR-003, FR-007, IX). |
| `metodo_login` | `ENUM('SENHA','GOOGLE')` | NOT NULL. Método com que a conta foi criada (auditoria/diagnóstico). |
| `criado_em` | `DATETIME(6)` | NOT NULL. Marco de criação. |
| `atualizado_em` | `DATETIME(6)` | NOT NULL. Atualizado a cada mudança de estado/papel. |
| `ultimo_acesso_em` | `DATETIME(6)` | NULL. Última autenticação bem-sucedida. |

**Índices/constraints**:
- `PRIMARY KEY (id)`
- `UNIQUE KEY uk_usuario_provider_uid (provider_uid)` — garante 1 conta por identidade do
  provedor (FR-008).
- `UNIQUE KEY uk_usuario_email (email)` — garante 1 conta por e-mail (FR-008, Assumption
  "identidade única por pessoa"). Em corrida de criação concorrente, a 2ª inserção falha
  na constraint e o serviço relê a conta vencedora (resolve-or-create idempotente).

**Estados (enum `EstadoConta`)** e transições:

```
                 (admin aprova / bootstrap)
   [novo] ──▶ PENDENTE ───────────────────────▶ ATIVO
                  │                                │
                  │ (admin reprova)                │ (admin reprova/bloqueia — feature 003)
                  ▼                                ▼
               REPROVADO ◀───────────────────────┘
```

- Toda conta nasce **PENDENTE** (FR-003). Exceção: e-mail em `admins-bootstrap` nasce
  **ATIVO + ADMINISTRADOR** (R5).
- Apenas **ATIVO** passa o gate de acesso a recursos de usuário (FR-006, IX).
- **REPROVADO** = sem acesso; "bloqueada" é sinônimo, **não** é estado distinto
  (clarificação Q3). As transições efetivas a partir de PENDENTE/ATIVO são executadas
  pela feature 003 (moderação); esta feature cria o estado inicial e **aplica o gate**.

**Papéis (enum `Papel`)**: `USUARIO` (padrão) e `ADMINISTRADOR` (FR-009). O papel
**autoriza** ações administrativas em features posteriores; a moderação em si é a 003.

---

## Entidade: `EventoIdentidade` (tabela `evento_identidade`)

Trilha de auditoria dos eventos de identidade (FR-011, Princípio V). **Sem credenciais,
sem token e sem PII sensível em texto claro** (R8).

| Campo | Tipo (MySQL) | Regras |
|-------|--------------|--------|
| `id` | `BIGINT AUTO_INCREMENT` | PK. |
| `usuario_id` | `BINARY(16)` | NULL. FK → `usuario(id)`. Nulo quando o evento não resolve uma conta (ex.: token inválido). |
| `tipo` | `ENUM('CADASTRO','AUTENTICACAO','LOGOUT','ACESSO_NEGADO')` | NOT NULL. |
| `metodo_login` | `ENUM('SENHA','GOOGLE')` | NULL (quando aplicável). |
| `sucesso` | `BOOLEAN` | NOT NULL. |
| `motivo` | `VARCHAR(80)` | NULL. Código curto/legível (ex.: `EMAIL_NAO_VERIFICADO`, `CONTA_PENDENTE`, `TOKEN_INVALIDO`). Sem dado pessoal livre. |
| `correlation_id` | `VARCHAR(64)` | NULL. Correlação com o log estruturado da requisição. |
| `criado_em` | `DATETIME(6)` | NOT NULL. |

**Índices/constraints**:
- `PRIMARY KEY (id)`
- `KEY idx_evento_usuario (usuario_id)`
- `KEY idx_evento_tipo_data (tipo, criado_em)`
- `CONSTRAINT fk_evento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)`

**Eventos registrados**:
- `CADASTRO` — conta provisionada (JIT) em PENDENTE (ou bootstrap admin).
- `AUTENTICACAO` — token verificado com sucesso e conta resolvida (atualiza
  `ultimo_acesso_em`); ou falha de verificação (`sucesso=false`, `usuario_id` nulo).
- `LOGOUT` — refresh tokens revogados (FR-010).
- `ACESSO_NEGADO` — conta autenticada porém sem acesso (PENDENTE/REPROVADO) tentou
  recurso de usuário (FR-006, IX), com `motivo`.

---

## Objeto de fronteira (não persistido): `IdentidadeVerificada`

Resultado da porta `VerificadorDeIdentidade.verify(token)` — o contrato que o domínio
consome, isolando o provedor concreto (Princípio VII).

| Campo | Tipo | Origem (claim Firebase) |
|-------|------|--------------------------|
| `uid` | `String` | `sub` |
| `email` | `String` | `email` |
| `emailVerificado` | `boolean` | `email_verified` |
| `metodoLogin` | `MetodoLogin` (`SENHA`/`GOOGLE`) | `firebase.sign_in_provider` (`password`/`google.com`) |

Validações aplicadas **antes** de persistir/conceder acesso:
- Assinatura, emissor, audiência e expiração — verificadas pelo adaptador (R2).
- `emailVerificado == true` para criar conta (FR-015); caso contrário recusa sem
  persistir.

---

## Principal de segurança (não persistido): `UsuarioAutenticado`

Populado no `SecurityContext` após resolução da conta. Carrega `usuarioId` (a chave de
isolamento), `papel` e `estado`. As autoridades concedidas dependem do estado:
- **ATIVO** → `ROLE_USUARIO` (e `ROLE_ADMINISTRADOR` se `papel = ADMINISTRADOR`).
- **PENDENTE/REPROVADO** → sem autoridades de acesso a recursos; só pode consultar o
  próprio estado em `GET /api/auth/me`.

---

## Migrações Flyway

- **`V3__create_usuario.sql`** — tabela `usuario` com PK, uniques (`provider_uid`,
  `email`) e enums/colunas acima.
- **`V4__create_evento_identidade.sql`** — tabela `evento_identidade` com FK para
  `usuario` e índices de consulta.

Ambas idempotentes na aplicação em base nova (portão de merge nº 3 da constituição).
