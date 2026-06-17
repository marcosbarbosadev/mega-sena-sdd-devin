import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { JogoResponse } from '../../../core/models';

@Component({
  selector: 'app-game-edit',
  standalone: true,
  imports: [FormsModule, RouterLink, LoadingComponent],
  template: `
    <div class="edit-container">
      <h1>Editar Jogo</h1>

      @if (loading()) {
        <app-loading />
      } @else if (!game()) {
        <div class="error-state">
          <p>Jogo não encontrado.</p>
          <a routerLink="/games" class="btn-secondary">Voltar</a>
        </div>
      } @else {
        <p class="info">Concurso: <strong>{{ game()!.concursoNumero }}</strong></p>
        <p class="section-desc">Selecione de 6 a 9 dezenas (1 a 60):</p>

        <div class="number-grid">
          @for (n of numbers; track n) {
            <button
              class="number-btn"
              [class.selected]="selectedNumbers().includes(n)"
              (click)="toggleNumber(n)"
              [disabled]="!selectedNumbers().includes(n) && selectedNumbers().length >= 9"
            >
              {{ n < 10 ? '0' + n : n }}
            </button>
          }
        </div>

        <p class="selection-count">{{ selectedNumbers().length }} de 6-9 dezenas</p>

        @if (errorMessage()) {
          <div class="error-banner" role="alert">{{ errorMessage() }}</div>
        }

        <div class="form-actions">
          <button class="btn-primary" (click)="save()" [disabled]="saving() || !isValid()">
            {{ saving() ? 'Salvando...' : 'Salvar Alterações' }}
          </button>
          <a routerLink="/games" class="btn-secondary">Cancelar</a>
        </div>
      }
    </div>
  `,
  styles: [`
    .edit-container { padding: 2rem 1rem; max-width: 700px; margin: 0 auto; }
    h1 { margin: 0 0 1rem; font-size: 1.375rem; color: #1a1a1a; }
    .info { color: #209869; font-weight: 500; margin-bottom: 0.5rem; }
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
    .error-banner {
      margin: 1rem 0;
      padding: 0.75rem;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 8px;
      color: #991b1b;
      font-size: 0.8125rem;
    }
    .form-actions { display: flex; gap: 1rem; margin-top: 2rem; }
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
    }
    .error-state {
      text-align: center;
      padding: 2rem;
      background: white;
      border-radius: 12px;
    }
    @media (max-width: 640px) {
      .number-grid { grid-template-columns: repeat(6, 1fr); }
    }
  `]
})
export class GameEditComponent implements OnInit {
  readonly numbers = Array.from({ length: 60 }, (_, i) => i + 1);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly game = signal<JogoResponse | null>(null);
  readonly selectedNumbers = signal<number[]>([]);
  readonly errorMessage = signal('');

  private gameId = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ApiService,
    private notifications: NotificationService
  ) {}

  ngOnInit(): void {
    this.gameId = this.route.snapshot.paramMap.get('id') ?? '';
    this.loadGame();
  }

  toggleNumber(n: number): void {
    this.selectedNumbers.update(nums => {
      if (nums.includes(n)) return nums.filter(x => x !== n);
      if (nums.length >= 9) return nums;
      return [...nums, n].sort((a, b) => a - b);
    });
  }

  isValid(): boolean {
    const count = this.selectedNumbers().length;
    return count >= 6 && count <= 9;
  }

  save(): void {
    if (!this.isValid()) return;
    this.saving.set(true);
    this.errorMessage.set('');

    this.api.updateGame(this.gameId, { dezenas: this.selectedNumbers() }).subscribe({
      next: () => {
        this.notifications.success('Jogo atualizado com sucesso!');
        this.router.navigate(['/games']);
      },
      error: (err) => {
        const msg = err.error?.mensagem ?? 'Erro ao atualizar jogo.';
        this.errorMessage.set(msg);
        this.saving.set(false);
      }
    });
  }

  private loadGame(): void {
    this.api.getGames().subscribe({
      next: (games) => {
        const found = games.find(g => g.id === this.gameId);
        if (found) {
          this.game.set(found);
          this.selectedNumbers.set([...found.dezenas]);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
