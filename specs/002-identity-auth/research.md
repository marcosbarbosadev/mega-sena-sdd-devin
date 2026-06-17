# Phase 0 — Research: Identidade & Autenticação

Resolve as incógnitas do Technical Context. Cada decisão segue o formato
**Decisão / Racional / Alternativas consideradas**.

---

## R1. Provedor de identidade gerenciado

**Decisão**: **Google Identity Platform** (GCP Identity Platform / Firebase
Authentication) como provedor concreto, com a fronteira abstraída no código pela porta
`VerificadorDeIdentidade`.

**Racional**:
- Atende aos dois métodos exigidos: **e-mail+senha** e **federação Google** (FR-001,
  FR-004), com a federação Google nativa e de baixo atrito.
- Emite **ID tokens OIDC (JWT, RS256)** verificáveis no servidor (assinatura, emissor,
  audiência, expiração), satisfazendo o Princípio VII sem armazenar senhas.
- Cobre o ciclo de credenciais (cadastro, redefinição de senha, verificação de e-mail)
  fora da aplicação — exatamente o que o Princípio VII e o "Out of Scope" da spec pedem.
- Escolha explícita do usuário para esta feature (decisão registrada no plano).

**Alternativas consideradas**:
- **AWS Cognito**: equivalente em capacidade (user pool + federação Google + JWT). Não
  escolhido por preferência de stack do usuário; a abstração da porta mantém a troca
  viável no futuro.
- **Keycloak auto-hospedado**: sem lock-in, mas exigiria operar o serviço — fora do
  espírito de "identidade **gerenciada**" do Princípio VII e adiciona complexidade
  operacional (Princípio VI).

---

## R2. Verificação do token no servidor

**Decisão**: Verificar o ID token com o **Firebase Admin SDK**
(`FirebaseAuth.verifyIdToken(token, checkRevoked=true)`) dentro de um adaptador que
implementa a porta `VerificadorDeIdentidade`. Um filtro de segurança
(`IdentidadeTokenFilter`) extrai o `Authorization: Bearer <idToken>`, chama a porta e,
em sucesso, resolve a conta e popula o `SecurityContext`.

**Racional**:
- O Admin SDK valida assinatura, emissor (`https://securetoken.google.com/<project-id>`),
  audiência (`<project-id>`) e expiração, e **cacheia as chaves públicas** — verificação
  em poucos ms após o warm-up.
- `checkRevoked=true` permite que o **logout** (R6) invalide sessões revogando os refresh
  tokens, atendendo FR-010 mesmo com tokens stateless.
- Encapsular o SDK atrás da porta cumpre o Princípio VII (fronteira abstraída) e permite
  **testar com um verificador dublê**, sem rede nem credencial real.

**Alternativas consideradas**:
- **Spring Security OAuth2 Resource Server (NimbusJwtDecoder via `jwk-set-uri`)**: o
  endpoint de chaves do Firebase publica **certificados X.509**, não um JWKS padrão, o
  que exigiria um `JwtDecoder` customizado. O Admin SDK já trata isso e ainda oferece
  `checkRevoked` — menos código e mais alinhado ao provedor escolhido.

---

## R3. Modelo de provisionamento da conta local

**Decisão**: Provisionamento **just-in-time (JIT)**. Na primeira requisição autenticada
de uma identidade ainda sem conta local, o backend cria a conta em **PENDENTE** e audita
o evento `CADASTRO`. Requisições seguintes resolvem a conta existente por `provider_uid`.
Não há endpoint de "registro" separado — o cadastro no provedor (via SDK do cliente) +
a primeira chamada autenticada constituem o auto-cadastro (User Story 1).

**Racional**:
- Simplicidade (Princípio VI): elimina um fluxo/endpoint de registro redundante; o
  provedor já é a fonte de verdade da credencial.
- A unicidade (FR-008) é garantida pela **chave única `provider_uid`** e por **`email`
  único**; corridas de criação concorrente são resolvidas por constraint no banco +
  tratamento idempotente (resolve-or-create).

**Alternativas consideradas**:
- **Endpoint `POST /api/auth/register` explícito**: redundante com o cadastro do
  provedor e adiciona estado intermediário; rejeitado por YAGNI.

---

## R4. Vínculo de identidade e e-mail verificado (FR-014 / FR-015)

**Decisão**:
- Configurar o Identity Platform para **"uma conta por e-mail"** (account linking),
  de modo que o provedor emita o **mesmo `uid`** quando a mesma pessoa usa e-mail+senha
  e Google com o mesmo e-mail verificado — o backend chaveia por `provider_uid` e vê uma
  só identidade (FR-014).
