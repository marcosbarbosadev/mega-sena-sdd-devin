# Modelo de Dados - Backend Completo

## Visão Geral

O modelo de dados do Mega Sena Manager é dividido em dois contextos principais:
1. **Dados Globais (Somente Leitura)**: Concursos da Mega Sena (compartilhados por todos)
2. **Dados Isolados por Usuário**: Contas, jogos e auditoria (isolamento multiusuário)

## Diagrama Entidade-Relacionamento

```
┌─────────────────┐       ┌──────────────────┐
│   concurso      │       │    usuario       │
├─────────────────┤       ├──────────────────┤
│ id (PK)         │       │ id (PK)          │
│ numero          │       │ firebase_uid     │
│ data_sorteio    │       │ email            │
│ valor_premio    │       │ papel            │
└────────┬────────┘       │ estado           │
         │                │ criado_em        │
         │                └────────┬─────────┘
         │                         │
┌────────┴────────┐       ┌────────┴─────────┐
│ concurso_dezena  │       │ evento_identidade│
├─────────────────┤       ├──────────────────┤
│ concurso_id (FK)│       │ id (PK)          │
│ dezena          │       │ usuario_id (FK)  │
└─────────────────┘       │ tipo_evento      │
                          │ momento          │
                          └──────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
          ┌─────────┴─────────┐         ┌─────────┴─────────┐
          │ decisao_moderacao │         │      jogo          │
          ├───────────────────┤         ├───────────────────┤
          │ id (PK)          │         │ id (PK)           │
          │ conta_id (FK)    │         │ usuario_id (FK)   │
          │ admin_id (FK)    │         │ concurso_numero   │
          │ decisao          │         │ tipo_selecao      │
          │ motivo           │         │ criado_em         │
          │ momento          │         │ atualizado_em     │
          └───────────────────┘         └────────┬──────────┘
                                               │
                                    ┌──────────┴──────────┐
                                    │    jogo_dezena      │
                                    ├────────────────────┤
                                    │ jogo_id (FK)        │
                                    │ dezena              │
                                    └────────────────────┘
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    │                          │                          │
          ┌─────────┴─────────┐    ┌──────────┴──────────┐    ┌───────────┴──────────┐
          │   evento_jogo     │    │ evento_conferencia  │    │      sync_run        │
          ├───────────────────┤    ├────────────────────┤    ├─────────────────────┤
          │ id (PK)           │    │ id (PK)             │    │ id (PK)             │
          │ jogo_id (FK)      │    │ jogo_id (FK)        │    │ inicio               │
          │ tipo_evento       │    │ usuario_id (FK)     │    │ fim                  │
          │ momento           │    │ acertos             │    │ origem               │
          │                   │    │ faixa               │    │ resultado            │
          │                   │    │ premiado            │    │ concursos_afetados   │
          │                   │    │ momento             │    │ mensagem_erro       │
          │                   │    │                     │    │                     │
          └───────────────────┘    └────────────────────┘    └─────────────────────┘
```

## Tabelas Detalhadas

### 1. concurso

Dados de concursos da Mega Sena (global, somente leitura).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGINT PK | Identificador interno |
| numero | INT UNIQUE | Número oficial do concurso |
| data_sorteio | DATE NOT NULL | Data do sorteio |
| valor_premio | DECIMAL(15,2) | Valor do prêmio (pode ser null) |

**Índices**:
- `idx_numero` em `numero` (único)

**Características**:
- Dado de referência global (compartilhado por todos)
- Imutável após confirmação
- Inserido via sincronização, nunca editado manualmente

---

### 2. concurso_dezena

Dezenas sorteadas de cada concurso (1:N com concurso).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| concurso_id | BIGINT FK | Referência ao concurso |
| dezena | INT | Dezena sorteada (1-60) |

**PK Composta**: `(concurso_id, dezena)`

**Características**:
- Exatamente 6 dezenas por concurso
- Dezenas distintas entre 1 e 60
- Ordenadas para consistência

---

### 3. usuario

