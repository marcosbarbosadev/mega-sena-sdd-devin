import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ContaResponse,
  JogoResponse,
  JogoRequest,
  ConferenciaResponse,
  ConcursoResponse,
  ContaPendenteResponse,
  SyncRunResponse,
  ReprovacaoRequest
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Auth
  getProfile(): Observable<ContaResponse> {
    return this.http.get<ContaResponse>(`${this.baseUrl}/auth/me`);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/logout`, {});
  }

  // Games
  getGames(): Observable<JogoResponse[]> {
    return this.http.get<JogoResponse[]>(`${this.baseUrl}/jogos`);
  }

  createGame(request: JogoRequest): Observable<JogoResponse> {
    return this.http.post<JogoResponse>(`${this.baseUrl}/jogos`, request);
  }

  updateGame(id: string, request: JogoRequest): Observable<JogoResponse> {
    return this.http.put<JogoResponse>(`${this.baseUrl}/jogos/${id}`, request);
  }

  deleteGame(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/jogos/${id}`);
  }

  // Conference
  getConferences(): Observable<ConferenciaResponse[]> {
    return this.http.get<ConferenciaResponse[]>(`${this.baseUrl}/conferencias`);
  }

  getConference(gameId: string): Observable<ConferenciaResponse> {
    return this.http.get<ConferenciaResponse>(`${this.baseUrl}/jogos/${gameId}/conferencia`);
  }

  // Concursos
  getLatestConcurso(): Observable<ConcursoResponse> {
    return this.http.get<ConcursoResponse>(`${this.baseUrl}/concursos/latest`);
  }

  getConcurso(numero: number): Observable<ConcursoResponse> {
    return this.http.get<ConcursoResponse>(`${this.baseUrl}/concursos/${numero}`);
  }

  // Admin - Moderation
  getPendingAccounts(): Observable<ContaPendenteResponse[]> {
    return this.http.get<ContaPendenteResponse[]>(`${this.baseUrl}/admin/contas/pendentes`);
  }

  approveAccount(id: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/admin/contas/${id}/aprovar`, {});
  }

  rejectAccount(id: string, request: ReprovacaoRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/admin/contas/${id}/reprovar`, request);
  }

  // Admin - Sync
  getSyncStatus(): Observable<SyncRunResponse> {
    return this.http.get<SyncRunResponse>(`${this.baseUrl}/admin/sync/status`);
  }

  triggerSync(): Observable<SyncRunResponse> {
    return this.http.post<SyncRunResponse>(`${this.baseUrl}/admin/sync/run`, {});
  }
}
