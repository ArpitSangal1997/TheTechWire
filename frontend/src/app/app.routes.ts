import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent) },
  { path: 'post/:slug', loadComponent: () => import('./features/post-detail/post-detail.component').then(m => m.PostDetailComponent) },
  { path: 'write', loadComponent: () => import('./features/post-editor/post-editor.component').then(m => m.PostEditorComponent), canActivate: [authGuard] },
  { path: 'write/:id', loadComponent: () => import('./features/post-editor/post-editor.component').then(m => m.PostEditorComponent), canActivate: [authGuard] },
  { path: 'my-stories', loadComponent: () => import('./features/my-stories/my-stories.component').then(m => m.MyStoriesComponent), canActivate: [authGuard] },
  { path: 'gk', loadComponent: () => import('./features/news/news-feed.component').then(m => m.NewsFeedComponent) },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
  { path: 'forgot-password', loadComponent: () => import('./features/auth/account-action/account-action.component').then(m => m.AccountActionComponent) },
  { path: 'reset-password', loadComponent: () => import('./features/auth/account-action/account-action.component').then(m => m.AccountActionComponent) },
  { path: 'verify-email', loadComponent: () => import('./features/auth/account-action/account-action.component').then(m => m.AccountActionComponent) },
  { path: 'admin', loadComponent: () => import('./features/admin/admin-dashboard.component').then(m => m.AdminDashboardComponent), canActivate: [authGuard, adminGuard] },
  { path: '**', redirectTo: '' }
];
