import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FriendEntry, FriendUser } from '../models/friend.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FriendService {
  private readonly base = `${environment.apiUrl}/friends`;

  constructor(private http: HttpClient) {}

  list(): Observable<FriendEntry[]> {
    return this.http.get<FriendEntry[]>(this.base);
  }

  searchUsers(query: string): Observable<FriendUser[]> {
    return this.http.get<FriendUser[]>(`${this.base}/search`, { params: new HttpParams().set('q', query) });
  }

  request(userId: number): Observable<FriendUser> {
    return this.http.post<FriendUser>(`${this.base}/${userId}`, {});
  }

  accept(userId: number): Observable<FriendUser> {
    return this.http.post<FriendUser>(`${this.base}/${userId}/accept`, {});
  }

  remove(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${userId}`);
  }
}
