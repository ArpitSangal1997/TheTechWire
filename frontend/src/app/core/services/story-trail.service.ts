import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/post.model';
import { StoryTrailDetail, StoryTrailSummary } from '../models/story-trail.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class StoryTrailService {
  private readonly baseUrl = `${environment.apiUrl}/trails`;

  constructor(private http: HttpClient) {}

  list(page = 0, size = 12): Observable<Page<StoryTrailSummary>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<StoryTrailSummary>>(this.baseUrl, { params });
  }

  get(slug: string): Observable<StoryTrailDetail> {
    return this.http.get<StoryTrailDetail>(`${this.baseUrl}/${slug}`);
  }
}
