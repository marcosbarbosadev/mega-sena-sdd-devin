import { Component } from '@angular/core';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  template: `
    <div class="notifications-container" aria-live="polite">
      @for (n of notificationService.notifications(); track n.id) {
        <div class="notification" [class]="'notification-' + n.type" role="alert">
          <span class="notification-message">{{ n.message }}</span>
          <button class="notification-close" (click)="notificationService.dismiss(n.id)" aria-label="Fechar">×</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .notifications-container {
      position: fixed;
      top: 70px;
      right: 1rem;
      z-index: 300;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      max-width: 400px;
    }
    .notification {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.875rem 1rem;
      border-radius: 8px;
      font-size: 0.875rem;
      box-shadow: 0 4px 16px rgba(0,0,0,0.12);
      animation: slideIn 0.3s ease-out;
    }
    @keyframes slideIn {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
    .notification-success { background: #d1fae5; color: #065f46; border: 1px solid #a7f3d0; }
    .notification-error { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
    .notification-info { background: #dbeafe; color: #1e40af; border: 1px solid #bfdbfe; }
    .notification-warning { background: #fef3c7; color: #92400e; border: 1px solid #fde68a; }
    .notification-close {
      background: none;
      border: none;
      font-size: 1.25rem;
      cursor: pointer;
      color: inherit;
      opacity: 0.6;
      line-height: 1;
      padding: 0;
    }
    .notification-close:hover { opacity: 1; }
  `]
})
export class NotificationsComponent {
  constructor(public notificationService: NotificationService) {}
}
