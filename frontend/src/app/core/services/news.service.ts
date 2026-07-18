import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NewsHeadline } from '../models/news.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class NewsService {
  constructor(private http: HttpClient) {}

  headlines(category?: string): Observable<NewsHeadline[]> {
    const query = category ? `?category=${encodeURIComponent(category)}` : '';
    return this.http.get<NewsHeadline[]>(`${environment.apiUrl}/news/headlines${query}`);
  }
}
