import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ReaderPulse } from '../models/reader-pulse.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReaderPulseService {
  private readonly baseUrl = `${environment.apiUrl}/pulses/posts`;

  constructor(private http: HttpClient) {}

  get(postId: number): Observable<ReaderPulse> {
    return this.http.get<ReaderPulse>(`${this.baseUrl}/${postId}`);
  }

  vote(postId: number, choice: string): Observable<ReaderPulse> {
    return this.http.post<ReaderPulse>(`${this.baseUrl}/${postId}`, { choice });
  }
}
