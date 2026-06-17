# Quickstart — Cadastro de Jogos

Como configurar, rodar e verificar a feature 004. Pré-requisitos: features 001
(concursos sincronizados) e 002 (identidade + gate de conta ATIVA) aplicadas.

## 1. Pré-requisitos

- Backend das 001/002 buildando (`backend/`, Java 21, Maven), MySQL acessível.
- Pelo menos **alguns concursos** na tabela `concurso` (001) — para o sistema
  determinar o próximo em aberto (`MAX(numero)+1`). Sem concursos, o cadastro é
  recusado com `SEM_CONCURSO_ABERTO`.
- Um **ID token** de uma conta **ATIVA** (papel USUARIO) — obtido como na 002.

## 2. Migrações

Ao subir, o Flyway aplica `V6__create_jogo.sql` (tabelas `jogo` + `jogo_dezena`) e
`V7__create_evento_jogo.sql`. Não alteram tabelas das 001/002.

```bash
cd backend && ./mvnw flyway:info   # V6 e V7 listadas
```

## 3. Subir o backend

```bash
cd backend && ./mvnw spring-boot:run
```

## 4. Verificação manual (curl)

> `$TOKEN` = ID token de conta ATIVA. `$PENDING` = token de conta PENDENTE.

Cadastrar informando as dezenas (6–9):
```bash
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"dezenas":[4,9,17,23,42,58]}' http://localhost:8080/api/jogos
# 201 + { id, concursoNumero, dezenas, editavel:true, ... }
```

Cadastrar pedindo geração (só a quantidade):
```bash
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"quantidade":8}' http://localhost:8080/api/jogos
# 201 + jogo com 8 dezenas distintas geradas
```

Cadastro parcial é recusado (FR-003):
```bash
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"dezenas":[4,9],"quantidade":6}' http://localhost:8080/api/jogos
# 400 CADASTRO_PARCIAL
```

Aposta inválida é recusada (FR-004):
```bash
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"dezenas":[4,4,9,17,23,99]}' http://localhost:8080/api/jogos
# 400 VALIDACAO (repetição + fora de 1–60)
```

Conta não-ativa é barrada (FR-009):
```bash
curl -i -H "Authorization: Bearer $PENDING" http://localhost:8080/api/jogos
# 403 ACESSO_NEGADO
```

Listar / obter (isolados por dono):
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/jogos
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/jogos/$ID_DE_OUTRO_USUARIO
# 404 NAO_ENCONTRADO (não revela posse alheia)
```

Editar antes do sorteio (troca dezenas/quantidade, mesmo concurso):
```bash
curl -i -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"quantidade":9}' http://localhost:8080/api/jogos/$ID
# 200 (jogo agora com 9 dezenas geradas; concursoNumero inalterado)
```

Editar/excluir após o sorteio é bloqueado (FR-008):
```bash
# (após a 001 ingerir o resultado do concurso vinculado)
curl -i -X DELETE -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/jogos/$ID
# 409 JOGO_BLOQUEADO
```

## 5. Conferir a auditoria

```sql
SELECT usuario_id, jogo_id, tipo, criado_em FROM evento_jogo ORDER BY criado_em DESC;
-- cada cadastro/edição/exclusão gera uma linha (FR-010)
```

## 6. O que validar (mapeado aos critérios de sucesso)

- **SC-001**: cadastrar (informado ou gerado) em < 1 min.
- **SC-002 / FR-006**: jogos só visíveis ao dono; acesso cruzado → 404.
- **SC-003 / FR-003**: cadastro parcial → 400.
- **SC-004 / FR-002**: jogo gerado tem exatamente a quantidade pedida de dezenas distintas 1–60.
- **SC-005 / FR-008**: editar/excluir após o sorteio → 409.
- **SC-006 / FR-004**: contagem fora de 6–9, fora de 1–60 ou com repetição → 400.

## 7. Testes automatizados

```bash
cd backend && ./mvnw test
```

Cobertura obrigatória (Princípio III): validação, regra "tudo ou geração", geração
com **fonte de aleatoriedade determinística**, gate da janela de edição, isolamento
por `usuario_id` e gate de conta ATIVA (com verificador de identidade dublê).
