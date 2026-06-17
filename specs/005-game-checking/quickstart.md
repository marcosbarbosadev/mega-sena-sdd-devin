# Quickstart — Conferência Automática de Jogos

Como configurar, rodar e verificar a feature 005. Pré-requisitos: 001 (concursos +
dezenas), 002 (identidade/gate) e 004 (jogos) aplicadas.

## 1. Pré-requisitos

- Backend das 001/002/004 buildando; MySQL acessível.
- Conta **ATIVA** com um **ID token** (papel USUARIO), como na 002.
- Pelo menos um **jogo** do usuário (004) e o respectivo **concurso sorteado** na
  base da 001 (para conferir) — e idealmente um jogo cujo concurso ainda **não**
  foi sorteado (para ver `AGUARDANDO_SORTEIO`).

## 2. Migração

Ao subir, o Flyway aplica `V8__create_evento_conferencia.sql` (auditoria). Não
altera tabelas das features anteriores.

```bash
cd backend && ./mvnw flyway:info   # V8 listada
```

> Ordem de migração: `V8` pressupõe `V5` (003) e `V6/V7` (004). Garantir merge
> 003 → 004 → 005 (ou `spring.flyway.out-of-order=true`).

## 3. Subir o backend

```bash
cd backend && ./mvnw spring-boot:run
```

## 4. Verificação manual (curl)

> `$TOKEN` = ID token de conta ATIVA. `$JOGO` = id de um jogo do usuário.

Conferir um jogo cujo concurso foi sorteado:
```bash
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/jogos/$JOGO/conferencia
# 200 { status: CONFERIDO, acertos, faixa, premiado }
```

Conferir um jogo cujo concurso ainda não foi sorteado:
```bash
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/jogos/$JOGO_FUTURO/conferencia
# 200 { status: AGUARDANDO_SORTEIO }
```

Listar a conferência de todos os meus jogos:
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/conferencias
# 200 [ { ...CONFERIDO... }, { ...AGUARDANDO_SORTEIO... } ]
```

Jogo de outro usuário é negado (isolamento):
```bash
curl -i -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/jogos/$JOGO_DE_OUTRO/conferencia
# 404 NAO_ENCONTRADO
```

Conta não-ativa é barrada:
```bash
curl -i -H "Authorization: Bearer $PENDING" http://localhost:8080/api/conferencias
# 403 ACESSO_NEGADO
```

## 5. Conferir a auditoria (idempotência)

```bash
# Conferir o mesmo jogo duas vezes:
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/jogos/$JOGO/conferencia >/dev/null
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/jogos/$JOGO/conferencia >/dev/null
```
```sql
SELECT jogo_id, concurso_numero, acertos, faixa, premiado, criado_em
FROM evento_conferencia WHERE jogo_id = UNHEX(REPLACE('<uuid>','-',''));
-- exatamente UMA linha por jogo×concurso (FR-011), mesmo após repetir a conferência
```

## 6. O que validar (mapeado aos critérios de sucesso)

- **SC-001 / FR-002**: acertos = interseção exata com as 6 dezenas sorteadas (inclui jogos de 7–9 dezenas).
- **SC-002 / FR-006**: conferir o mesmo jogo repetidas vezes retorna sempre o mesmo resultado e **não** cria nova linha de auditoria.
- **SC-003 / FR-003,004**: faixa SENA/QUINA/QUADRA/NENHUMA e premiado ⇔ acertos≥4.
- **SC-004 / FR-005**: jogo de concurso não sorteado → AGUARDANDO_SORTEIO (nunca resultado).
- **SC-005 / FR-007**: jogo de outro usuário → 404; isolamento total.

## 7. Testes automatizados

```bash
cd backend && ./mvnw test
```

Cobertura **obrigatória** (Princípios II/III): cálculo de acertos/faixa/premiado
(6, 7, 8, 9 dezenas; faixas e limiar de premiado), idempotência (resultado + uma
única linha de auditoria), estado AGUARDANDO_SORTEIO, isolamento por `usuario_id` e
gate de conta ATIVA (verificador de identidade dublê).
