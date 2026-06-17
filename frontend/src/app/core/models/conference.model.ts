export interface ConferenciaResponse {
  jogoId: string;
  concursoNumero: number;
  dezenasJogadas: number[];
  dezenasSorteadas: number[];
  acertos: number;
  faixa: 'SENA' | 'QUINA' | 'QUADRA' | 'NENHUMA';
  premiado: boolean;
  status: 'CONFERIDO' | 'AGUARDANDO_SORTEIO';
}
