# Implementation Plan: Identidade & Autenticação

**Branch**: `002-identity-auth` | **Date**: 2026-06-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-identity-auth/spec.md`

## Summary

Estabelecer a fundação de identidade do sistema: delegar autenticação e guarda de
credenciais a um provedor gerenciado (**Google Identity Platform / Firebase Auth**),
validar a identidade no servidor a cada requisição e materializar localmente uma **conta
de usuário** com papel (usuário/administrador) e estado (pendente/ativo/reprovado). A
conta nasce **pendente** (sem acesso a recursos de usuário) e só uma conta **ativa**
passa pelo portão de acesso (Princípio IX). O identificador local da conta é a chave de
isolamento (Princípio I) à qual as features seguintes (jogos, conferências) vão se
vincular. A fronteira de identidade é **abstraída por uma porta** (`VerificadorDe
Identidade`), de modo que o provedor concreto não vaza para as regras de negócio
(Princípio VII). Esta feature entrega o **backend**; a UI de login é delegada ao SDK do
provedor e a integração Angular é um incremento posterior.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.3.x (Web, Data JPA, Validation, Security),
Flyway (+ flyway-mysql), MySQL Connector/J, **Firebase Admin SDK**
(`com.google.firebase:firebase-admin`) como adaptador concreto da porta de identidade
(verificação de ID token: assinatura, emissor, audiência, expiração; revogação de
refresh token no logout). Build: Maven. Testes: JUnit 5, Mockito, Testcontainers
(MySQL), `spring-security-test`. A verificação de identidade é abstraída por uma porta,
então os testes usam um **verificador dublê** (sem depender da rede do provedor).

**Storage**: MySQL (schema versionado com Flyway; novas migrações `V3__`, `V4__`
seguindo as `V1`/`V2` da feature 001)

**Testing**: JUnit 5 + Mockito (unidade: gate de estado, provisionamento JIT, regras
FR-014/FR-015); Testcontainers MySQL (integração de persistência e isolamento);
verificador de identidade dublê nos testes de integração da cadeia de segurança

**Target Platform**: Servidor Linux (containerizado)

**Project Type**: Web application (monorepo). Esta feature implementa o **backend**
(`backend/`). A obtenção de token (login e-mail+senha / Google) é responsabilidade do
SDK do provedor no cliente; a UI Angular é introduzida como incremento posterior
(precedente da feature 001, alinhado ao crescimento incremental do Princípio VI).

**Performance Goals**: Verificação de ID token por requisição em poucos milissegundos
após o warm-up (chaves públicas do provedor são cacheadas pelo SDK); resolução da conta
é consulta indexada por `provider_uid`. Auto-cadastro completável em < 2 min (SC-001);
chegada à área protegida em poucos segundos (SC-005).

**Constraints**: Stateless no processo (sem sessão em memória; identidade derivada do
token a cada requisição — Princípios VII e VIII); nenhuma senha persistida pela
aplicação (SC-004); o estado da conta é reforçado no servidor em toda requisição
(Princípio IX); mensagens de erro de autenticação não revelam existência de conta
(SC-006). Configuração e segredos vêm do ambiente (credencial de serviço, project id).

**Scale/Scope**: Baixo volume (cadastro/login esporádicos por usuário). Uma tabela de
contas + uma de eventos de auditoria. Sem necessidade de cache distribuído nesta feature.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Aderência neste plano |
|-----------|----------------------|
| I. Isolamento Multiusuário (NÃO-NEGOCIÁVEL) | `usuario.id` (UUID local) é a **chave de isolamento** derivada do token verificado; nunca de ID do cliente. As features seguintes farão FK a ele. Esta feature não expõe dados por-usuário ainda, mas fixa o eixo de isolamento e o gate de identidade. ✅ |
| II. Integridade dos Dados de Sorteio | Não aplicável (feature não toca em concursos/conferência). — |
| III. Qualidade via Testes Pragmáticos | Cobertura obrigatória: derivação da identidade a partir do token, gate de estado (pendente/ativo/reprovado), provisionamento JIT, unicidade (FR-008), regras de e-mail verificado/vínculo (FR-014/FR-015) e isolamento. Verificador dublê isola os testes do provedor. ✅ |
| IV. Resiliência da Integração | Indisponibilidade do provedor degrada com mensagem clara, sem criar contas inconsistentes (FR-013); verificação de token tolera falha de rede do provedor sem 500 opaco. ✅ |
| V. Observabilidade & Auditoria | Tabela `evento_identidade` registra cadastro/autenticação/logout/acesso-negado (quem, o quê, quando) sem credenciais nem PII sensível em texto claro; logging estruturado correlacionado por requisição. ✅ |
| VI. Simplicidade & YAGNI | Provisionamento **just-in-time** (sem endpoint de registro separado); porta única de identidade com um adaptador; sem cache distribuído; frontend adiado. ✅ |
| VII. Identidade Gerenciada Externamente | Autenticação delegada ao Google Identity Platform; **nenhuma senha** na aplicação; todo token tem assinatura/emissor/audiência/expiração validados no servidor a cada requisição; `user_id` derivado do token; fronteira abstraída pela porta `VerificadorDeIdentidade`. ✅ |
| VIII. Design Cloud-Native | Credencial de serviço e project id via ambiente/secret manager; processo stateless (sem sessão local); containerizável; provedor tratado como serviço de rede sujeito a falha. ✅ |
| IX. Acesso Somente Após Aprovação | Conta nasce **PENDENTE** sem acesso; só **ATIVO** passa o gate; **REPROVADO** suportado; estado reforçado no servidor em cada requisição; transições auditadas. A UI de aprovação é a feature 003; aqui o admin inicial é provisionado fora de banda por configuração. ✅ |

**Resultado**: PASS — nenhuma violação. Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/002-identity-auth/
├── plan.md              # Este arquivo (/speckit-plan)
├── research.md          # Fase 0 — decisões de pesquisa (provedor, verificação, vínculo)
├── data-model.md        # Fase 1 — entidades, estados e regras
├── quickstart.md        # Fase 1 — como configurar/rodar/verificar (com emulador)
├── contracts/           # Fase 1 — contratos de interface
│   ├── auth-api.yaml         # API REST própria (/api/auth/me, /api/auth/logout)
│   └── identity-provider.md  # Contrato da fronteira de identidade (claims do token, porta)
├── checklists/
│   └── requirements.md       # Checklist de qualidade da spec (já existente)
└── tasks.md             # Fase 2 — gerado por /speckit-tasks (NÃO criado aqui)
```

