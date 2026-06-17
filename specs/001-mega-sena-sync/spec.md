# Feature Specification: Sincronização com a API da Mega Sena

**Feature Branch**: `001-mega-sena-sync`

**Created**: 2026-05-31

**Status**: Draft

**Input**: User description: "Sincronização com a API oficial da Mega Sena para importar e manter atualizados concursos e sorteios, com cache local e sincronização agendada e resiliente."

## Clarifications

### Session 2026-05-31

- Q: Como proteger o disparo manual e a consulta de status da sincronização? → A: A
  consulta de status fica disponível para o perfil **admin**; a sincronização roda de
  forma automática via **cron uma vez ao dia**, com mecanismo de **retry** em caso de
  falha. O disparo manual é uma ação de recuperação restrita ao perfil admin.
- Q: Como tratar a atualização de premiação/ganhadores de um concurso já importado? → A:
  Premiação detalhada (nº de ganhadores, rateio/valor por ganhador) e informações de
  acúmulo estão **fora de escopo**. Cada concurso guarda apenas número, data, dezenas
  sorteadas e **valor do prêmio**. O objetivo do sistema é permitir que usuários gerenciem
  seus próprios jogos e confiram suas apostas automaticamente — não espelhar a gestão da
  Mega Sena.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manter o concurso mais recente atualizado automaticamente (Priority: P1)

Após cada novo sorteio oficial da Mega Sena, o sistema atualiza sozinho os dados do
concurso mais recente — números sorteados, data e valor do prêmio — sem que ninguém
precise acionar nada manualmente.

**Why this priority**: É o coração da feature e a razão de existir dela. Sem dados de
concurso atualizados de forma confiável, nenhuma outra funcionalidade do produto
(conferência de jogos, histórico) tem valor. Entregar só isto já mantém a base de
resultados viva e correta.

**Independent Test**: Disponibilizar (em ambiente de teste/dublê da fonte) um novo
concurso ainda não armazenado e aguardar/forçar a sincronização agendada; verificar que
o concurso passa a existir localmente com dezenas e valor do prêmio corretos, idêntico à
fonte.

**Acceptance Scenarios**:

1. **Given** a fonte oficial publicou um concurso mais novo do que o último armazenado,
   **When** a sincronização agendada executa, **Then** o novo concurso é armazenado
   localmente com número, data, dezenas sorteadas e valor do prêmio.
2. **Given** o concurso mais recente já está armazenado e idêntico à fonte, **When** a
   sincronização executa novamente, **Then** nenhum dado é duplicado nem alterado
   (operação idempotente).
3. **Given** há uma lacuna entre o último concurso armazenado e o atual da fonte,
   **When** a sincronização executa, **Then** os concursos faltantes são importados de
   forma que a sequência fique completa, sem buracos.

---

### User Story 2 - Carga inicial do histórico completo de concursos (Priority: P2)

Em uma primeira execução (ou sob demanda), o sistema importa todos os concursos
passados da Mega Sena, populando a base para que jogos antigos possam ser conferidos e o
histórico fique disponível.

**Why this priority**: Necessário para conferências e relatórios sobre concursos
anteriores, mas não bloqueia o valor central de manter o resultado mais recente em dia
(US1). Pode ser executado uma vez e depois apenas mantido pela US1.

**Independent Test**: Executar a carga histórica contra a fonte (ou dublê com N
concursos) e verificar que a quantidade de concursos armazenados corresponde ao total
esperado, sem lacunas e sem duplicatas, com dezenas e valor do prêmio batendo por
amostragem.

**Acceptance Scenarios**:

1. **Given** a base local está vazia, **When** a carga histórica é executada, **Then**
   todos os concursos já sorteados ficam disponíveis localmente, em sequência contínua.
2. **Given** a carga histórica é interrompida no meio, **When** ela é executada
   novamente, **Then** ela retoma e completa sem recriar nem corromper os concursos já
   importados.

---

