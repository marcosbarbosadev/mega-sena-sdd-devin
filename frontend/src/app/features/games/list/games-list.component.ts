import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { JogoResponse } from '../../../core/models';

@Component({
  selector: 'app-games-list',
  standalone: true,
  imports: [RouterLink, DatePipe, LoadingComponent],
  template: `
    <div class="games-container">
      <div class="games-header">
        <h1>Meus Jogos</h1>
        <a routerLink="/games/create" class="btn-primary">+ Novo Jogo</a>
      </div>

      @if (loading()) {
        <app-loading />
      } @else if (games().length === 0) {
        <div class="empty-state">
          <span class="empty-icon">🎲</span>
          <h2>Nenhum jogo encontrado</h2>
          <p>Cadastre seu primeiro jogo!</p>
          <a routerLink="/games/create" class="btn-primary">Cadastrar Jogo</a>
        </div>
      } @else {
        <div class="games-grid">
          @for (game of games(); track game.id) {
            <div class="game-card">
              <div class="game-header-row">
                <span class="concurso-badge">Concurso {{ game.concursoNumero }}</span>
                <span class="tipo-badge" [class]="game.tipoSelecao === 'MANUAL' ? 'manual' : 'auto'">
                  {{ game.tipoSelecao === 'MANUAL' ? 'Manual' : 'Automático' }}
                </span>
              </div>
              <div class="dezenas">
                @for (d of game.dezenas; track d) {
                  <span class="dezena">{{ d < 10 ? '0' + d : d }}</span>
                }
              </div>
              <div class="game-footer">
                <span class="game-date">{{ game.criadoEm | date:'dd/MM/yyyy' }}</span>
                <div class="game-actions">
                  <a [routerLink]="['/games/edit', game.id]" class="btn-edit" title="Editar">✏️</a>
                  <button class="btn-delete" (click)="confirmDelete(game)" title="Excluir">🗑️</button>
                </div>
              </div>
            </div>
          }
        </div>
      }

      @if (deleteTarget()) {
        <div class="modal-overlay" (click)="deleteTarget.set(null)">
          <div class="modal" (click)="$event.stopPropagation()">
            <h3>Confirmar Exclusão</h3>
            <p>Deseja realmente excluir este jogo?</p>
            <div class="dezenas small">
              @for (d of deleteTarget()!.dezenas; track d) {
                <span class="dezena small">{{ d < 10 ? '0' + d : d }}</span>
              }
            </div>
            <div class="modal-actions">
              <button class="btn-cancel" (click)="deleteTarget.set(null)">Cancelar</button>
              <button class="btn-confirm-delete" (click)="deleteGame()">Excluir</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .games-container { padding: 2rem 1rem; max-width: 900px; margin: 0 auto; }
    .games-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
    }
    h1 { margin: 0; font-size: 1.375rem; color: #1a1a1a; }
    .btn-primary {
      padding: 0.625rem 1.25rem;
      background: #209869;
      color: white;
      border-radius: 8px;
      text-decoration: none;
      font-size: 0.875rem;
      font-weight: 600;
    }
    .btn-primary:hover { background: #1a7d56; }
    .empty-state {
      text-align: center;
      padding: 3rem;
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
    }
    .empty-icon { font-size: 3rem; }
    .empty-state h2 { margin: 1rem 0 0.5rem; }
    .empty-state p { color: #6b7280; margin: 0 0 1.5rem; }
    .games-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 1rem;
    }
    .game-card {
      background: white;
      border-radius: 12px;
      padding: 1.25rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
    }
    .game-header-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }
    .concurso-badge {
      font-size: 0.8125rem;
      font-weight: 600;
      color: #209869;
    }
    .tipo-badge {
      font-size: 0.6875rem;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      font-weight: 500;
    }
    .tipo-badge.manual { background: #dbeafe; color: #1e40af; }
    .tipo-badge.auto { background: #fef3c7; color: #92400e; }
    .dezenas {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
    .dezena {
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      background: #209869;
      color: white;
      font-weight: 700;
      font-size: 0.875rem;
    }
    .dezena.small { width: 32px; height: 32px; font-size: 0.75rem; }
    .dezenas.small { justify-content: center; margin: 1rem 0; }
    .game-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .game-date { font-size: 0.75rem; color: #9ca3af; }
    .game-actions { display: flex; gap: 0.5rem; }
    .btn-edit, .btn-delete {
      background: none;
      border: none;
      cursor: pointer;
      font-size: 1rem;
      padding: 0.25rem;
      text-decoration: none;
    }
    .modal-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 200;
    }
    .modal {
      background: white;
      border-radius: 12px;
      padding: 2rem;
      max-width: 400px;
      width: 90%;
      text-align: center;
    }
    .modal h3 { margin: 0 0 0.5rem; color: #1a1a1a; }
    .modal p { color: #6b7280; font-size: 0.875rem; }
    .modal-actions { display: flex; gap: 1rem; justify-content: center; margin-top: 1.5rem; }
    .btn-cancel {
      padding: 0.625rem 1.25rem;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      background: white;
      cursor: pointer;
    }
    .btn-confirm-delete {
      padding: 0.625rem 1.25rem;
      background: #ef4444;
      color: white;
      border: none;
      border-radius: 8px;
      cursor: pointer;
    }
    .btn-confirm-delete:hover { background: #dc2626; }
  `]
})
export class GamesListComponent implements OnInit {
  readonly loading = signal(true);
  readonly games = signal<JogoResponse[]>([]);
  readonly deleteTarget = signal<JogoResponse | null>(null);

  constructor(
    private api: ApiService,
    private notifications: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadGames();
  }

  confirmDelete(game: JogoResponse): void {
    this.deleteTarget.set(game);
  }

  deleteGame(): void {
    const game = this.deleteTarget();
    if (!game) return;
    this.api.deleteGame(game.id).subscribe({
      next: () => {
        this.games.update(list => list.filter(g => g.id !== game.id));
        this.deleteTarget.set(null);
        this.notifications.success('Jogo excluído com sucesso.');
      },
      error: () => {
        this.notifications.error('Erro ao excluir jogo.');
        this.deleteTarget.set(null);
      }
    });
  }

  private loadGames(): void {
    this.api.getGames().subscribe({
      next: (games) => {
        this.games.set(games);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
