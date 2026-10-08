import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page, PostDetail, PostRequest, PostSummary } from '../models/post.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PostService {
  private readonly baseUrl = `${environment.apiUrl}/posts`;

  constructor(private http: HttpClient) {}

  list(params: { page?: number; size?: number; tag?: string; q?: string } = {}): Observable<Page<PostSummary>> {
    const httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 12)
      .set('tag', params.tag ?? '')
      .set('q', params.q ?? '');

    return this.http.get<Page<PostSummary>>(this.baseUrl, { params: httpParams });
  }

  mine(page = 0, size = 20): Observable<Page<PostSummary>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<PostSummary>>(`${this.baseUrl}/mine`, { params });
  }

  getBySlug(slug: string): Observable<PostDetail> {
    return this.http.get<PostDetail>(`${this.baseUrl}/${slug}`);
  }

  getForEditing(id: number): Observable<PostDetail> {
    return this.http.get<PostDetail>(`${this.baseUrl}/${id}/edit`);
  }

  create(payload: PostRequest): Observable<PostDetail> {
    return this.http.post<PostDetail>(this.baseUrl, payload);
  }

  shareLink(payload: { url: string; title?: string; note?: string }): Observable<PostDetail> {
    return this.http.post<PostDetail>(`${this.baseUrl}/share-link`, payload);
  }

  updateSharedLink(id: number, payload: { url: string; title?: string; note?: string }): Observable<PostDetail> {
    return this.http.put<PostDetail>(`${this.baseUrl}/${id}/share-link`, payload);
  }

  update(id: number, payload: PostRequest): Observable<PostDetail> {
    return this.http.put<PostDetail>(`${this.baseUrl}/${id}`, payload);
  }

  publish(id: number): Observable<PostSummary> {
    return this.http.post<PostSummary>(`${this.baseUrl}/${id}/publish`, {});
  }

  archive(id: number): Observable<PostSummary> {
    return this.http.post<PostSummary>(`${this.baseUrl}/${id}/archive`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