### Source Code (repository root)

```text
backend/
├── pom.xml                     # + dependência firebase-admin
└── src/
    ├── main/
    │   ├── java/com/megasena/sync/
    │   │   ├── identidade/             # NÚCLEO desta feature
    │   │   │   ├── Usuario.java               # entidade conta (id, provider_uid, email, papel, estado)
    │   │   │   ├── Papel.java                 # enum USUARIO/ADMINISTRADOR
    │   │   │   ├── EstadoConta.java           # enum PENDENTE/ATIVO/REPROVADO
    │   │   │   ├── UsuarioRepository.java
    │   │   │   ├── ProvisionamentoService.java     # JIT: cria/resolve conta a partir da identidade verificada
    │   │   │   ├── ContaController.java            # GET /api/auth/me, POST /api/auth/logout
    │   │   │   ├── ContaResponse.java
    │   │   │   ├── EventoIdentidade.java           # auditoria
    │   │   │   ├── EventoIdentidadeRepository.java
    │   │   │   └── AuditoriaIdentidadeService.java
    │   │   ├── identidade/provedor/        # PORTA + adaptador (fronteira abstraída — Princípio VII)
    │   │   │   ├── VerificadorDeIdentidade.java    # porta: verify(token) -> IdentidadeVerificada
    │   │   │   ├── IdentidadeVerificada.java       # uid, email, emailVerificado, metodoLogin
    │   │   │   ├── IdentidadeInvalidaException.java
    │   │   │   ├── ProvedorIndisponivelException.java
    │   │   │   └── firebase/
    │   │   │       └── FirebaseVerificadorDeIdentidade.java  # adaptador Firebase Admin SDK
    │   │   └── config/
    │   │       ├── SecurityConfig.java             # ATUALIZADO: filtro de identidade no lugar do AdminTokenFilter
    │   │       ├── IdentidadeTokenFilter.java      # extrai Bearer, verifica, resolve conta, aplica gate de estado
    │   │       ├── FirebaseConfig.java             # inicializa FirebaseApp a partir de credencial do ambiente
    │   │       ├── IdentidadeProperties.java       # project-id, admins-bootstrap, etc.
    │   │       └── UsuarioAutenticado.java         # principal: usuarioId, papel, estado
    │   └── resources/
    │       ├── application.yml             # + bloco megasena.identidade.*
    │       └── db/migration/
    │           ├── V3__create_usuario.sql
    │           └── V4__create_evento_identidade.sql
    └── test/
        └── java/com/megasena/sync/
            ├── identidade/             # provisionamento JIT, gate de estado, FR-014/FR-015, unicidade
            ├── config/                 # cadeia de segurança com verificador dublê (autenticado/negado)
            └── support/                # verificador de identidade dublê + base de teste de identidade

frontend/                              # (adiado) Angular LTS — integra Firebase SDK + Bearer em incremento posterior
```

**Structure Decision**: Mantém o monorepo e o pacote raiz `com.megasena.sync` da feature
001. A feature introduz o contexto **`identidade`** (núcleo de domínio) e
**`identidade.provedor`** (porta + adaptador Firebase), e **substitui** o
`AdminTokenFilter` estático da 001 por autenticação baseada na identidade verificada do
provedor — o papel ADMINISTRADOR passa a vir da conta, não de um token de serviço. A
separação porta/adaptador isola o Firebase atrás de uma interface, honrando o Princípio
VII e permitindo testes sem rede.

## Complexity Tracking

> Sem violações da Constitution Check — nenhuma justificativa de complexidade necessária.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
