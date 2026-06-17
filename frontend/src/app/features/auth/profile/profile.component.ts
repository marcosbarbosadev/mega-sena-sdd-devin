import { Component } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [DatePipe],
  template: `
    <div class="profile-container">
      <div class="profile-card">
        <h1>Meu Perfil</h1>

        @if (authService.user(); as user) {
          <div class="profile-fields">
            <div class="field">
              <span class="label">E-mail</span>
              <span class="value">{{ user.email }}</span>
            </div>
            <div class="field">
              <span class="label">Papel</span>
              <span class="badge" [class]="user.papel === 'ADMINISTRADOR' ? 'badge-admin' : 'badge-user'">
                {{ user.papel === 'ADMINISTRADOR' ? 'Administrador' : 'Usuário' }}
              </span>
            </div>
            <div class="field">
              <span class="label">Estado</span>
              <span class="badge badge-active">{{ user.estado }}</span>
            </div>
            <div class="field">
              <span class="label">Método de Login</span>
              <span class="value">{{ user.metodoLogin === 'GOOGLE' ? 'Google' : 'E-mail e Senha' }}</span>
            </div>
            <div class="field">
              <span class="label">Último Acesso</span>
              <span class="value">{{ user.ultimoAcessoEm | date:'dd/MM/yyyy HH:mm' }}</span>
            </div>
          </div>
        }

        <button class="btn-logout" (click)="authService.logout()">Sair</button>
      </div>
    </div>
  `,
  styles: [`
    .profile-container { padding: 2rem 1rem; max-width: 600px; margin: 0 auto; }
    .profile-card {
      background: white;
      border-radius: 12px;
      padding: 2rem;
      box-shadow: 0 2px 12px rgba(0,0,0,0.06);
    }
    h1 { margin: 0 0 1.5rem; font-size: 1.375rem; color: #1a1a1a; }
    .profile-fields { display: flex; flex-direction: column; gap: 1rem; }
    .field {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.75rem 0;
      border-bottom: 1px solid #f3f4f6;
    }
    .label { font-size: 0.875rem; color: #6b7280; }
    .value { font-size: 0.9375rem; color: #1a1a1a; font-weight: 500; }
    .badge {
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
    }
    .badge-admin { background: #fef3c7; color: #92400e; }
    .badge-user { background: #dbeafe; color: #1e40af; }
    .badge-active { background: #d1fae5; color: #065f46; }
    .btn-logout {
      margin-top: 2rem;
      width: 100%;
      padding: 0.75rem;
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
export class ProfileComponent {
  constructor(public authService: AuthService) {}
}