### User Story 3 - Visibilidade e operação da sincronização (Priority: P3)

Um **admin** consegue consultar o estado da sincronização (quando ocorreu a última, se
teve sucesso ou falha, qual concurso foi afetado) e, como ação de recuperação, disparar
uma sincronização manual sob demanda quando necessário.

**Why this priority**: Aumenta a confiança operacional e a capacidade de diagnóstico,
mas o valor central (dados atualizados) já é entregue pelas US1/US2 mesmo sem painel de
operação.

**Independent Test**: Provocar uma sincronização (manual e agendada), depois consultar o
status como admin e verificar que ele reflete corretamente horário, resultado
(sucesso/falha) e concursos afetados; simular uma falha da fonte e verificar que o status
mostra a falha.

**Acceptance Scenarios**:

1. **Given** sincronizações já ocorreram, **When** o admin consulta o status, **Then**
   ele vê o horário, o resultado e os concursos afetados da última execução.
2. **Given** o admin precisa de dados imediatos, **When** ele dispara uma sincronização
   manual, **Then** ela executa sem conflitar com a sincronização agendada diária e o
   resultado fica registrado.

---

### Edge Cases

- **Fonte oficial indisponível ou lenta (timeout)**: o sistema continua servindo os
  últimos dados válidos já armazenados, registra a falha e tenta novamente mais tarde,
  sem propagar erro para quem consome resultados.
- **Resposta malformada ou incompleta da fonte**: dados inválidos são rejeitados e
  registrados; os dados já armazenados não são corrompidos nem sobrescritos por lixo.
- **Concurso ainda não sorteado / data futura**: nenhum resultado inexistente é criado.
- **Lacuna de concursos** entre o último armazenado e o atual: os concursos faltantes
  são preenchidos para manter a sequência contínua.
- **Sincronização agendada e manual ao mesmo tempo**: o sistema evita processamento
  duplicado e condição de corrida sobre o mesmo concurso.
- **Republicação/correção de um concurso pela fonte oficial**: tratada como nova
  ingestão rastreável; um concurso já confirmado não é alterado silenciosamente.
- **Falha transitória durante a carga histórica longa**: a carga é retomável, sem
  reimportar tudo do zero.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST obter, da fonte oficial da Mega Sena, os dados de cada
  concurso necessários ao produto: número do concurso, data do sorteio, dezenas sorteadas
  e valor do prêmio. Premiação detalhada e informações de acúmulo estão fora de escopo.
- **FR-002**: O sistema MUST armazenar localmente os dados de cada concurso de forma
  persistente, servindo como fonte de leitura para o restante do produto.
- **FR-003**: O sistema MUST executar a sincronização de forma agendada e automática via
  cron, uma vez ao dia, sem exigir ação de usuários finais. O horário da execução diária
  é configurável.
- **FR-004**: O sistema MUST detectar e importar novos concursos assim que ficarem
  disponíveis na fonte oficial.
- **FR-005**: O sistema MUST suportar a importação do histórico completo de concursos
  passados como carga inicial, de forma retomável.
- **FR-006**: O sistema MUST tratar a sincronização de forma idempotente — reprocessar o
  mesmo concurso não cria duplicatas nem altera dados já confirmados.
- **FR-007**: O sistema MUST continuar servindo os últimos dados válidos armazenados
  quando a fonte oficial estiver indisponível (degradação graciosa).
- **FR-008**: O sistema MUST registrar cada execução de sincronização com horário,
  resultado (sucesso, falha ou parcial), concursos afetados e eventuais mensagens de
  erro, para fins de auditoria e observabilidade.
- **FR-009**: O sistema MUST tentar novamente automaticamente após falhas transitórias
  da fonte, respeitando um limite de tentativas e espaçamento entre elas.
- **FR-010**: O sistema MUST validar a integridade dos dados recebidos antes de
  persistir (ex.: exatamente 6 dezenas distintas entre 1 e 60), rejeitando e registrando
  dados malformados sem corromper os existentes.
