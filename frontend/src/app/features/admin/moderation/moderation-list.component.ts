import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { ContaPendenteResponse } from '../../../core/models';

@Component({
  selector: 'app-moderation-list',
  standalone: true,
  imports: [DatePipe, FormsModule, LoadingComponent],
  template: `
    <div class="mod-container">
      <h1>Moderação de Contas</h1>

      @if (loading()) {
        <app-loading />
      } @else if (accounts().length === 0) {
        <div class="empty-state">
          <span class="empty-icon">✅</span>
          <h2>Nenhuma conta pendente</h2>
          <p>Todas as contas foram processadas.</p>
        </div>
      } @else {
        <div class="accounts-list">
          @for (account of accounts(); track account.id) {
            <div class="account-card">
              <div class="account-info">
                <span class="account-email">{{ account.email }}</span>
                <div class="account-meta">
                  <span class="meta-item">
                    {{ account.metodoLogin === 'GOOGLE' ? '🔵 Google' : '📧 E-mail' }}
                  </span>
                  <span class="meta-item">{{ account.criadoEm | date:'dd/MM/yyyy HH:mm' }}</span>
                </div>
              </div>
              <div class="account-actions">
                <button class="btn-approve" (click)="approve(account)" [disabled]="processing()">
                  Aprovar
                </button>
                <button class="btn-reject" (click)="openRejectModal(account)" [disabled]="processing()">
                  Reprovar
                </button>
              </div>
            </div>
          }
        </div>
      }

      @if (rejectTarget()) {
        <div class="modal-overlay" (click)="rejectTarget.set(null)">
          <div class="modal" (click)="$event.stopPropagation()">
            <h3>Reprovar Conta</h3>
            <p>Conta: <strong>{{ rejectTarget()!.email }}</strong></p>
            <div class="form-group">
              <label for="motivo">Motivo (obrigatório):</label>
              <textarea
                id="motivo"
                [(ngModel)]="rejectReason"
                rows="3"
                placeholder="Informe o motivo da reprovação..."
              ></textarea>
            </div>
            @if (rejectError()) {
              <div class="error-banner" role="alert">{{ rejectError() }}</div>
            }
            <div class="modal-actions">
              <button class="btn-cancel" (click)="rejectTarget.set(null)">Cancelar</button>
              <button class="btn-confirm-reject" (click)="reject()" [disabled]="!rejectReason.trim() || processing()">
                Confirmar Reprovação
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .mod-container { padding: 2rem 1rem; max-width: 800px; margin: 0 auto; }
    h1 { margin: 0 0 1.5rem; font-size: 1.375rem; color: #1a1a1a; }
    .empty-state {
      text-align: center;
      padding: 3rem;
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
    }
    .empty-icon { font-size: 3rem; }
    .empty-state h2 { margin: 1rem 0 0.5rem; }
    .empty-state p { color: #6b7280; }
    .accounts-list { display: flex; flex-direction: column; gap: 0.75rem; }
    .account-card {
      background: white;
      border-radius: 12px;
      padding: 1.25rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
    }
    .account-email { font-weight: 600; color: #1a1a1a; display: block; }
    .account-meta {
      display: flex;
      gap: 1rem;
      margin-top: 0.25rem;
    }
    .meta-item { font-size: 0.8125rem; color: #6b7280; }
    .account-actions { display: flex; gap: 0.5rem; flex-shrink: 0; }
    .btn-approve {
      padding: 0.5rem 1rem;
      background: #209869;
      color: white;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 600;
      font-size: 0.8125rem;
    }
    .btn-approve:hover:not(:disabled) { background: #1a7d56; }
    .btn-reject {
      padding: 0.5rem 1rem;
      background: white;
      color: #ef4444;
      border: 1px solid #ef4444;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 600;
      font-size: 0.8125rem;
    }
    .btn-reject:hover:not(:disabled) { background: #fef2f2; }
    .btn-approve:disabled, .btn-reject:disabled { opacity: 0.5; cursor: not-allowed; }
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
      max-width: 480px;
      width: 90%;
    }
    .modal h3 { margin: 0 0 0.5rem; }
    .modal p { color: #6b7280; font-size: 0.875rem; margin: 0 0 1rem; }
    .form-group { margin-bottom: 1rem; }
    .form-group label {
      display: block;
      font-size: 0.8125rem;
      font-weight: 500;
      color: #374151;
      margin-bottom: 0.375rem;
    }
    .form-group textarea {
      width: 100%;
      padding: 0.625rem;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      font-size: 0.9375rem;
      resize: vertical;
      font-family: inherit;
      box-sizing: border-box;
    }
    .form-group textarea:focus {
      outline: none;
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
      margin-bottom: 1rem;
    }
    .modal-actions { display: flex; gap: 1rem; justify-content: flex-end; }
    .btn-cancel {
      padding: 0.625rem 1.25rem;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      background: white;
      cursor: pointer;
    }
    .btn-confirm-reject {
      padding: 0.625rem 1.25rem;
      background: #ef4444;
      color: white;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 600;
    }
    .btn-confirm-reject:hover:not(:disabled) { background: #dc2626; }
    .btn-confirm-reject:disabled { opacity: 0.5; cursor: not-allowed; }
    @media (max-width: 640px) {
      .account-card { flex-direction: column; align-items: flex-start; }
      .account-actions { width: 100%; }
      .account-actions button { flex: 1; }
    }
  `]
})
export class ModerationListComponent implements OnInit {
  readonly loading = signal(true);
  readonly processing = signal(false);
  readonly accounts = signal<ContaPendenteResponse[]>([]);
  readonly rejectTarget = signal<ContaPendenteResponse | null>(null);
  readonly rejectError = signal('');
  rejectReason = '';

  constructor(
    private api: ApiService,
    private notifications: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadAccounts();
  }

  approve(account: ContaPendenteResponse): void {
    this.processing.set(true);
    this.api.approveAccount(account.id).subscribe({
      next: () => {
        this.accounts.update(list => list.filter(a => a.id !== account.id));
        this.notifications.success(`Conta ${account.email} aprovada.`);
        this.processing.set(false);
      },
      error: () => {
        this.notifications.error('Erro ao aprovar conta.');
        this.processing.set(false);
      }
    });
  }

  openRejectModal(account: ContaPendenteResponse): void {
    this.rejectTarget.set(account);
    this.rejectReason = '';
    this.rejectError.set('');
  }

  reject(): void {
    const account = this.rejectTarget();
    if (!account || !this.rejectReason.trim()) {
      this.rejectError.set('Motivo é obrigatório.');
      return;
    }
    this.processing.set(true);
    this.api.rejectAccount(account.id, { motivo: this.rejectReason.trim() }).subscribe({
      next: () => {
        this.accounts.update(list => list.filter(a => a.id !== account.id));
        this.rejectTarget.set(null);
        this.notifications.success(`Conta ${account.email} reprovada.`);
        this.processing.set(false);
      },
      error: () => {
        this.rejectError.set('Erro ao reprovar conta.');
        this.processing.set(false);
      }
    });
  }

  private loadAccounts(): void {
    this.api.getPendingAccounts().subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
