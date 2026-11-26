import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login/login.page').then((component) => component.LoginPage),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/register/register.page').then((component) => component.RegisterPage),
  },
  {
    path: 'verify-email',
    loadComponent: () =>
      import('./features/auth/verify-email/verify-email.page').then(
        (component) => component.VerifyEmailPage,
      ),
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password.page').then(
        (component) => component.ForgotPasswordPage,
      ),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password.page').then(
        (component) => component.ResetPasswordPage,
      ),
  },
  {
    path: 'auth/oauth/callback/:provider',
    loadComponent: () =>
      import('./features/auth/oauth-callback/oauth-callback.page').then(
        (component) => component.OAuthCallbackPage,
      ),
  },
  {
    path: 'history',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/progress/history.page').then((component) => component.HistoryPage),
  },
  {
    path: 'history/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/progress/session-detail.page').then(
        (component) => component.SessionDetailPage,
      ),
  },
  {
    path: 'records',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/progress/records.page').then((component) => component.RecordsPage),
  },
  {
    path: 'stats',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/progress/stats.page').then((component) => component.StatsPage),
  },
  {
    path: 'body',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/body/body.page').then((component) => component.BodyPage),
  },
  {
    path: 'routines',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/routines/routines.page').then((component) => component.RoutinesPage),
  },
  {
    path: 'routines/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/routines/routine-builder.page').then(
        (component) => component.RoutineBuilderPage,
      ),
  },
  {
    path: 'routines/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/routines/routine-builder.page').then(
        (component) => component.RoutineBuilderPage,
      ),
  },
  {
    path: 'catalog',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/catalog/catalog.page').then((component) => component.CatalogPage),
  },
  {
    path: 'training/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/training/training.page').then((component) => component.TrainingPage),
  },
  {
    path: 'catalog/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/catalog/exercise-detail/exercise-detail.page').then(
        (component) => component.ExerciseDetailPage,
      ),
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/settings/settings.page').then((component) => component.SettingsPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/home/home.page').then((component) => component.HomePage),
  },
  { path: '**', redirectTo: '' },
];
