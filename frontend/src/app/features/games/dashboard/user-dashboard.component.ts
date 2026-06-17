import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { JogoResponse, ConferenciaResponse } from '../../../core/models';

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [RouterLink, LoadingComponent],
  template: `
    <div class="dashboard-container">
      <h1>Dashboard</h1>
      <p class="welcome">Bem-vindo, {{ authService.user()?.email }}</p>

      @if (loading()) {
        <app-loading />
      } @else {
        <div class="stats-grid">
          <div class="stat-card">
            <span class="stat-number">{{ totalGames() }}</span>
            <span class="stat-label">Total de Jogos</span>
          </div>
          <div class="stat-card">
            <span class="stat-number">{{ awaitingGames() }}</span>
            <span class="stat-label">Aguardando Sorteio</span>
          </div>
          <div class="stat-card">
            <span class="stat-number">{{ checkedGames() }}</span>
            <span class="stat-label">Conferidos</span>
          </div>
          <div class="stat-card highlight">
            <span class="stat-number">{{ awardedGames() }}</span>
            <span class="stat-label">Premiados</span>
          </div>
        </div>

        @if (totalGames() === 0) {
          <div class="empty-state">
            <span class="empty-icon">🎲</span>
            <h2>Nenhum jogo cadastrado</h2>
            <p>Cadastre seu primeiro jogo da Mega Sena!</p>
            <a routerLink="/games/create" class="btn-primary">Cadastrar Jogo</a>
          </div>
        } @else {
          <div class="actions">
            <a routerLink="/games/create" class="btn-primary">Cadastrar Novo Jogo</a>
            <a routerLink="/games" class="btn-secondary">Ver Meus Jogos</a>
            <a routerLink="/conference" class="btn-secondary">Ver Conferências</a>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .dashboard-container { padding: 2rem 1rem; max-width: 900px; margin: 0 auto; }
    h1 { margin: 0; font-size: 1.5rem; color: #1a1a1a; }
    .welcome { color: #6b7280; font-size: 0.875rem; margin: 0.25rem 0 1.5rem; }
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 1rem;
      margin-bottom: 2rem;
    }
    .stat-card {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      text-align: center;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
    }
    .stat-card.highlight { border: 2px solid #209869; }
    .stat-number {
      display: block;
      font-size: 2rem;
      font-weight: 700;
      color: #1a1a1a;
    }
    .stat-label {
      display: block;
      margin-top: 0.25rem;
      font-size: 0.8125rem;
      color: #6b7280;
    }
    .empty-state {
      text-align: center;
      padding: 3rem 1rem;
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
    }
    .empty-icon { font-size: 3rem; }
    .empty-state h2 { margin: 1rem 0 0.5rem; color: #1a1a1a; font-size: 1.25rem; }
    .empty-state p { color: #6b7280; margin: 0 0 1.5rem; }
    .actions { display: flex; gap: 1rem; flex-wrap: wrap; }
    .btn-primary, .btn-secondary {
      padding: 0.75rem 1.5rem;
      border-radius: 8px;
      text-decoration: none;
      font-size: 0.9375rem;
      font-weight: 600;
      text-align: center;
      transition: background 0.2s;
    }
    .btn-primary { background: #209869; color: white; }
    .btn-primary:hover { background: #1a7d56; }
    .btn-secondary {
      background: white;
      color: #209869;
      border: 1px solid #209869;
    }
    .btn-secondary:hover { background: #f0fdf4; }
  `]
})
export class UserDashboardComponent implements OnInit {
  readonly loading = signal(true);
  readonly totalGames = signal(0);
  readonly awaitingGames = signal(0);
  readonly checkedGames = signal(0);
  readonly awardedGames = signal(0);

  constructor(
    private api: ApiService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  private loadStats(): void {
    this.api.getGames().subscribe({
      next: (games) => {
        this.totalGames.set(games.length);
        this.api.getConferences().subscribe({
          next: (confs) => {
            const awaiting = confs.filter(c => c.status === 'AGUARDANDO_SORTEIO').length;
            const checked = confs.filter(c => c.status === 'CONFERIDO').length;
            const awarded = confs.filter(c => c.premiado).length;
            this.awaitingGames.set(awaiting);
            this.checkedGames.set(checked);
            this.awardedGames.set(awarded);
            this.loading.set(false);
          },
          error: () => this.loading.set(false)
        });
      },
      error: () => this.loading.set(false)
    });
  }
}
