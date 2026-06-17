import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { unauthGuard } from './core/guards/unauth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  {
    path: 'login',
    canActivate: [unauthGuard],
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'pending',
    loadComponent: () => import('./features/auth/pending-state/pending-state.component').then(m => m.PendingStateComponent)
  },
  {
    path: 'rejected',
    loadComponent: () => import('./features/auth/pending-state/pending-state.component').then(m => m.PendingStateComponent)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./features/auth/profile/profile.component').then(m => m.ProfileComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/games/dashboard/user-dashboard.component').then(m => m.UserDashboardComponent)
  },
  {
    path: 'games',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/games/list/games-list.component').then(m => m.GamesListComponent)
      },
      {
        path: 'create',
        loadComponent: () => import('./features/games/create/game-create.component').then(m => m.GameCreateComponent)
      },
      {
        path: 'edit/:id',
        loadComponent: () => import('./features/games/edit/game-edit.component').then(m => m.GameEditComponent)
      }
    ]
  },
  {
    path: 'conference',
    canActivate: [authGuard],
    loadComponent: () => import('./features/conference/list/conference-list.component').then(m => m.ConferenceListComponent)
  },
  {
    path: 'concursos',
    canActivate: [authGuard],
    loadComponent: () => import('./features/concursos/latest/concurso-latest.component').then(m => m.ConcursoLatestComponent)
  },
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/admin/dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)
      },
      {
        path: 'moderation',
        loadComponent: () => import('./features/admin/moderation/moderation-list.component').then(m => m.ModerationListComponent)
      },
      {
        path: 'sync',
        loadComponent: () => import('./features/admin/sync/sync-status.component').then(m => m.SyncStatusComponent)
      }
    ]
  },
  { path: '**', redirectTo: '/login' }
];