- **FR-011**: O perfil **admin** MUST be able to consultar o status da última
  sincronização e disparar uma sincronização manual sob demanda (ação de recuperação);
  essas operações não são expostas a usuários finais comuns.
- **FR-012**: O sistema MUST tratar dados de concursos confirmados como imutáveis;
  qualquer correção ocorre somente via nova ingestão rastreável a partir da fonte
  oficial.
- **FR-013**: O sistema MUST tratar os dados de concursos/sorteios como dados de
  referência globais e somente leitura para os usuários finais — eles são compartilhados
  por todos os usuários e não estão sujeitos ao isolamento por usuário (que se aplica aos
  jogos pessoais, fora do escopo desta feature).
- **FR-014**: O sistema MUST preencher lacunas de concursos (importar os faltantes) para
  garantir uma sequência contínua entre o primeiro concurso conhecido e o mais recente.

### Key Entities *(include if feature involves data)*

- **Concurso**: representa um sorteio oficial da Mega Sena. Atributos: número do concurso,
  data do sorteio, conjunto de dezenas sorteadas (6 números de 1 a 60) e valor do prêmio.
  Premiação detalhada (quantidade de ganhadores, rateio por faixa, valor por ganhador) e
  informações de acúmulo estão fora de escopo. É dado de referência global e imutável após
  confirmado.
- **Execução de Sincronização**: representa uma rodada de sincronização. Atributos:
  horário de início/fim, origem (agendada ou manual), resultado (sucesso, falha,
  parcial), concursos afetados/importados e mensagem de erro quando aplicável. Base para
  a trilha de auditoria e para a visibilidade operacional.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um novo concurso oficial fica disponível no sistema em até 24 horas após
  sua divulgação pela fonte oficial (na próxima execução diária agendada da
  sincronização).
- **SC-002**: Após a carga inicial, 100% dos concursos já sorteados estão disponíveis
  localmente, em sequência contínua e sem lacunas.
- **SC-003**: Durante uma indisponibilidade da fonte oficial, 100% das consultas a
  resultados já sincronizados continuam sendo atendidas, sem erro para quem consome os
  dados.
- **SC-004**: Após reprocessamentos repetidos, há 0 concursos duplicados e 0 alterações
  indevidas em concursos já confirmados.
- **SC-005**: O admin consegue, a qualquer momento, verificar o horário e o resultado da
  última sincronização.
- **SC-006**: Em auditoria por amostragem, 100% dos concursos sincronizados conferem com
  a fonte oficial nas dezenas sorteadas e no valor do prêmio.

## Assumptions

- Existe uma fonte oficial da Mega Sena (Caixa) acessível que fornece, por concurso, ao
  menos número, data, dezenas sorteadas e valor do prêmio. A escolha técnica do
  endpoint/protocolo é decidida no planejamento.
- Sorteios ocorrem em dias determinados da semana; a frequência da sincronização agendada
  é configurada para captar novos resultados pouco após cada sorteio (default razoável,
  ajustável).
- Os dados de concursos são dados de referência compartilhados entre todos os usuários e
  não estão sujeitos ao isolamento por usuário descrito na constituição (que se aplica
  aos jogos pessoais).
- Esta feature cobre ingestão, armazenamento, manutenção e visibilidade operacional dos
  dados de concursos. **Não** cobre o cadastro de jogos do usuário nem a conferência
  automática de jogos — essas são specs separadas que consumirão estes dados.
- **Fora de escopo**: premiação detalhada (quantidade de ganhadores, rateio/valor por
  ganhador) e informações de acúmulo. O propósito do produto é permitir que usuários
  gerenciem seus próprios jogos e confiram suas apostas automaticamente, e não espelhar a
  gestão financeira da Mega Sena.
- A janela de atualização (SC-001) e a profundidade da carga histórica (todos os
  concursos desde o primeiro) usam defaults razoáveis e são ajustáveis sem mudar o
  escopo desta feature.
