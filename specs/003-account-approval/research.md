# Phase 0 — Research: Aprovação de Contas (Admin)

Resolve as decisões de desenho da feature 003. Não há `NEEDS CLARIFICATION` no
Technical Context — as questões abertas da spec foram fechadas na sessão de
clarificações (motivo na reprovação, escopo só-pendentes, estados terminais). Esta
pesquisa registra as decisões técnicas que sustentam o plano.

---

## R1 — Resolução de concorrência na transição de estado

**Decisão**: Usar um **UPDATE condicionado ao estado de origem** —
`UPDATE usuario SET estado=?, atualizado_em=? WHERE id=? AND estado='PENDENTE'` — e
tratar **linhas afetadas = 0** como "transição não permitida / já decidida". A
gravação da decisão e o UPDATE ocorrem na **mesma transação**.

**Rationale**: Atende FR-008 (decisões concorrentes não geram estado inconsistente:
a 1ª vence, a 2ª é recusada) sem alterar a tabela `usuario`. É a opção mais simples
(Princípio VI) e atômica no banco — o predicado `estado='PENDENTE'` é a própria
trava. Cobre também FR-005 (só transiciona a partir de PENDENTE) com a mesma
cláusula.

**Alternativas consideradas**:
- **Bloqueio otimista com coluna `@Version`** em `usuario`: exigiria migração para
  alterar a tabela da 002 e ainda recairia em conflito a ser tratado; mais
  cerimônia para o mesmo efeito. Rejeitada por acoplar/alterar o schema da 002 sem
  ganho.
- **`SELECT ... FOR UPDATE` + checagem em código**: bloqueio pessimista
  desnecessário para volume baixo; mais contenção. Rejeitada por YAGNI.

---

## R2 — Modelagem da trilha de auditoria da decisão

**Decisão**: Criar uma **tabela dedicada `decisao_moderacao`** (entidade
`DecisaoModeracao`) com: id, `usuario_id` (conta alvo), `admin_id` (ator),
`decisao` (`APROVADO`/`REPROVADO`), `motivo` (obrigatório quando `REPROVADO`),
`criado_em`. **Não** reutilizar `evento_identidade`.

**Rationale**: A decisão de moderação tem forma própria que não cabe na semântica
de `EventoIdentidade` (eventos de identidade do próprio sujeito: cadastro,
autenticação, logout, acesso negado). A decisão tem **dois atores** (admin e alvo)
e um **motivo de texto** — atributos ausentes naquela tabela. Uma tabela dedicada
mantém cada trilha coesa (Princípio V) e evita sobrecarregar o enum/colunas da 002.

**Alternativas consideradas**:
- **Estender `evento_identidade`** com tipos `APROVACAO`/`REPROVACAO` + coluna de
  ator + alargar `motivo`: misturaria duas auditorias de natureza diferente e
  alteraria o schema da 002. Rejeitada por coesão e por não mexer na 002.
- **Apenas logar (sem tabela)**: viola FR-007/Princípio V (auditoria precisa ser
  consultável e rastreável, não só log volátil). Rejeitada.

---

## R3 — Autorização das ações de moderação

**Decisão**: Mapear todos os endpoints sob `/api/admin/**` à autoridade
**`ROLE_ADMINISTRADOR`** no `SecurityConfig` (única alteração em código da 002). A
identidade e o papel do admin vêm do `UsuarioAutenticado` já populado pelo
`IdentidadeTokenFilter`.

**Rationale**: Reaproveita 100% a cadeia de segurança da 002 (Princípios VII/VIII);
usuário comum recebe 403 (FR-002). Concentra a regra de acesso num único ponto
declarativo, fácil de testar com `spring-security-test`.

**Alternativas consideradas**:
- **`@PreAuthorize` por método**: válido, porém espalha a regra; preferimos o
  mapeamento por rota (`/api/admin/**`) como contrato claro do espaço administrativo.
  Pode complementar pontualmente, mas a rota é a fronteira primária.

---

## R4 — Forma da API de moderação

**Decisão**: Três endpoints REST sob `/api/admin/contas`:
- `GET /api/admin/contas/pendentes` → lista contas PENDENTE (id, email, metodoLogin, criadoEm).
- `POST /api/admin/contas/{id}/aprovar` → PENDENTE → ATIVO (sem corpo).
- `POST /api/admin/contas/{id}/reprovar` → PENDENTE → REPROVADO (corpo `{ motivo }` obrigatório).

Respostas de erro reusam o schema `Erro` da 002 (status, codigo, mensagem).
Transição inválida → **409 Conflict** (`codigo: TRANSICAO_INVALIDA`); motivo ausente
na reprovação → **400** (`codigo: MOTIVO_OBRIGATORIO`); não-admin → **403**;
não-autenticado → **401**.

**Rationale**: Verbos de transição explícitos (`aprovar`/`reprovar`) tornam a
intenção e a auditoria claras, melhor que um `PATCH estado` genérico que abriria
transições não suportadas. 409 comunica corretamente "estado atual não permite"
(FR-005). Mantém o estilo e o modelo de erro da 002 (consistência de terminologia).

**Alternativas consideradas**:
- **`PATCH /api/admin/contas/{id}` com `{estado}`**: genérico demais; permitiria
  pedir transições inválidas e diluiria a semântica de aprovação/reprovação e do
  motivo. Rejeitada.

---

## R5 — Escopo de entrega (backend vs. UI)

**Decisão**: Entregar o **backend** (API + auditoria + segurança). A tela
administrativa Angular (fila + botões) fica como incremento posterior.

**Rationale**: Segue o precedente das features 001 e 002 (backend-first, frontend
adiado) e o Princípio VI (incremental). Os critérios de aceite da spec são
verificáveis pela API (Independent Test de cada user story). Não bloqueia: o admin
pode operar via API até a UI chegar.

**Alternativas consideradas**:
- **Entregar UI junto**: ampliaria o escopo da feature além do necessário para
  validar as regras; adiado sem perda de valor verificável agora.
