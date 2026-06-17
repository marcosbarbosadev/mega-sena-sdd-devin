import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { of, delay } from 'rxjs';
import { MockDataService } from './mock-data.service';
import { environment } from '../../../environments/environment';

export const mockApiInterceptor: HttpInterceptorFn = (req, next) => {
  if (!environment.useMockAuth) {
    return next(req);
  }

  const mockData = inject(MockDataService);
  const url = req.url;
  const method = req.method;
  const email = mockData.getCurrentEmail();
  const simulatedDelay = 200;

  // Auth endpoints
  if (url.endsWith('/auth/me') && method === 'GET') {
    const profile = mockData.getCurrentUser();
    if (profile) {
      return of(new HttpResponse({ status: 200, body: profile })).pipe(delay(simulatedDelay));
    }
    return of(new HttpResponse({ status: 401, body: { mensagem: 'Não autenticado' } })).pipe(delay(simulatedDelay));
  }

  if (url.endsWith('/auth/logout') && method === 'POST') {
    return of(new HttpResponse({ status: 200 })).pipe(delay(simulatedDelay));
  }

  // Games endpoints
  if (url.match(/\/jogos$/) && method === 'GET') {
    return of(new HttpResponse({ status: 200, body: mockData.getGames(email!) })).pipe(delay(simulatedDelay));
  }

  if (url.match(/\/jogos$/) && method === 'POST') {
    const body = req.body as { dezenas?: number[]; quantidade?: number };
    const game = mockData.createGame(email!, body.dezenas, body.quantidade);
    return of(new HttpResponse({ status: 201, body: game })).pipe(delay(simulatedDelay));
  }

  if (url.match(/\/jogos\/[\w-]+$/) && method === 'PUT') {
    const id = url.split('/').pop()!;
    const body = req.body as { dezenas?: number[] };
    const game = mockData.updateGame(email!, id, body.dezenas ?? []);
    if (game) {
      return of(new HttpResponse({ status: 200, body: game })).pipe(delay(simulatedDelay));
    }
    return of(new HttpResponse({ status: 404, body: { mensagem: 'Jogo não encontrado' } })).pipe(delay(simulatedDelay));
  }

  if (url.match(/\/jogos\/[\w-]+$/) && method === 'DELETE') {
    const id = url.split('/').pop()!;
    mockData.deleteGame(email!, id);
    return of(new HttpResponse({ status: 204 })).pipe(delay(simulatedDelay));
  }

  // Conference endpoints
  if (url.match(/\/conferencias$/) && method === 'GET') {
    return of(new HttpResponse({ status: 200, body: mockData.getConferences(email!) })).pipe(delay(simulatedDelay));
  }

  if (url.match(/\/jogos\/[\w-]+\/conferencia$/) && method === 'GET') {
    const parts = url.split('/');
    const gameId = parts[parts.length - 2];
    const confs = mockData.getConferences(email!);
    const conf = confs.find(c => c.jogoId === gameId);
    if (conf) {
      return of(new HttpResponse({ status: 200, body: conf })).pipe(delay(simulatedDelay));
    }
    return of(new HttpResponse({ status: 404, body: { mensagem: 'Conferência não encontrada' } })).pipe(delay(simulatedDelay));
  }

  // Concursos endpoints
  if (url.match(/\/concursos\/latest$/) && method === 'GET') {
    const c = mockData.getLatestConcurso();
    if (c) {
      return of(new HttpResponse({ status: 200, body: c })).pipe(delay(simulatedDelay));
    }
    return of(new HttpResponse({ status: 404 })).pipe(delay(simulatedDelay));
  }

  if (url.match(/\/concursos\/\d+$/) && method === 'GET') {
    const numero = parseInt(url.split('/').pop()!, 10);
    const c = mockData.getConcurso(numero);
    if (c) {
      return of(new HttpResponse({ status: 200, body: c })).pipe(delay(simulatedDelay));
    }
    return of(new HttpResponse({ status: 404, body: { mensagem: 'Concurso não encontrado' } })).pipe(delay(simulatedDelay));
  }

  // Admin endpoints
  if (url.match(/\/admin\/contas\/pendentes$/) && method === 'GET') {
    return of(new HttpResponse({ status: 200, body: mockData.getPendingAccounts() })).pipe(delay(simulatedDelay));
  }

  if (url.match(/\/admin\/contas\/[\w-]+\/aprovar$/) && method === 'POST') {
    const parts = url.split('/');
    const id = parts[parts.length - 2];
    mockData.approveAccount(id);
    return of(new HttpResponse({ status: 200 })).pipe(delay(simulatedDelay));
  }

  if (url.match(/\/admin\/contas\/[\w-]+\/reprovar$/) && method === 'POST') {
    const parts = url.split('/');
    const id = parts[parts.length - 2];
    const body = req.body as { motivo: string };
    mockData.rejectAccount(id, body.motivo);
    return of(new HttpResponse({ status: 200 })).pipe(delay(simulatedDelay));
  }

  if (url.match(/\/admin\/sync\/status$/) && method === 'GET') {
    return of(new HttpResponse({ status: 200, body: mockData.getSyncStatus() })).pipe(delay(simulatedDelay));
  }

  if (url.match(/\/admin\/sync\/run$/) && method === 'POST') {
    return of(new HttpResponse({ status: 200, body: mockData.triggerSync() })).pipe(delay(simulatedDelay));
  }

  return next(req);
};
