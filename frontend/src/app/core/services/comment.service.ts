import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CommentItem } from '../models/comment.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CommentService {
  private readonly baseUrl = `${environment.apiUrl}/comments`;

  constructor(private http: HttpClient) {}

  forPost(postId: number): Observable<CommentItem[]> {
    return this.http.get<CommentItem[]>(`${this.baseUrl}/post/${postId}`);
  }

  add(postId: number, body: string, parentId?: number): Observable<CommentItem> {
    return this.http.post<CommentItem>(`${this.baseUrl}/post/${postId}`, { body, ...(parentId ? { parentId } : {}) });
  }

  delete(commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${commentId}`);
  }
}
