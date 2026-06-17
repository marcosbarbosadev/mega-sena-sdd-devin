# Roadmap de Features — Mega Sena Manager

> Cada item vira uma spec via `/speckit.specify` quando chegar a sua vez.
> Referencie a visão com `@draft/vision.md` ao especificar cada feature.

## Versão atual

- [x] 001 Sincronização de concursos — ingere e mantém atualizados número do
  concurso e dezenas sorteadas da fonte oficial; referência global somente
  leitura. **(Concluída — `specs/001-mega-sena-sync/`.)**
- [ ] 002 Identidade & autenticação — integração com provedor de identidade
  gerenciado; auto-cadastro e login por e-mail+senha **ou** conta Google;
  identidade que sustenta o isolamento multiusuário. *Fora:* aprovação de contas
  (003) e cadastro de jogos (004).
- [ ] 003 Aprovação de contas (admin) — conta recém-cadastrada nasce pendente;
  administrador aprova/reprova; ao aprovar, a conta é ativada (define/troca
  credenciais no caso e-mail+senha, ou apenas ativa se via Google). *Fora:* login
  em si (002) e qualquer gestão de jogos.
- [ ] 004 Cadastro de jogos — criar/editar/excluir apostas de 6, 7, 8 ou 9 dezenas
  (1–60), informando todas as dezenas ou pedindo geração automática; vínculo ao
  próximo concurso em aberto; isolado por usuário. *Fora:* conferência (005).
- [ ] 005 Conferência automática — ao haver resultado do concurso vinculado,
  comparar cada jogo do usuário e exibir acertos, faixa (sena/quina/quadra) e
  indicador de premiado (≥4 acertos), sem valores monetários. *Fora:* notificações
  e premiação detalhada.

## Futuro

> Com a antecipação do login Google e da aprovação por administrador para a versão
> atual, não há features adiadas comprometidas no momento. Esta seção fica como
> espaço para itens que surgirem na evolução (ex.: histórico/estatísticas de
> conferência, notificações de resultado) — nenhum deles definido ainda.

## Notas de dependência

- **002** é fundação: estabelece a identidade exigida pelo isolamento
  multiusuário de 003, 004 e 005.
- **003** depende de **002** (atua sobre contas autenticáveis).
- **004** depende de **002** (jogo pertence a um usuário) e de **001** (determinar
  o próximo concurso em aberto).
- **005** depende de **004** (o que conferir) e de **001** (resultado para
  conferir).
- A escolha do provedor de identidade e o design cloud-native são **princípios**
  (ver `@draft/vision.md` → Princípios candidatos) e devem ser fixados na
  constituição via `/speckit.constitution` antes/junto da spec da **002**.
