# Visão Geral — Mega Sena Manager

## Propósito
Aplicativo multiusuário para gerenciar jogos da Mega Sena: o usuário cadastra
suas apostas e o sistema as confere automaticamente contra o resultado oficial
do concurso, mantendo isolamento total entre os jogos de cada usuário.

## Público
Apostadores da Mega Sena que querem organizar seus jogos em um só lugar e saber,
sem conferência manual, quantas dezenas acertaram em cada concurso. Há também o
papel de **administrador**, responsável por aprovar quem pode usar o sistema.

## Visão funcional
O sistema sincroniza os resultados dos concursos direto da fonte oficial
(somente número do concurso e dezenas sorteadas). Usuários autenticam-se por um
**provedor de identidade gerenciado** (e-mail+senha ou conta Google) e, após
aprovação de um administrador, passam a cadastrar jogos de 6 a 9 dezenas —
informando todas as dezenas ou deixando o sistema gerá-las. Cada jogo é vinculado
ao próximo concurso em aberto e, quando esse concurso é sorteado, o sistema
confere o jogo e informa os acertos, a faixa (sena/quina/quadra) e se foi
premiado. Os dados de cada usuário são isolados dos demais.

## Módulos
- **Sincronização de concursos** — ingere e mantém atualizados, da fonte oficial,
  o número do concurso e as dezenas sorteadas; referência global somente leitura.
  *(Já implementado — feature 001.)*
- **Identidade & autenticação** — integração com provedor de identidade
  gerenciado; auto-cadastro e login por e-mail+senha ou conta Google; isolamento
  por usuário a partir da identidade do provedor.
- **Aprovação de contas (admin)** — o usuário se cadastra e fica pendente; só após
  a confirmação de um administrador a conta é ativada e ganha acesso (definição/
  troca de credenciais para e-mail+senha, ou apenas ativação se entrou via Google).
- **Cadastro de jogos** — criação de apostas com 6, 7, 8 ou 9 dezenas (1–60),
  informando todas as dezenas ou pedindo geração automática; vínculo ao próximo
  concurso em aberto.
- **Conferência automática** — compara os jogos do usuário com as dezenas do
  concurso e exibe quantidade de acertos, faixa e indicador de premiado.

## Relações entre módulos
- **Identidade & autenticação** é a fundação: estabelece a identidade que sustenta
  o **isolamento multiusuário** de todos os demais módulos de usuário.
- **Aprovação de contas** depende de **Identidade & autenticação** (atua sobre as
  contas recém-cadastradas, controlando quem recebe acesso).
- **Cadastro de jogos** depende de **Identidade & autenticação** (jogo pertence a
  um usuário) e de **Sincronização de concursos** (para determinar o próximo
  concurso em aberto).
- **Conferência automática** depende de **Cadastro de jogos** (o que conferir) e de
  **Sincronização de concursos** (o resultado contra o qual conferir).

## Concerns transversais
- **Autenticação/identidade**: delegada a um provedor gerenciado (ex.: AWS Cognito
  ou Google Identity Platform); o backend confia na identidade emitida pelo
  provedor. Provedor específico ainda a definir (ver Princípios candidatos).
- **Isolamento multiusuário**: total entre os jogos de cada usuário — princípio
  não-negociável já presente na constituição.
- **Persistência**: MySQL versionado com Flyway (constituição).
- **Integração externa**: fonte oficial dos concursos (já coberta pela feature 001,
  com retry/timeout, idempotência e cache do último dado válido).
- **Frontend**: Angular LTS (constituição) — introduzido pelas features com UI.
- **Cloud-native**: o sistema é desenhado para rodar em ambiente cloud.

## Princípios candidatos (mover para a constituição)
> Decisões arquiteturais que pertencem ao `/speckit.constitution`, não a features.
- **Identidade gerenciada externamente** — autenticação e ciclo de vida de
  credenciais delegados a um provedor de identidade gerenciado (Cognito / Google
  Identity Platform); provedor específico a decidir.
- **Design cloud-native** — o sistema deve ser projetado para operar em ambiente
  cloud.
- **Acesso somente após aprovação** — nenhuma conta recém-cadastrada obtém acesso
  sem confirmação explícita de um administrador (porta de segurança).

## Fora de escopo
- Valores de prêmio, premiação detalhada e acúmulo (a fonte usada traz apenas
  número do concurso e dezenas sorteadas).
- Apostas com seleção parcial de dezenas: ou o usuário informa **todas** as
  dezenas, ou não informa nenhuma e o sistema gera — nunca uma parte.
- Cadastro manual de usuários pelo administrador (substituído pelo fluxo de
  auto-cadastro + aprovação).
