export interface JogoResponse {
  id: string;
  concursoNumero: number;
  tipoSelecao: 'MANUAL' | 'AUTOMATICO';
  dezenas: number[];
  criadoEm: string;
  atualizadoEm: string;
  editavel?: boolean;
}

export interface JogoRequest {
  dezenas?: number[];
  quantidade?: number;
  concursoNumero?: number;
}
