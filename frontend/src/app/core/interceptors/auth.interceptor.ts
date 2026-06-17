import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const notifications = inject(NotificationService);
  const token = authService.getToken();

  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        authService.logout();
        router.navigate(['/login']);
        notifications.error('Sessão expirada. Faça login novamente.');
      } else if (error.status === 403) {
        notifications.error('Acesso negado.');
      } else if (error.status === 0) {
        notifications.error('Servidor indisponível. Tente novamente mais tarde.');
      } else if (error.status >= 500) {
        notifications.error('Erro interno do servidor. Tente novamente mais tarde.');
      }
      return throwError(() => error);
    })
  );
};