Contas de usuários do sistema.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGINT PK | Identificador interno |
| firebase_uid | VARCHAR(255) UNIQUE | Identificador do Firebase |
| email | VARCHAR(255) NOT NULL | E-mail do usuário |
| papel | ENUM('USUARIO', 'ADMIN') NOT NULL | Papel no sistema |
| estado | ENUM('PENDENTE', 'ATIVO', 'REPROVADO') NOT NULL | Estado da conta |
| criado_em | TIMESTAMP NOT NULL | Momento de criação |
| ultimo_acesso | TIMESTAMP | Último acesso bem-sucedido |

**Índices**:
- `idx_firebase_uid` em `firebase_uid` (único)
- `idx_email` em `email` (único)
- `idx_estado` em `estado`

**Características**:
- Uma conta por e-mail/identidade
- Estado controla acesso (gate de segurança)
- Papel define permissões (admin vs usuário)

---

### 4. evento_identidade

Auditoria de eventos de identidade (cadastro, autenticação, logout).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGINT PK | Identificador interno |
| usuario_id | BIGINT FK | Referência ao usuário |
| tipo_evento | ENUM('CADASTRO', 'AUTH_SUCCESS', 'AUTH_FAILED', 'LOGOUT') | Tipo de evento |
| metodo_login | ENUM('EMAIL_SENHA', 'GOOGLE') | Método usado |
| momento | TIMESTAMP NOT NULL | Momento do evento |
| ip_address | VARCHAR(45) | IP de origem (opcional) |
| user_agent | VARCHAR(255) | User agent (opcional) |

**Índices**:
- `idx_usuario_id` em `usuario_id`
- `idx_momento` em `momento`

**Características**:
- Trilha de auditoria obrigatória (Princípio V)
- Não expõe credenciais em texto claro
- Preservado mesmo após exclusão de conta

---

### 5. decisao_moderacao

Decisões de aprovação/reprovação de contas por administradores.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGINT PK | Identificador interno |
| conta_id | BIGINT FK | Conta decidida |
| admin_id | BIGINT FK | Administrador que decidiu |
| decisao | ENUM('APROVACAO', 'REPROVACAO') NOT NULL | Decisão tomada |
| motivo | TEXT | Motivo (obrigatório na reprovação) |
| momento | TIMESTAMP NOT NULL | Momento da decisão |

**Índices**:
- `idx_conta_id` em `conta_id`
- `idx_admin_id` em `admin_id`
- `idx_momento` em `momento`

**Características**:
- Motivo obrigatório apenas na reprovação
- Permite rastreabilidade completa (quem decidiu o quê quando)
- Preservado para auditoria

---

### 6. jogo

Apostas/jogos dos usuários (isolados por usuário).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGINT PK | Identificador interno |
| usuario_id | BIGINT FK | Dono do jogo (chave de isolamento) |
| concurso_numero | INT NOT NULL | Concurso vinculado (snapshot imutável) |
| tipo_selecao | ENUM('INFORMADO', 'GERADO') NOT NULL | Origem das dezenas |
| criado_em | TIMESTAMP NOT NULL | Momento de criação |
| atualizado_em | TIMESTAMP | Momento da última atualização |

**Índices**:
- `idx_usuario_id` em `usuario_id` (isolamento)
- `idx_concurso_numero` em `concurso_numero`
- `idx_criado_em` em `criado_em`

**Características**:
- Pertence exclusivamente ao usuário (isolamento multiusuário)
- `concurso_numero` é fixado no cadastro (imutável)
- Editável apenas enquanto concurso não sorteado

---

### 7. jogo_dezena

Dezenas de cada jogo (1:N com jogo).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| jogo_id | BIGINT FK | Referência ao jogo |
| dezena | INT | Dezena apostada (1-60) |

**PK Composta**: `(jogo_id, dezena)`

**Características**:
- 6 a 9 dezenas por jogo
- Dezenas distintas entre 1 e 60
- Ordenadas para consistência

---

### 8. evento_jogo

Auditoria de operações de jogos (cadastro, edição, exclusão).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGINT PK | Identificador interno |
| jogo_id | BIGINT FK | Jogo afetado |
| usuario_id | BIGINT FK | Usuário que realizou a operação |
| tipo_evento | ENUM('CADASTRO', 'EDICAO', 'EXCLUSAO') | Tipo de operação |
| momento | TIMESTAMP NOT NULL | Momento da operação |
| detalhes | JSON | Detalhes adicionais (opcional) |

