import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './layout/header/header.component';
import { FooterComponent } from './layout/footer/footer.component';
import { NotificationsComponent } from './shared/components/notifications/notifications.component';
import { AuthService } from './core/services/auth.service';
import { LoadingComponent } from './shared/components/loading/loading.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent, NotificationsComponent, LoadingComponent],
  template: `
    @if (authService.loading()) {
      <div class="app-loading">
        <app-loading message="Inicializando..." />
      </div>
    } @else {
      <div class="app-layout">
        <app-header />
        <main class="main-content">
          <router-outlet />
        </main>
        <app-footer />
      </div>
    }
    <app-notifications />
  `,
  styles: [`
    .app-layout {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }
    .main-content {
      flex: 1;
      background: #f9fafb;
    }
    .app-loading {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
    }
  `]
})
export class AppComponent {
  constructor(public authService: AuthService) {}
}
