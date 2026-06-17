import { Component } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-pending-state',
  standalone: true,
  template: `
    <div class="state-page">
      <div class="state-card">
        @if (authService.accountState() === 'REPROVADO') {
          <span class="state-icon">🚫</span>
          <h1>Acesso Negado</h1>
          <p>Sua conta foi analisada e o acesso não foi aprovado.</p>
          <p class="state-detail">Se você acredita que houve um engano, entre em contato com o administrador.</p>
        } @else {
          <span class="state-icon">⏳</span>
          <h1>Conta Pendente</h1>
          <p>Sua conta foi criada e está aguardando aprovação de um administrador.</p>
          <p class="state-detail">Você será notificado quando sua conta for aprovada.</p>
        }
        <button class="btn-logout" (click)="authService.logout()">Sair</button>
      </div>
    </div>
  `,
  styles: [`
    .state-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f9fafb;
      padding: 1rem;
    }
    .state-card {
      background: white;
      border-radius: 16px;
      padding: 3rem 2rem;
      text-align: center;
      max-width: 480px;
      box-shadow: 0 4px 24px rgba(0,0,0,0.08);
    }
    .state-icon { font-size: 4rem; }
    h1 { margin: 1rem 0 0.5rem; color: #1a1a1a; font-size: 1.5rem; }
    p { color: #4b5563; margin: 0.5rem 0; }
    .state-detail { font-size: 0.875rem; color: #9ca3af; }
    .btn-logout {
      margin-top: 2rem;
      padding: 0.625rem 2rem;
      background: #ef4444;
      color: white;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      font-size: 0.9375rem;
    }
    .btn-logout:hover { background: #dc2626; }
  `]
})
export class PendingStateComponent {
  constructor(public authService: AuthService) {}
}
