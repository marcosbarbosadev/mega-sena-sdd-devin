# Descrição original

Preciso criar um aplicativo para gerenciar jogos da mega sena.
O sistema deve ser multiusuário e deve ser capaz de manter isolamento total entre os jogos de cada usuário.
O sistema também deve atualizar informações de concursos e sorteios, conectando diretamente na API de mega sena. Os dados que serão utilizados são apenas o número do concurso e resultado das dezenas sorteads.

Funcionalidades que o sistema deve oferecer:
- cadastro de apostas com 6, 7, 8 ou 9 números. O usuário marca a quantidade de números que deseja e seleciona os números em uma lista de 1 a 60.
- O sistema deve permitir o cadastro informando todos os números ou informando a quantidade de números e deixando o sistema gerar.
- A área de administrador deve permitir cadastrar usuários manualmente

Restrições:
- o usuário deve informar todos os números ou não informar e deixar o sistema gerar.
- não há opção de informar parte dos números

A ideia é que o usuário possa:
- cadastrar seus jogos
- conferir jogos automaticamente com base nos jogos cadastrados e resultado do concuros que ele fez as apostas.


Funcionalidades que deverão ser implementadas no futuro:
- Login via google
  - Confirmação da conta por um usuário administrador para evitar que qualquer pessoa faça cadastro livremente