**Índices**:
- `idx_jogo_id` em `jogo_id`
- `idx_usuario_id` em `usuario_id`
- `idx_momento` em `momento`

**Características**:
- Trilha de auditoria obrigatória
- Permite reconstruir histórico de operações

---

### 9. evento_conferencia

Auditoria de conferências de jogos realizadas.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGINT PK | Identificador interno |
| jogo_id | BIGINT FK | Jogo conferido |
| usuario_id | BIGINT FK | Usuário que consultou |
| acertos | INT NOT NULL | Número de acertos (0-6) |
| faixa | ENUM('SENA', 'QUINA', 'QUADRA', 'NENHUMA') | Faixa de prêmio |
| premiado | BOOLEAN NOT NULL | Indica se foi premiado (≥4 acertos) |
| momento | TIMESTAMP NOT NULL | Momento da conferência |

**Índices**:
- `idx_jogo_id` em `jogo_id`
- `idx_usuario_concurso` em `(usuario_id, jogo_id)` (único para idempotência)
- `idx_momento` em `momento`

**Características**:
- Único por jogo×concurso (idempotente)
- Resultado computado sob demanda (não materializado)
- Auditoria preserva histórico de consultas

---

### 10. sync_run

Execuções de sincronização de concursos.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGINT PK | Identificador interno |
| inicio | TIMESTAMP NOT NULL | Início da execução |
| fim | TIMESTAMP | Fim da execução |
| origem | ENUM('AGENDADA', 'MANUAL') NOT NULL | Origem da execução |
| resultado | ENUM('SUCESSO', 'FALHA', 'PARCIAL') NOT NULL | Resultado |
| concursos_afetados | INT | Quantidade de concursos importados |
| mensagem_erro | TEXT | Mensagem de erro (se aplicável) |

**Índices**:
- `idx_inicio` em `inicio`
- `idx_origem` em `origem`

**Características**:
- Permite visibilidade operacional
- Base para retry automático em falhas

---

## Relações e Isolamento

### Isolamento Multiusuário

- **usuario.id** é a chave de isolamento
- **jogo.usuario_id** garante que jogos pertencem ao dono
- Queries sempre filtram por `usuario_id` derivado da sessão
- Identidade derivada no servidor, nunca confia em cliente

### Dados Globais vs. Isolados

**Globais (compartilhados)**:
- concurso, concurso_dezena (dados de referência)
- sync_run (operacional)

**Isolados (por usuário)**:
- usuario (embora a tabela seja global, o acesso é isolado)
- jogo, jogo_dezena (pertencem ao usuário)
- evento_identidade (trilha do usuário)
- evento_jogo (trilha do usuário)
- evento_conferencia (trilha do usuário)
- decisao_moderacao (acessível apenas a admins)

## Integridade de Dados

### Constraints

- **UNIQUE**: firebase_uid, email (usuario); numero (concurso)
- **FOREIGN KEY**: Todas as relações FK
- **CHECK**: Dezenas entre 1-60, quantidade 6-9
- **NOT NULL**: Campos obrigatórios marcados

### Validações de Aplicação

- **Concurso**: Exatamente 6 dezenas distintas
- **Jogo**: 6-9 dezenas distintas, todas entre 1-60
- **Conta**: E-mail válido, estado transitions válidas
- **Idempotência**: Sincronização, auditoria de conferência

## Migrações Flyway

As migrações versionam o schema:
- V1__create_initial_schema.sql
- V2__add_auditoria_tables.sql
- V3__add_conferencia_tables.sql
- (etc.)

Cada mudança de schema requer nova migração.

## Performance

### Índices Estratégicos

- Lookup por firebase_uid (autenticação)
- Lookup por usuario_id (isolamento)
- Lookup por concurso_numero (jogos e conferência)
- Índices de tempo para auditoria e logs

### Consultas Otimizadas

- JOIN de jogo + jogo_dezena (eager fetch)
- Contagem de jogos por usuário (indexed)
- Filter por estado de conta (indexed)