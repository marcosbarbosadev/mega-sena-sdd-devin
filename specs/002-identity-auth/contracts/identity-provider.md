# Contrato da Fronteira de Identidade (Porta `VerificadorDeIdentidade`)

Define o contrato entre o domínio e o provedor de identidade gerenciado, **abstraindo o
fornecedor concreto** (Princípio VII). O adaptador concreto é o Google Identity Platform
via Firebase Admin SDK; o domínio depende **apenas** desta porta.

---

## Porta (interface de domínio)

```java
public interface VerificadorDeIdentidade {
    /**
     * Verifica um ID token emitido pelo provedor e retorna a identidade.
     * @throws IdentidadeInvalidaException   token ausente/malformado/expirado/assinatura inválida
     * @throws ProvedorIndisponivelException falha de rede/indisponibilidade do provedor (FR-013)
     */
    IdentidadeVerificada verify(String idToken);

    /** Revoga as sessões (refresh tokens) da identidade — usado no logout (FR-010). */
    void revogarSessoes(String uid);
}
```

`IdentidadeVerificada` (objeto de valor):

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `uid` | `String` | Identificador estável da identidade no provedor. |
| `email` | `String` | E-mail da identidade. |
| `emailVerificado` | `boolean` | Se o provedor verificou o e-mail. |
| `metodoLogin` | `MetodoLogin` | `SENHA` ou `GOOGLE`. |

---

## Mapeamento dos claims do ID token (Firebase / Identity Platform)

O ID token é um **JWT RS256**. O adaptador valida e mapeia:

| Claim do token | Campo da porta | Validação |
|----------------|----------------|-----------|
| `sub` | `uid` | NOT NULL. |
| `iss` | — | DEVE ser `https://securetoken.google.com/<project-id>`. |
| `aud` | — | DEVE ser `<project-id>`. |
| `exp` / `iat` | — | Token não expirado; `iat` no passado. |
| (assinatura) | — | Verificada contra as chaves públicas do provedor (cacheadas). |
| `email` | `email` | NOT NULL. |
| `email_verified` | `emailVerificado` | Lido como booleano. |
| `firebase.sign_in_provider` | `metodoLogin` | `password` → `SENHA`; `google.com` → `GOOGLE`. |

> O adaptador usa `FirebaseAuth.verifyIdToken(idToken, /*checkRevoked=*/ true)`, que
> executa todas as validações de assinatura/emissor/audiência/expiração **e** rejeita
> tokens cujas sessões foram revogadas (suporte ao logout — R6).

---

## Regras de negócio aplicadas sobre a identidade verificada

| Regra | Requisito | Comportamento |
|-------|-----------|---------------|
| **E-mail não verificado** | FR-015 | Se `metodoLogin = GOOGLE` e `emailVerificado = false`, **recusar** criação de conta (403 `EMAIL_NAO_VERIFICADO`), **sem persistir**. |
| **Vínculo de identidade** | FR-014 | O provedor é configurado para "uma conta por e-mail" e emite o **mesmo `uid`** ao vincular Google a uma conta de e-mail+senha existente (e-mail verificado). O backend chaveia por `uid` (`provider_uid`) e vê uma identidade única. |
| **Unicidade** | FR-008 | Constraints `UNIQUE(provider_uid)` e `UNIQUE(email)`; criação concorrente resolvida por resolve-or-create idempotente. |
| **Identidade não confiável do cliente** | FR-005, I, VII | O `uid`/`email` vêm **sempre** do token verificado, nunca do corpo/query da requisição. |
| **Indisponibilidade do provedor** | FR-013 | `ProvedorIndisponivelException` → 503, sem criar conta inconsistente. |

---

## Configuração (via ambiente — Princípio VIII)

| Propriedade (`megasena.identidade.*`) | Variável de ambiente | Descrição |
|---------------------------------------|----------------------|-----------|
| `project-id` | `IDENTIDADE_PROJECT_ID` | Project id do Identity Platform (emissor/audiência). |
| `credenciais-servico` | `GOOGLE_APPLICATION_CREDENTIALS` | Caminho/where da credencial de serviço do Admin SDK (secret manager em produção). |
| `admins-bootstrap` | `IDENTIDADE_ADMINS_BOOTSTRAP` | Lista de e-mails (CSV) provisionados como ADMINISTRADOR + ATIVO (R5). |

Nenhum segredo é versionado; tudo vem do ambiente.

---

## Testabilidade

Os testes substituem `VerificadorDeIdentidade` por um **dublê** que devolve
`IdentidadeVerificada` controlada (uid/email/emailVerificado/metodoLogin) e registra
chamadas de `revogarSessoes`. Assim, a cadeia de segurança, o provisionamento JIT, o gate
de estado e as regras FR-014/FR-015 são testados **sem rede nem credencial real**,
mantendo a fronteira do provedor fora dos testes de domínio.
