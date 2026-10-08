import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminPost, AdminUser, ModerationComment } from '../models/moderation.model';
import { Page } from '../models/post.model';
import { environment } from '../../../environments/environment';
import { HttpParams } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class ModerationService {
  private base = `${environment.apiUrl}/admin`;
  constructor(private http: HttpClient) {}
  flaggedComments(): Observable<ModerationComment[]> { return this.http.get<ModerationComment[]>(`${this.base}/comments/flagged`); }
  setFlag(id: number, flagged: boolean): Observable<ModerationComment> { return this.http.patch<ModerationComment>(`${this.base}/comments/${id}/flag`, { flagged }); }
  deleteComment(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/comments/${id}`); }
  posts(page = 0, size = 20, query = ''): Observable<Page<AdminPost>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (query.trim()) params = params.set('q', query.trim());
    return this.http.get<Page<AdminPost>>(`${this.base}/posts`, { params });
  }
  publishPost(id: number): Observable<AdminPost> { return this.http.post<AdminPost>(`${this.base}/posts/${id}/publish`, {}); }
  archivePost(id: number): Observable<AdminPost> { return this.http.post<AdminPost>(`${this.base}/posts/${id}/archive`, {}); }
  deletePost(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/posts/${id}`); }
  users(query = ''): Observable<AdminUser[]> {
    const params = query.trim() ? new HttpParams().set('q', query.trim()) : new HttpParams();
    return this.http.get<AdminUser[]>(`${this.base}/users`, { params });
  }
  setEnabled(id: number, enabled: boolean): Observable<AdminUser> { return this.http.patch<AdminUser>(`${this.base}/users/${id}/enabled`, { flagged: enabled }); }
  setRole(id: number, role: string): Observable<AdminUser> { return this.http.patch<AdminUser>(`${this.base}/users/${id}/role`, { role }); }
  deleteUser(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/users/${id}`); }
}
