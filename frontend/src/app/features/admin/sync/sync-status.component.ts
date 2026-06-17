import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { SyncRunResponse } from '../../../core/models';

@Component({
  selector: 'app-sync-status',
  standalone: true,
  imports: [DatePipe, LoadingComponent],
  template: `
    <div class="sync-container">
      <h1>Sincronização de Concursos</h1>

      @if (loading()) {
        <app-loading />
      } @else {
        @if (syncRun()) {
          <div class="status-card">
            <h2>Última Execução</h2>
            <div class="status-fields">
              <div class="field">
                <span class="label">Status</span>
                <span class="badge" [class]="'status-' + syncRun()!.status.toLowerCase()">
                  {{ syncRun()!.status }}
                </span>
              </div>
              <div class="field">
                <span class="label">Origem</span>
                <span class="value">{{ syncRun()!.origem === 'MANUAL' ? 'Manual' : 'Agendada' }}</span>
              </div>
              <div class="field">
                <span class="label">Iniciado em</span>
                <span class="value">{{ syncRun()!.iniciadoEm | date:'dd/MM/yyyy HH:mm:ss' }}</span>
              </div>
              @if (syncRun()!.finalizadoEm) {
                <div class="field">
                  <span class="label">Finalizado em</span>
                  <span class="value">{{ syncRun()!.finalizadoEm | date:'dd/MM/yyyy HH:mm:ss' }}</span>
                </div>
              }
              <div class="field">
                <span class="label">Concursos afetados</span>
                <span class="value">{{ syncRun()!.concursosAfetados }}</span>
              </div>
              @if (syncRun()!.mensagemErro) {
                <div class="field error">
                  <span class="label">Erro</span>
                  <span class="value">{{ syncRun()!.mensagemErro }}</span>
                </div>
              }
            </div>
          </div>
        } @else {
          <div class="empty-state">
            <p>Nenhuma sincronização registrada ainda.</p>
          </div>
        }

        <div class="action-section">
          <button
            class="btn-sync"
            (click)="triggerSync()"
            [disabled]="syncing()"
          >
            @if (syncing()) {
              <span class="spinner-inline"></span>
              Sincronizando...
            } @else {
              🔄 Sincronizar Agora
            }
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .sync-container { padding: 2rem 1rem; max-width: 700px; margin: 0 auto; }
    h1 { margin: 0 0 1.5rem; font-size: 1.375rem; color: #1a1a1a; }
    .status-card {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      box-shadow: 0 2px 12px rgba(0,0,0,0.06);
      margin-bottom: 1.5rem;
    }
    .status-card h2 { margin: 0 0 1rem; font-size: 1.125rem; color: #1a1a1a; }
    .status-fields { display: flex; flex-direction: column; gap: 0.75rem; }
    .field {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.5rem 0;
      border-bottom: 1px solid #f3f4f6;
    }
    .field.error .value { color: #ef4444; }
    .label { font-size: 0.875rem; color: #6b7280; }
    .value { font-size: 0.9375rem; color: #1a1a1a; font-weight: 500; }
    .badge {
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
    }
    .status-sucesso, .status-concluido { background: #d1fae5; color: #065f46; }
    .status-erro, .status-falha { background: #fef2f2; color: #991b1b; }
    .status-executando, .status-em_andamento { background: #dbeafe; color: #1e40af; }
    .empty-state {
      text-align: center;
      padding: 2rem;
      background: white;
      border-radius: 12px;
      color: #6b7280;
    }
    .action-section { text-align: center; }
    .btn-sync {
      padding: 0.875rem 2rem;
      background: #209869;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      transition: background 0.2s;
    }
    .btn-sync:hover:not(:disabled) { background: #1a7d56; }
    .btn-sync:disabled { opacity: 0.6; cursor: not-allowed; }
    .spinner-inline {
      width: 16px;
      height: 16px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class SyncStatusComponent implements OnInit {
  readonly loading = signal(true);
  readonly syncing = signal(false);
  readonly syncRun = signal<SyncRunResponse | null>(null);

  constructor(
    private api: ApiService,
    private notifications: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadStatus();
  }

  triggerSync(): void {
    this.syncing.set(true);
    this.api.triggerSync().subscribe({
      next: (run) => {
        this.syncRun.set(run);
        this.syncing.set(false);
        this.notifications.success('Sincronização iniciada com sucesso!');
        setTimeout(() => this.loadStatus(), 5000);
      },
      error: (err) => {
        const msg = err.error?.mensagem ?? 'Erro ao iniciar sincronização.';
        this.notifications.error(msg);
        this.syncing.set(false);
      }
    });
  }

  private loadStatus(): void {
    this.api.getSyncStatus().subscribe({
      next: (run) => {
        this.syncRun.set(run);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
