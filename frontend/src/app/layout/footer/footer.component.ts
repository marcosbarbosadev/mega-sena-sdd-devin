import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `
    <footer class="footer">
      <p>Mega Sena Manager &copy; {{ year }} — Projeto educacional</p>
    </footer>
  `,
  styles: [`
    .footer {
      text-align: center;
      padding: 1rem;
      color: #888;
      font-size: 0.8125rem;
      border-top: 1px solid #eee;
      margin-top: auto;
    }
  `]
})
export class FooterComponent {
  year = new Date().getFullYear();
}
