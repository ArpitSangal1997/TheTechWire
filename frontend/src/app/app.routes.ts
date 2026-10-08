import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent) },
  { path: 'trails', loadComponent: () => import('./features/story-trails/story-trails.component').then(m => m.StoryTrailsComponent) },
  { path: 'trails/:slug', loadComponent: () => import('./features/story-trail-detail/story-trail-detail.component').then(m => m.StoryTrailDetailComponent) },
  { path: 'post/:slug', loadComponent: () => import('./features/post-detail/post-detail.component').then(m => m.PostDetailComponent) },
  { path: 'write', loadComponent: () => import('./features/post-editor/post-editor.component').then(m => m.PostEditorComponent), canActivate: [authGuard] },
  { path: 'write/:id', loadComponent: () => import('./features/post-editor/post-editor.component').then(m => m.PostEditorComponent), canActivate: [authGuard] },
  { path: 'my-stories', loadComponent: () => import('./features/my-stories/my-stories.component').then(m => m.MyStoriesComponent), canActivate: [authGuard] },
  { path: 'account', loadComponent: () => import('./features/account/account.component').then(m => m.AccountComponent), canActivate: [authGuard] },
  { path: 'gk', loadComponent: () => import('./features/news/news-feed.component').then(m => m.NewsFeedComponent) },
  { path: 'groups', loadComponent: () => import('./features/groups/groups.component').then(m => m.GroupsComponent) },
  { path: 'groups/:id', loadComponent: () => import('./features/groups/group-detail.component').then(m => m.GroupDetailComponent) },
  { path: 'friends', loadComponent: () => import('./features/friends/friends.component').then(m => m.FriendsComponent), canActivate: [authGuard] },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
  { path: 'admin', loadComponent: () => import('./features/admin/admin-dashboard.component').then(m => m.AdminDashboardComponent), canActivate: [authGuard, adminGuard] },
  { path: '**', redirectTo: '' }
];