- Independentemente, o backend **lê o claim `email_verified`** do token e, para
  identidades cujo e-mail **não** está verificado, **recusa o provisionamento/acesso**
  (FR-015), sem persistir conta.

**Racional**:
- Defesa em profundidade: mesmo que a configuração do provedor mude, o backend não cria
  conta a partir de e-mail não verificado, fechando a porta a sequestro de identidade
  (decisão de clarificação Q1/Q2).
- O claim `firebase.sign_in_provider` (`password` ou `google.com`) permite auditar o
  método de login e aplicar a regra de e-mail verificado de forma específica.

**Alternativas consideradas**:
- **Vínculo manual pelo usuário**: fora de escopo (a spec assume uma identidade por
  pessoa); rejeitado.
- **Confiar só na config do provedor**: frágil; a checagem server-side de
  `email_verified` é barata e elimina a dependência de configuração externa correta.

---

## R5. Papéis e administrador inicial

**Decisão**: Papel armazenado localmente em `usuario.papel`
(`USUARIO`/`ADMINISTRADOR`). O **admin inicial** é provisionado **fora de banda** por
configuração: uma propriedade de ambiente
`megasena.identidade.admins-bootstrap` (lista de e-mails). Quando uma identidade cujo
e-mail está nessa lista é provisionada, a conta nasce diretamente **ATIVO +
ADMINISTRADOR**. Demais contas nascem PENDENTE/USUARIO.

**Racional**:
- Resolve o paradoxo "quem aprova o primeiro admin" (Assumption da spec) sem UI e sem
  burlar o gate para usuários comuns.
- Cloud-native (Princípio VIII): a lista vem do ambiente/secret manager, não hard-coded.
- A moderação (aprovar/reprovar contas comuns) permanece como feature 003; aqui só o
  **distintivo de papel** e o bootstrap existem (FR-009).

**Alternativas consideradas**:
- **Seed via migração Flyway com e-mail fixo**: acopla um e-mail ao schema versionado e
  vaza dado de ambiente para o código; rejeitado em favor de configuração.
- **Custom claims de papel no provedor**: empurraria autorização para o provedor e
  acoplaria regras de negócio a ele (contra Princípio VII); o papel é decisão da
  aplicação.

---

## R6. Logout e ciclo de sessão

**Decisão**: Tokens são **stateless e de curta duração** (ID token Firebase ~1h; refresh
gerido pelo SDK do cliente). O **logout** (`POST /api/auth/logout`) **revoga os refresh
tokens** da identidade via Admin SDK (`revokeRefreshTokens(uid)`) e audita `LOGOUT`; o
cliente descarta o token local. Com `verifyIdToken(checkRevoked=true)`, ID tokens
emitidos antes da revogação deixam de ser aceitos após expirarem/serem checados,
exigindo nova autenticação (FR-010).

**Racional**:
- Mantém o processo stateless (Princípio VIII) sem sessão em servidor.
- A revogação cobre o requisito de "exigir nova autenticação" sem inventar um store de
  sessão próprio (Princípio VI).

**Alternativas consideradas**:
- **Blacklist de tokens no backend**: introduz estado e complexidade; a revogação no
  provedor já resolve.

---

## R7. Erros que não revelam existência de conta (SC-006 / FR-012)

**Decisão**: A autenticação acontece no provedor (o backend não compara senha), então o
backend **nunca** emite mensagens do tipo "e-mail não encontrado". Respostas do backend
para token ausente/inválido são **401 genérico**; para conta sem acesso
(pendente/reprovada) são **403 com o estado**, derivado do token já verificado — não de
uma busca por e-mail.

**Racional**: O design já satisfaz o requisito porque a verificação de credencial não
mora na aplicação; resta apenas padronizar 401/403 genéricos.

---

## R8. Auditoria sem PII/credenciais em texto claro (Princípio V)

**Decisão**: A tabela `evento_identidade` referencia a conta por `usuario_id` (FK) e
registra `tipo`, `metodo_login`, `sucesso`, `motivo` (enum/curto), `correlation_id` e
`criado_em`. **Não** armazena senhas (nunca chegam à aplicação) nem o token. Eventos sem
conta resolvida (ex.: token inválido) gravam `usuario_id` nulo + motivo, sem e-mail em
claro. O e-mail da conta vive apenas em `usuario` (mínimo necessário — LGPD).

**Racional**: Trilha auditável (quem/o quê/quando) com minimização de dados, alinhada aos
Princípios V e à conformidade LGPD.

---

## Incógnitas remanescentes

Nenhuma marcada como **NEEDS CLARIFICATION**. Tempo de vida de token e retenção de
auditoria adotam os padrões acima; ajustes finos são operacionais e não bloqueiam o
design.
