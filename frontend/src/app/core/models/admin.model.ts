export interface ContaPendenteResponse {
  id: string;
  email: string;
  metodoLogin: 'EMAIL_SENHA' | 'GOOGLE';
  criadoEm: string;
}

export interface ReprovacaoRequest {
  motivo: string;
}

export interface SyncRunResponse {
  id: number;
  origem: 'AGENDADA' | 'MANUAL';
  status: string;
  iniciadoEm: string;
  finalizadoEm: string | null;
  concursosAfetados: number;
  mensagemErro: string | null;
}
