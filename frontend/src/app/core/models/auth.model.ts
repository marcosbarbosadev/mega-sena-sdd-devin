export interface ContaResponse {
  id: string;
  email: string;
  papel: 'USUARIO' | 'ADMINISTRADOR';
  estado: 'PENDENTE' | 'ATIVO' | 'REPROVADO';
  metodoLogin: 'EMAIL_SENHA' | 'GOOGLE';
  ultimoAcessoEm: string;
}

export interface ApiError {
  status: number;
  codigo: string;
  mensagem: string;
}
