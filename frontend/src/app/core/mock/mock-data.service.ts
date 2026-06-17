import { Injectable } from '@angular/core';
import { ContaResponse, ContaPendenteResponse, JogoResponse, ConferenciaResponse, ConcursoResponse, SyncRunResponse } from '../models';

interface MockAccount {
  profile: ContaResponse;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class MockDataService {
  private accounts = new Map<string, MockAccount>();
  private pendingAccounts: ContaPendenteResponse[] = [];
  private games = new Map<string, JogoResponse[]>();
  private currentUserEmail: string | null = null;

  constructor() {
    this.seedAdmin();
    this.seedConcursos();
  }

  private seedAdmin(): void {
    const adminId = this.uuid();
    this.accounts.set('admin@local.com', {
      profile: {
        id: adminId,
        email: 'admin@local.com',
        papel: 'ADMINISTRADOR',
        estado: 'ATIVO',
        metodoLogin: 'EMAIL_SENHA',
        ultimoAcessoEm: new Date().toISOString()
      },
      password: '123456'
    });
  }

  private concursos: ConcursoResponse[] = [];

  private seedConcursos(): void {
    this.concursos = [
      {
        numero: 2750,
        dataSorteio: '2024-12-28',
        dezenas: [5, 12, 23, 34, 45, 56],
        valorPremio: 35000000
      },
      {
        numero: 2749,
        dataSorteio: '2024-12-25',
        dezenas: [3, 17, 29, 38, 44, 52],
        valorPremio: 22000000
      }
    ];
  }

  // Auth
  login(email: string, password: string): ContaResponse | null {
    const account = this.accounts.get(email);
    if (account && account.password === password) {
      account.profile.ultimoAcessoEm = new Date().toISOString();
      this.currentUserEmail = email;
      return { ...account.profile };
    }
    return null;
  }

  register(email: string, password: string): ContaResponse | null {
    if (this.accounts.has(email)) {
      return null;
    }
    const id = this.uuid();
    const profile: ContaResponse = {
      id,
      email,
      papel: 'USUARIO',
      estado: 'PENDENTE',
      metodoLogin: 'EMAIL_SENHA',
      ultimoAcessoEm: new Date().toISOString()
    };
    this.accounts.set(email, { profile, password });
    this.pendingAccounts.push({
      id,
      email,
      metodoLogin: 'EMAIL_SENHA',
      criadoEm: new Date().toISOString()
    });
    this.currentUserEmail = email;
    return { ...profile };
  }

  getProfile(email: string): ContaResponse | null {
    const account = this.accounts.get(email);
    return account ? { ...account.profile } : null;
  }

  getCurrentUser(): ContaResponse | null {
    if (!this.currentUserEmail) return null;
    return this.getProfile(this.currentUserEmail);
  }

  setCurrentUser(email: string | null): void {
    this.currentUserEmail = email;
  }

  getCurrentEmail(): string | null {
    return this.currentUserEmail;
  }

  // Admin - Moderation
  getPendingAccounts(): ContaPendenteResponse[] {
    return [...this.pendingAccounts];
  }

  approveAccount(id: string): boolean {
    const pending = this.pendingAccounts.find(a => a.id === id);
    if (!pending) return false;

    const account = this.accounts.get(pending.email);
    if (account) {
      account.profile.estado = 'ATIVO';
    }
    this.pendingAccounts = this.pendingAccounts.filter(a => a.id !== id);
    return true;
  }

  rejectAccount(id: string, motivo: string): boolean {
    const pending = this.pendingAccounts.find(a => a.id === id);
    if (!pending) return false;

    const account = this.accounts.get(pending.email);
    if (account) {
      account.profile.estado = 'REPROVADO';
    }
    this.pendingAccounts = this.pendingAccounts.filter(a => a.id !== id);
    return true;
  }

  // Games
  getGames(email: string): JogoResponse[] {
    return [...(this.games.get(email) ?? [])];
  }

  createGame(email: string, dezenas?: number[], quantidade?: number): JogoResponse {
    const nums = dezenas ?? this.generateRandomNumbers(quantidade ?? 6);
    const game: JogoResponse = {
      id: this.uuid(),
      concursoNumero: this.concursos[0]?.numero ?? 2750,
      tipoSelecao: dezenas ? 'MANUAL' : 'AUTOMATICO',
      dezenas: nums.sort((a, b) => a - b),
      criadoEm: new Date().toISOString(),
      atualizadoEm: new Date().toISOString()
    };
    const userGames = this.games.get(email) ?? [];
    userGames.push(game);
    this.games.set(email, userGames);
    return { ...game };
  }

  updateGame(email: string, id: string, dezenas: number[]): JogoResponse | null {
    const userGames = this.games.get(email) ?? [];
    const game = userGames.find(g => g.id === id);
    if (!game) return null;
    game.dezenas = dezenas.sort((a, b) => a - b);
    game.tipoSelecao = 'MANUAL';
    game.atualizadoEm = new Date().toISOString();
    return { ...game };
  }

  deleteGame(email: string, id: string): boolean {
    const userGames = this.games.get(email) ?? [];
    const idx = userGames.findIndex(g => g.id === id);
    if (idx === -1) return false;
    userGames.splice(idx, 1);
    return true;
  }

  // Conference
  getConferences(email: string): ConferenciaResponse[] {
    const userGames = this.games.get(email) ?? [];
    const latestConcurso = this.concursos[0];
    if (!latestConcurso) return [];

    return userGames.map(game => {
      const hits = game.dezenas.filter(d => latestConcurso.dezenas.includes(d));
      const acertos = hits.length;
      let faixa: 'SENA' | 'QUINA' | 'QUADRA' | 'NENHUMA' = 'NENHUMA';
      if (acertos >= 6) faixa = 'SENA';
      else if (acertos === 5) faixa = 'QUINA';
      else if (acertos === 4) faixa = 'QUADRA';

      return {
        jogoId: game.id,
        concursoNumero: game.concursoNumero,
        dezenasJogadas: game.dezenas,
        dezenasSorteadas: latestConcurso.dezenas,
        acertos,
        faixa,
        premiado: acertos >= 4,
        status: 'CONFERIDO' as const
      };
    });
  }

  // Concursos
  getLatestConcurso(): ConcursoResponse | null {
    return this.concursos[0] ?? null;
  }

  getConcurso(numero: number): ConcursoResponse | null {
    return this.concursos.find(c => c.numero === numero) ?? null;
  }

  // Sync
  getSyncStatus(): SyncRunResponse {
    return {
      id: 1,
      origem: 'MANUAL',
      status: 'CONCLUIDO',
      iniciadoEm: new Date(Date.now() - 3600000).toISOString(),
      finalizadoEm: new Date(Date.now() - 3590000).toISOString(),
      concursosAfetados: 2,
      mensagemErro: null
    };
  }

  triggerSync(): SyncRunResponse {
    return {
      id: 2,
      origem: 'MANUAL',
      status: 'CONCLUIDO',
      iniciadoEm: new Date().toISOString(),
      finalizadoEm: new Date().toISOString(),
      concursosAfetados: 0,
      mensagemErro: null
    };
  }

  private generateRandomNumbers(count: number): number[] {
    const nums = new Set<number>();
    while (nums.size < count) {
      nums.add(Math.floor(Math.random() * 60) + 1);
    }
    return Array.from(nums);
  }

  private uuid(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
}
