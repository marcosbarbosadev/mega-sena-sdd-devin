import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="header">
      <div class="header-content">
        <a routerLink="/" class="logo">
          <span class="logo-icon">🍀</span>
          <span class="logo-text">Mega Sena Manager</span>
        </a>

        @if (authService.isAuthenticated()) {
          <nav class="nav" aria-label="Navegação principal">
            @if (!authService.isAdmin()) {
              <a routerLink="/dashboard" routerLinkActive="active" class="nav-link">Dashboard</a>
              <a routerLink="/games" routerLinkActive="active" class="nav-link">Meus Jogos</a>
              <a routerLink="/conference" routerLinkActive="active" class="nav-link">Conferência</a>
              <a routerLink="/concursos" routerLinkActive="active" class="nav-link">Concursos</a>
            }
            @if (authService.isAdmin()) {
              <a routerLink="/admin" routerLinkActive="active" class="nav-link">Admin</a>
              <a routerLink="/admin/moderation" routerLinkActive="active" class="nav-link">Moderação</a>
              <a routerLink="/admin/sync" routerLinkActive="active" class="nav-link">Sincronização</a>
            }
          </nav>

          <div class="user-actions">
            <a routerLink="/profile" class="nav-link profile-link">
              {{ authService.user()?.email }}
            </a>
            <button class="btn-logout" (click)="authService.logout()" aria-label="Sair">
              Sair
            </button>
          </div>
        }

        <button class="menu-toggle" (click)="toggleMenu()" aria-label="Menu">
          ☰
        </button>
      </div>

      @if (menuOpen) {
        <div class="mobile-menu">
          @if (authService.isAuthenticated()) {
            @if (!authService.isAdmin()) {
              <a routerLink="/dashboard" class="mobile-link" (click)="toggleMenu()">Dashboard</a>
              <a routerLink="/games" class="mobile-link" (click)="toggleMenu()">Meus Jogos</a>
              <a routerLink="/conference" class="mobile-link" (click)="toggleMenu()">Conferência</a>
              <a routerLink="/concursos" class="mobile-link" (click)="toggleMenu()">Concursos</a>
            }
            @if (authService.isAdmin()) {
              <a routerLink="/admin" class="mobile-link" (click)="toggleMenu()">Admin</a>
              <a routerLink="/admin/moderation" class="mobile-link" (click)="toggleMenu()">Moderação</a>
              <a routerLink="/admin/sync" class="mobile-link" (click)="toggleMenu()">Sincronização</a>
            }
            <a routerLink="/profile" class="mobile-link" (click)="toggleMenu()">Perfil</a>
            <button class="mobile-link logout" (click)="authService.logout()">Sair</button>
          }
        </div>
      }
    </header>
  `,
  styles: [`
    .header {
      background: #209869;
      color: white;
      box-shadow: 0 2px 8px rgba(0,0,0,0.15);
      position: sticky;
      top: 0;
      z-index: 100;
    }
    .header-content {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 1rem;
      height: 60px;
      display: flex;
      align-items: center;
      gap: 2rem;
    }
    .logo {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      text-decoration: none;
      color: white;
      font-weight: 700;
      font-size: 1.125rem;
      white-space: nowrap;
    }
    .logo-icon { font-size: 1.5rem; }
    .nav {
      display: flex;
      gap: 0.25rem;
      flex: 1;
    }
    .nav-link {
      color: rgba(255,255,255,0.85);
      text-decoration: none;
      padding: 0.5rem 0.75rem;
      border-radius: 6px;
      font-size: 0.875rem;
      transition: background 0.2s, color 0.2s;
    }
    .nav-link:hover, .nav-link.active {
      background: rgba(255,255,255,0.15);
      color: white;
    }
    .user-actions {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-left: auto;
    }
    .profile-link {
      font-size: 0.8125rem;
      max-width: 180px;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .btn-logout {
      background: rgba(255,255,255,0.15);
      border: 1px solid rgba(255,255,255,0.3);
      color: white;
      padding: 0.375rem 0.75rem;
      border-radius: 6px;
      cursor: pointer;
      font-size: 0.8125rem;
      transition: background 0.2s;
    }
    .btn-logout:hover { background: rgba(255,255,255,0.25); }
    .menu-toggle {
      display: none;
      background: none;
      border: none;
      color: white;
      font-size: 1.5rem;
      cursor: pointer;
      margin-left: auto;
    }
    .mobile-menu {
      display: none;
      flex-direction: column;
      padding: 0.5rem 1rem 1rem;
    }
    .mobile-link {
      display: block;
      color: white;
      text-decoration: none;
      padding: 0.75rem;
      border-radius: 6px;
      font-size: 0.9375rem;
      background: none;
      border: none;
      text-align: left;
      cursor: pointer;
      width: 100%;
    }
    .mobile-link:hover { background: rgba(255,255,255,0.15); }
    .mobile-link.logout { color: #fca5a5; }

    @media (max-width: 768px) {
      .nav, .user-actions { display: none; }
      .menu-toggle { display: block; }
      .mobile-menu { display: flex; }
    }
  `]
})
export class HeaderComponent {
  menuOpen = false;
  constructor(public authService: AuthService) {}
  toggleMenu(): void { this.menuOpen = !this.menuOpen; }
}
