import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminUser, ModerationComment } from '../models/moderation.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ModerationService {
  private base = `${environment.apiUrl}/admin`;
  constructor(private http: HttpClient) {}
  flaggedComments(): Observable<ModerationComment[]> { return this.http.get<ModerationComment[]>(`${this.base}/comments/flagged`); }
  setFlag(id: number, flagged: boolean): Observable<ModerationComment> { return this.http.patch<ModerationComment>(`${this.base}/comments/${id}/flag`, { flagged }); }
  deleteComment(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/comments/${id}`); }
  users(): Observable<AdminUser[]> { return this.http.get<AdminUser[]>(`${this.base}/users`); }
  setEnabled(id: number, enabled: boolean): Observable<AdminUser> { return this.http.patch<AdminUser>(`${this.base}/users/${id}/enabled`, { flagged: enabled }); }
}
