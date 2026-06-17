import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { ContaPendenteResponse, SyncRunResponse } from '../../../core/models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterLink, LoadingComponent],
  template: `
    <div class="admin-container">
      <h1>Painel Administrativo</h1>

      @if (loading()) {
        <app-loading />
      } @else {
        <div class="stats-grid">
          <a routerLink="/admin/moderation" class="stat-card clickable">
            <span class="stat-number" [class.alert]="pendingCount() > 0">{{ pendingCount() }}</span>
            <span class="stat-label">Contas Pendentes</span>
            @if (pendingCount() > 0) {
              <span class="alert-dot"></span>
            }
          </a>
          <a routerLink="/admin/sync" class="stat-card clickable">
            <span class="stat-number">{{ syncStatus()?.status ?? '—' }}</span>
            <span class="stat-label">Última Sincronização</span>
          </a>
        </div>

        <div class="actions">
          <a routerLink="/admin/moderation" class="btn-primary">Gerenciar Contas</a>
          <a routerLink="/admin/sync" class="btn-secondary">Sincronização</a>
        </div>
      }
    </div>
  `,
  styles: [`
    .admin-container { padding: 2rem 1rem; max-width: 900px; margin: 0 auto; }
    h1 { margin: 0 0 1.5rem; font-size: 1.375rem; color: #1a1a1a; }
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 1rem;
      margin-bottom: 2rem;
    }
    .stat-card {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      text-align: center;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
      position: relative;
      text-decoration: none;
      color: inherit;
    }
    .stat-card.clickable { cursor: pointer; transition: box-shadow 0.2s; }
    .stat-card.clickable:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
    .stat-number {
      display: block;
      font-size: 2rem;
      font-weight: 700;
      color: #1a1a1a;
    }
    .stat-number.alert { color: #ef4444; }
    .stat-label {
      display: block;
      margin-top: 0.25rem;
      font-size: 0.8125rem;
      color: #6b7280;
    }
    .alert-dot {
      position: absolute;
      top: 0.75rem;
      right: 0.75rem;
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: #ef4444;
    }
    .actions { display: flex; gap: 1rem; flex-wrap: wrap; }
    .btn-primary, .btn-secondary {
      padding: 0.75rem 1.5rem;
      border-radius: 8px;
      text-decoration: none;
      font-size: 0.9375rem;
      font-weight: 600;
      text-align: center;
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
export class AdminDashboardComponent implements OnInit {
  readonly loading = signal(true);
  readonly pendingCount = signal(0);
  readonly syncStatus = signal<SyncRunResponse | null>(null);

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    let completed = 0;
    const checkDone = () => { if (++completed >= 2) this.loading.set(false); };

    this.api.getPendingAccounts().subscribe({
      next: (accounts) => { this.pendingCount.set(accounts.length); checkDone(); },
      error: () => checkDone()
    });

    this.api.getSyncStatus().subscribe({
      next: (status) => { this.syncStatus.set(status); checkDone(); },
      error: () => checkDone()
    });
  }
}
