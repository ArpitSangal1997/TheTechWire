import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthUser } from '../models/user.model';
import { environment } from '../../../environments/environment';

const STORAGE_KEY = 'wireblog.auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUserSignal = signal<AuthUser | null>(this.loadFromStorage());

  readonly currentUser = computed(() => this.currentUserSignal());
  readonly isLoggedIn = computed(() => this.currentUserSignal() !== null);
  readonly isAdmin = computed(() => this.currentUserSignal()?.role === 'ADMIN');

  constructor(private http: HttpClient) {}

  register(payload: { displayName: string; handle: string; email: string; password: string; avatarUrl?: string }): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${environment.apiUrl}/auth/register`, payload).pipe(
      tap((user) => this.setSession(user))
    );
  }

  login(payload: { email: string; password: string }): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${environment.apiUrl}/auth/login`, payload).pipe(
      tap((user) => this.setSession(user))
    );
  }

  me(): Observable<AuthUser> {
    return this.http.get<AuthUser>(`${environment.apiUrl}/users/me`).pipe(
      tap((user) => this.setSession(user))
    );
  }

  updateProfile(payload: { displayName: string; bio?: string; avatarUrl?: string }): Observable<AuthUser> {
    return this.http.patch<AuthUser>(`${environment.apiUrl}/users/me`, payload).pipe(
      tap((user) => this.setSession(user))
    );
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.currentUserSignal.set(null);
  }

  getToken(): string | null {
    return this.currentUserSignal()?.token ?? null;
  }

  private setSession(user: AuthUser): void {
    if (!user.token) {
      this.logout();
      return;
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    this.currentUserSignal.set(user);
  }

  private loadFromStorage(): AuthUser | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  }
}
