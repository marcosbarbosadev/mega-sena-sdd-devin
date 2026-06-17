import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, LoadingComponent],
  template: `
    <div class="login-page">
      <div class="login-card">
        <div class="login-header">
          <span class="login-icon">🍀</span>
          <h1>Mega Sena Manager</h1>
          <p class="login-subtitle">Gerencie seus jogos da Mega Sena</p>
        </div>

        @if (loading()) {
          <app-loading message="Autenticando..." />
        } @else {
          <div class="login-tabs">
            <button
              class="tab"
              [class.active]="mode() === 'login'"
              (click)="mode.set('login')"
            >Entrar</button>
            <button
              class="tab"
              [class.active]="mode() === 'register'"
              (click)="mode.set('register')"
            >Cadastrar</button>
          </div>

          <form (submit)="onEmailSubmit($event)" class="login-form">
            <div class="form-group">
              <label for="email">E-mail</label>
              <input
                id="email"
                type="email"
                [(ngModel)]="email"
                name="email"
                required
                placeholder="seu@email.com"
                autocomplete="email"
              />
            </div>
            <div class="form-group">
              <label for="password">Senha</label>
              <input
                id="password"
                type="password"
                [(ngModel)]="password"
                name="password"
                required
                placeholder="Sua senha"
                [attr.autocomplete]="mode() === 'register' ? 'new-password' : 'current-password'"
                minlength="6"
              />
            </div>

            @if (errorMessage()) {
              <div class="error-banner" role="alert">{{ errorMessage() }}</div>
            }

            <button type="submit" class="btn-primary" [disabled]="!email || !password">
              {{ mode() === 'login' ? 'Entrar' : 'Cadastrar' }}
            </button>
          </form>

          <div class="divider">
            <span>ou</span>
          </div>

          <button class="btn-google" (click)="onGoogleLogin()">
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            Continuar com Google
          </button>
        }
      </div>
    </div>
  `,
  styles: [`
    .login-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #209869 0%, #16a34a 100%);
      padding: 1rem;
    }
    .login-card {
      background: white;
      border-radius: 16px;
      padding: 2.5rem 2rem;
      width: 100%;
      max-width: 420px;
      box-shadow: 0 20px 60px rgba(0,0,0,0.15);
    }
    .login-header {
      text-align: center;
      margin-bottom: 2rem;
    }
    .login-icon { font-size: 3rem; }
    .login-header h1 {
      margin: 0.5rem 0 0.25rem;
      font-size: 1.5rem;
      color: #1a1a1a;
    }
    .login-subtitle {
      color: #666;
      font-size: 0.875rem;
      margin: 0;
    }
    .login-tabs {
      display: flex;
      gap: 0.25rem;
      background: #f3f4f6;
      border-radius: 8px;
      padding: 0.25rem;
      margin-bottom: 1.5rem;
    }
    .tab {
      flex: 1;
      padding: 0.5rem;
      border: none;
      background: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: 0.875rem;
      font-weight: 500;
      color: #666;
      transition: all 0.2s;
    }
    .tab.active {
      background: white;
      color: #209869;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    .login-form { display: flex; flex-direction: column; gap: 1rem; }
    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.375rem;
    }
    .form-group label {
      font-size: 0.8125rem;
      font-weight: 500;
      color: #374151;
    }
    .form-group input {
      padding: 0.625rem 0.75rem;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      font-size: 0.9375rem;
      transition: border-color 0.2s;
      outline: none;
    }
    .form-group input:focus {
      border-color: #209869;
      box-shadow: 0 0 0 3px rgba(32,152,105,0.1);
    }
    .error-banner {
      padding: 0.75rem;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 8px;
      color: #991b1b;
      font-size: 0.8125rem;
    }
    .btn-primary {
      padding: 0.75rem;
      background: #209869;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
    }
    .btn-primary:hover:not(:disabled) { background: #1a7d56; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .divider {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin: 1.5rem 0;
      color: #9ca3af;
      font-size: 0.8125rem;
    }
    .divider::before, .divider::after {
      content: '';
      flex: 1;
      height: 1px;
      background: #e5e7eb;
    }
    .btn-google {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
      padding: 0.75rem;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      background: white;
      cursor: pointer;
      font-size: 0.9375rem;
      color: #374151;
      transition: background 0.2s;
    }
    .btn-google:hover { background: #f9fafb; }
  `]
})
export class LoginComponent {
  email = '';
  password = '';
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly mode = signal<'login' | 'register'>('login');

  constructor(
    private authService: AuthService,
    private router: Router,
    private notifications: NotificationService
  ) {}

  async onEmailSubmit(event: Event): Promise<void> {
    event.preventDefault();
    this.loading.set(true);
    this.errorMessage.set('');
    try {
      if (this.mode() === 'register') {
        await this.authService.registerWithEmail(this.email, this.password);
      } else {
        await this.authService.loginWithEmail(this.email, this.password);
      }
      this.navigateAfterLogin();
    } catch (err: unknown) {
      this.errorMessage.set(this.getFirebaseError(err));
    } finally {
      this.loading.set(false);
    }
  }

  async onGoogleLogin(): Promise<void> {
    this.loading.set(true);
    this.errorMessage.set('');
    try {
      await this.authService.loginWithGoogle();
      this.navigateAfterLogin();
    } catch (err: unknown) {
      this.errorMessage.set(this.getFirebaseError(err));
    } finally {
      this.loading.set(false);
    }
  }

  private navigateAfterLogin(): void {
    const state = this.authService.accountState();
    if (state === 'PENDENTE') {
      this.router.navigate(['/pending']);
    } else if (state === 'REPROVADO') {
      this.router.navigate(['/rejected']);
    } else if (this.authService.isAdmin()) {
      this.router.navigate(['/admin']);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }

  private getFirebaseError(err: unknown): string {
    const code = (err as { code?: string })?.code ?? '';
    const map: Record<string, string> = {
      'auth/user-not-found': 'Usuário não encontrado.',
      'auth/wrong-password': 'Senha incorreta.',
      'auth/invalid-email': 'E-mail inválido.',
      'auth/email-already-in-use': 'Este e-mail já está em uso.',
      'auth/weak-password': 'A senha deve ter pelo menos 6 caracteres.',
      'auth/too-many-requests': 'Muitas tentativas. Tente novamente mais tarde.',
      'auth/popup-closed-by-user': 'Login cancelado.',
      'auth/network-request-failed': 'Erro de conexão. Verifique sua internet.',
      'auth/invalid-credential': 'Credenciais inválidas.',
    };
    return map[code] ?? 'Erro ao autenticar. Tente novamente.';
  }
}
