import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    const state = authService.accountState();
    if (state === 'PENDENTE') {
      router.navigate(['/pending']);
      return false;
    }
    if (state === 'REPROVADO') {
      router.navigate(['/rejected']);
      return false;
    }
    return true;
  }

  router.navigate(['/login']);
  return false;
};
