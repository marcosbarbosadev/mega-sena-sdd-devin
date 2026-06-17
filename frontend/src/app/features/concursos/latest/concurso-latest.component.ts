import { Component, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { ConcursoResponse } from '../../../core/models';

@Component({
  selector: 'app-concurso-latest',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, LoadingComponent],
  template: `
    <div class="concurso-container">
      <h1>Concursos</h1>

      @if (loading()) {
        <app-loading />
      } @else if (!concurso()) {
        <div class="empty-state">
          <span class="empty-icon">🎱</span>
          <h2>Nenhum concurso sincronizado</h2>
          <p>Aguarde a sincronização dos resultados.</p>
        </div>
      } @else {
        <div class="concurso-card">
          <div class="concurso-header">
            <h2>Concurso {{ concurso()!.numero }}</h2>
            <span class="date">{{ concurso()!.dataSorteio | date:'dd/MM/yyyy' }}</span>
          </div>

          <div class="dezenas-section">
            <p class="label">Dezenas Sorteadas:</p>
            <div class="dezenas">
              @for (d of concurso()!.dezenas; track d) {
                <span class="dezena-ball">{{ d < 10 ? '0' + d : d }}</span>
              }
            </div>
          </div>

          <div class="premio-section">
            <span class="premio-label">Prêmio estimado:</span>
            <span class="premio-value">R$ {{ concurso()!.valorPremio | number:'1.2-2':'pt-BR' }}</span>
          </div>
        </div>

        <div class="search-section">
          <h3>Buscar Concurso</h3>
          <div class="search-row">
            <input
              type="number"
              [(ngModel)]="searchNumber"
              placeholder="Número do concurso"
              (keyup.enter)="searchConcurso()"
            />
            <button class="btn-search" (click)="searchConcurso()">Buscar</button>
          </div>
        </div>

        @if (searchResult()) {
          <div class="concurso-card search-result">
            <div class="concurso-header">
              <h2>Concurso {{ searchResult()!.numero }}</h2>
              <span class="date">{{ searchResult()!.dataSorteio | date:'dd/MM/yyyy' }}</span>
            </div>
            <div class="dezenas-section">
              <p class="label">Dezenas Sorteadas:</p>
              <div class="dezenas">
                @for (d of searchResult()!.dezenas; track d) {
                  <span class="dezena-ball">{{ d < 10 ? '0' + d : d }}</span>
                }
              </div>
            </div>
            <div class="premio-section">
              <span class="premio-label">Prêmio:</span>
              <span class="premio-value">R$ {{ searchResult()!.valorPremio | number:'1.2-2':'pt-BR' }}</span>
            </div>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .concurso-container { padding: 2rem 1rem; max-width: 700px; margin: 0 auto; }
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
    .concurso-card {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      box-shadow: 0 2px 12px rgba(0,0,0,0.06);
      margin-bottom: 1.5rem;
    }
    .concurso-card.search-result { border: 2px solid #209869; }
    .concurso-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.25rem;
    }
    .concurso-header h2 { margin: 0; font-size: 1.125rem; color: #1a1a1a; }
    .date { color: #6b7280; font-size: 0.875rem; }
    .dezenas-section { margin-bottom: 1.25rem; }
    .label { font-size: 0.8125rem; color: #9ca3af; margin: 0 0 0.75rem; }
    .dezenas { display: flex; flex-wrap: wrap; gap: 0.625rem; }
    .dezena-ball {
      width: 52px;
      height: 52px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      background: linear-gradient(135deg, #209869, #16a34a);
      color: white;
      font-weight: 700;
      font-size: 1.125rem;
      box-shadow: 0 2px 6px rgba(32,152,105,0.3);
    }
    .premio-section {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 1rem;
      border-top: 1px solid #f3f4f6;
    }
    .premio-label { color: #6b7280; font-size: 0.875rem; }
    .premio-value { color: #209869; font-size: 1.25rem; font-weight: 700; }
    .search-section {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
      margin-bottom: 1.5rem;
    }
    .search-section h3 { margin: 0 0 1rem; font-size: 1rem; color: #1a1a1a; }
    .search-row { display: flex; gap: 0.75rem; }
    .search-row input {
      flex: 1;
      padding: 0.625rem 0.75rem;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      font-size: 0.9375rem;
    }
    .search-row input:focus {
      outline: none;
      border-color: #209869;
      box-shadow: 0 0 0 3px rgba(32,152,105,0.1);
    }
    .btn-search {
      padding: 0.625rem 1.25rem;
      background: #209869;
      color: white;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 600;
    }
    .btn-search:hover { background: #1a7d56; }
  `]
})
export class ConcursoLatestComponent implements OnInit {
  readonly loading = signal(true);
  readonly concurso = signal<ConcursoResponse | null>(null);
  readonly searchResult = signal<ConcursoResponse | null>(null);
  searchNumber = '';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getLatestConcurso().subscribe({
      next: (c) => {
        this.concurso.set(c);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  searchConcurso(): void {
    const num = parseInt(this.searchNumber, 10);
    if (isNaN(num) || num <= 0) return;
    this.searchResult.set(null);
    this.api.getConcurso(num).subscribe({
      next: (c) => this.searchResult.set(c),
      error: () => this.searchResult.set(null)
    });
  }
}
