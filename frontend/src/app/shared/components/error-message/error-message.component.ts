import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-error-message',
  standalone: true,
  template: `
    <div class="error-container" role="alert">
      <span class="error-icon">⚠</span>
      <p class="error-text">{{ message }}</p>
    </div>
  `,
  styles: [`
    .error-container {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1rem;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 8px;
      color: #991b1b;
    }
    .error-icon { font-size: 1.25rem; }
    .error-text { margin: 0; font-size: 0.875rem; }
  `]
})
export class ErrorMessageComponent {
  @Input() message = 'Ocorreu um erro. Tente novamente.';
}
