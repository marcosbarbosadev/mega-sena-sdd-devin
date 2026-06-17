import { Component, OnInit, signal } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { ConferenciaResponse } from '../../../core/models';

@Component({
  selector: 'app-conference-list',
  standalone: true,
  imports: [LoadingComponent],
  template: `
    <div class="conf-container">
      <div class="conf-header">
        <h1>Conferência de Jogos</h1>
        <button class="btn-refresh" (click)="load()" [disabled]="loading()" aria-label="Atualizar">
          🔄 Atualizar
        </button>
      </div>

      @if (loading()) {
        <app-loading message="Conferindo jogos..." />
      } @else if (conferences().length === 0) {
        <div class="empty-state">
          <span class="empty-icon">📊</span>
          <h2>Nenhuma conferência disponível</h2>
          <p>Cadastre jogos para ver os resultados aqui.</p>
        </div>
      } @else {
        <div class="conf-grid">
          @for (conf of conferences(); track conf.jogoId) {
            <div class="conf-card" [class]="getCardClass(conf)">
              <div class="conf-top">
                <span class="concurso">Concurso {{ conf.concursoNumero }}</span>
                @if (conf.status === 'CONFERIDO') {
                  <span class="faixa-badge" [class]="'faixa-' + conf.faixa.toLowerCase()">
                    {{ getFaixaLabel(conf.faixa) }}
                  </span>
                } @else {
                  <span class="status-badge awaiting">Aguardando Sorteio</span>
                }
              </div>

              <div class="dezenas-section">
                <p class="dezenas-label">Suas dezenas:</p>
                <div class="dezenas">
                  @for (d of conf.dezenasJogadas; track d) {
                    <span class="dezena"
                      [class.hit]="conf.status === 'CONFERIDO' && conf.dezenasSorteadas.includes(d)"
                    >
                      {{ d < 10 ? '0' + d : d }}
                    </span>
                  }
                </div>
              </div>

              @if (conf.status === 'CONFERIDO') {
                <div class="result-section">
                  <div class="result-row">
                    <span class="result-label">Acertos:</span>
                    <span class="result-value acertos">{{ conf.acertos }}</span>
                  </div>
                  @if (conf.premiado) {
                    <div class="premio-badge">
                      🏆 Premiado!
                    </div>
                  }
                </div>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .conf-container { padding: 2rem 1rem; max-width: 900px; margin: 0 auto; }
    .conf-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
    }
    h1 { margin: 0; font-size: 1.375rem; color: #1a1a1a; }
    .btn-refresh {
      padding: 0.5rem 1rem;
      background: white;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      cursor: pointer;
      font-size: 0.875rem;
    }
    .btn-refresh:hover { background: #f9fafb; }
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
    .conf-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
      gap: 1rem;
    }
    .conf-card {
      background: white;
      border-radius: 12px;
      padding: 1.25rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
      border-left: 4px solid #e5e7eb;
    }
    .conf-card.sena { border-left-color: #f59e0b; background: #fffbeb; }
    .conf-card.quina { border-left-color: #8b5cf6; background: #f5f3ff; }
    .conf-card.quadra { border-left-color: #f97316; background: #fff7ed; }
    .conf-card.awaiting { border-left-color: #9ca3af; }
    .conf-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }
    .concurso { font-size: 0.875rem; font-weight: 600; color: #374151; }
    .faixa-badge, .status-badge {
      padding: 0.2rem 0.625rem;
      border-radius: 9999px;
      font-size: 0.6875rem;
      font-weight: 600;
      text-transform: uppercase;
    }
    .faixa-sena { background: #fef3c7; color: #92400e; }
    .faixa-quina { background: #ede9fe; color: #5b21b6; }
    .faixa-quadra { background: #ffedd5; color: #9a3412; }
    .faixa-nenhuma { background: #f3f4f6; color: #6b7280; }
    .status-badge.awaiting { background: #f3f4f6; color: #6b7280; }
    .dezenas-label { font-size: 0.75rem; color: #9ca3af; margin: 0 0 0.5rem; }
    .dezenas { display: flex; flex-wrap: wrap; gap: 0.375rem; }
    .dezena {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      background: #e5e7eb;
      color: #374151;
      font-weight: 600;
      font-size: 0.8125rem;
    }
    .dezena.hit {
      background: #209869;
      color: white;
    }
    .result-section {
      margin-top: 1rem;
      padding-top: 0.75rem;
      border-top: 1px solid #f3f4f6;
    }
    .result-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .result-label { font-size: 0.875rem; color: #6b7280; }
    .result-value.acertos { font-size: 1.25rem; font-weight: 700; color: #209869; }
    .premio-badge {
      margin-top: 0.75rem;
      text-align: center;
      padding: 0.5rem;
      background: linear-gradient(135deg, #f59e0b, #eab308);
      border-radius: 8px;
      color: white;
      font-weight: 700;
      font-size: 0.9375rem;
    }
  `]
})
export class ConferenceListComponent implements OnInit {
  readonly loading = signal(true);
  readonly conferences = signal<ConferenciaResponse[]>([]);

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.getConferences().subscribe({
      next: (confs) => {
        this.conferences.set(confs);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  getCardClass(conf: ConferenciaResponse): string {
    if (conf.status !== 'CONFERIDO') return 'awaiting';
    return conf.faixa.toLowerCase();
  }

  getFaixaLabel(faixa: string): string {
    const labels: Record<string, string> = {
      SENA: 'Sena',
      QUINA: 'Quina',
      QUADRA: 'Quadra',
      NENHUMA: 'Nenhuma'
    };
    return labels[faixa] ?? faixa;
  }
}
