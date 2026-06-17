import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { NotificationService } from '../../../core/services/notification.service';
import { JogoResponse } from '../../../core/models';

@Component({
  selector: 'app-game-create',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="create-container">
      <h1>Cadastrar Jogo</h1>

      @if (created()) {
        <div class="success-card">
          <span class="success-icon">🎉</span>
          <h2>Jogo cadastrado!</h2>
          <p>Concurso vinculado: <strong>{{ created()!.concursoNumero }}</strong></p>
          <div class="dezenas">
            @for (d of created()!.dezenas; track d) {
              <span class="dezena">{{ d < 10 ? '0' + d : d }}</span>
            }
          </div>
          <div class="success-actions">
            <button class="btn-primary" (click)="reset()">Cadastrar Outro</button>
            <a routerLink="/games" class="btn-secondary">Ver Meus Jogos</a>
          </div>
        </div>
      } @else {
        <div class="mode-tabs">
          <button class="tab" [class.active]="mode() === 'MANUAL'" (click)="mode.set('MANUAL')">
            Manual
          </button>
          <button class="tab" [class.active]="mode() === 'AUTOMATICO'" (click)="mode.set('AUTOMATICO')">
            Automático
          </button>
        </div>

        @if (mode() === 'MANUAL') {
          <div class="manual-section">
            <p class="section-desc">Selecione de 6 a 9 dezenas (1 a 60):</p>
            <div class="number-grid">
              @for (n of numbers; track n) {
                <button
                  class="number-btn"
                  [class.selected]="selectedNumbers().includes(n)"
                  (click)="toggleNumber(n)"
                  [disabled]="!selectedNumbers().includes(n) && selectedNumbers().length >= 9"
                  [attr.aria-label]="'Número ' + n"
                >
                  {{ n < 10 ? '0' + n : n }}
                </button>
              }
            </div>
            <p class="selection-count">
              {{ selectedNumbers().length }} de 6-9 dezenas selecionadas
            </p>
          </div>
        } @else {
          <div class="auto-section">
            <p class="section-desc">Escolha a quantidade de dezenas a gerar:</p>
            <div class="quantity-options">
              @for (q of [6, 7, 8, 9]; track q) {
                <button
                  class="qty-btn"
                  [class.active]="quantity() === q"
                  (click)="quantity.set(q)"
                >
                  {{ q }} dezenas
                </button>
              }
            </div>
          </div>
        }

        @if (errorMessage()) {
          <div class="error-banner" role="alert">{{ errorMessage() }}</div>
        }

        <div class="form-actions">
          <button
            class="btn-primary"
            (click)="submit()"
            [disabled]="saving() || !isValid()"
          >
            @if (saving()) {
              Salvando...
            } @else {
              Salvar Jogo
            }
          </button>
          <a routerLink="/games" class="btn-secondary">Cancelar</a>
        </div>
      }
    </div>
  `,
  styles: [`
    .create-container { padding: 2rem 1rem; max-width: 700px; margin: 0 auto; }
    h1 { margin: 0 0 1.5rem; font-size: 1.375rem; color: #1a1a1a; }
    .mode-tabs {
      display: flex;
      gap: 0.25rem;
      background: #f3f4f6;
      border-radius: 8px;
      padding: 0.25rem;
      margin-bottom: 1.5rem;
    }
    .tab {
      flex: 1;
      padding: 0.625rem;
      border: none;
      background: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: 0.9375rem;
      font-weight: 500;
      color: #666;
      transition: all 0.2s;
    }
    .tab.active {
      background: white;
      color: #209869;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    .section-desc { color: #6b7280; font-size: 0.875rem; margin: 0 0 1rem; }
    .number-grid {
      display: grid;
      grid-template-columns: repeat(10, 1fr);
      gap: 0.375rem;
    }
    .number-btn {
      aspect-ratio: 1;
      border: 2px solid #e5e7eb;
      border-radius: 50%;
      background: white;
      cursor: pointer;
      font-size: 0.8125rem;
      font-weight: 600;
      color: #374151;
      transition: all 0.15s;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .number-btn:hover:not(:disabled) { border-color: #209869; }
    .number-btn.selected {
      background: #209869;
      border-color: #209869;
      color: white;
    }
    .number-btn:disabled:not(.selected) { opacity: 0.3; cursor: not-allowed; }
    .selection-count {
      text-align: center;
      margin-top: 1rem;
      font-size: 0.875rem;
      color: #6b7280;
    }
    .auto-section { margin-bottom: 1.5rem; }
    .quantity-options { display: flex; gap: 0.75rem; }
    .qty-btn {
      flex: 1;
      padding: 1rem;
      border: 2px solid #e5e7eb;
      border-radius: 10px;
      background: white;
      cursor: pointer;
      font-size: 0.9375rem;
      font-weight: 600;
      color: #374151;
      transition: all 0.15s;
    }
    .qty-btn.active {
      border-color: #209869;
      background: #f0fdf4;
      color: #209869;
    }
    .error-banner {
      margin: 1rem 0;
      padding: 0.75rem;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 8px;
      color: #991b1b;
      font-size: 0.8125rem;
    }
    .form-actions {
      display: flex;
      gap: 1rem;
      margin-top: 2rem;
    }
    .btn-primary {
      flex: 1;
      padding: 0.75rem;
      background: #209869;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
    }
    .btn-primary:hover:not(:disabled) { background: #1a7d56; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-secondary {
      padding: 0.75rem 1.5rem;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      background: white;
      color: #374151;
      text-decoration: none;
      text-align: center;
      font-size: 0.9375rem;
    }
    .success-card {
      background: white;
      border-radius: 12px;
      padding: 2.5rem;
      text-align: center;
      box-shadow: 0 2px 12px rgba(0,0,0,0.06);
    }
    .success-icon { font-size: 3rem; }
    .success-card h2 { margin: 1rem 0 0.5rem; color: #1a1a1a; }
    .success-card p { color: #6b7280; }
    .dezenas {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      justify-content: center;
      margin: 1.5rem 0;
    }
    .dezena {
      width: 44px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      background: #209869;
      color: white;
      font-weight: 700;
      font-size: 0.9375rem;
    }
    .success-actions { display: flex; gap: 1rem; justify-content: center; margin-top: 1.5rem; }

    @media (max-width: 640px) {
      .number-grid { grid-template-columns: repeat(6, 1fr); }
      .quantity-options { flex-wrap: wrap; }
      .qty-btn { flex: 1 1 calc(50% - 0.375rem); }
    }
  `]
})
export class GameCreateComponent {
  readonly numbers = Array.from({ length: 60 }, (_, i) => i + 1);
  readonly mode = signal<'MANUAL' | 'AUTOMATICO'>('MANUAL');
  readonly selectedNumbers = signal<number[]>([]);
  readonly quantity = signal(6);
  readonly saving = signal(false);
  readonly errorMessage = signal('');
  readonly created = signal<JogoResponse | null>(null);

  constructor(
    private api: ApiService,
    private router: Router,
    private notifications: NotificationService
  ) {}

  toggleNumber(n: number): void {
    this.selectedNumbers.update(nums => {
      if (nums.includes(n)) return nums.filter(x => x !== n);
      if (nums.length >= 9) return nums;
      return [...nums, n].sort((a, b) => a - b);
    });
  }

  isValid(): boolean {
    if (this.mode() === 'MANUAL') {
      const count = this.selectedNumbers().length;
      return count >= 6 && count <= 9;
    }
    return this.quantity() >= 6 && this.quantity() <= 9;
  }

  submit(): void {
    if (!this.isValid()) return;
    this.saving.set(true);
    this.errorMessage.set('');

    const request = this.mode() === 'MANUAL'
      ? { dezenas: this.selectedNumbers() }
      : { quantidade: this.quantity() };

    this.api.createGame(request).subscribe({
      next: (game) => {
        this.created.set(game);
        this.saving.set(false);
        this.notifications.success('Jogo cadastrado com sucesso!');
      },
      error: (err) => {
        const msg = err.error?.mensagem ?? 'Erro ao cadastrar jogo. Tente novamente.';
        this.errorMessage.set(msg);
        this.saving.set(false);
      }
    });
  }

  reset(): void {
    this.created.set(null);
    this.selectedNumbers.set([]);
    this.quantity.set(6);
    this.mode.set('MANUAL');
  }
}
