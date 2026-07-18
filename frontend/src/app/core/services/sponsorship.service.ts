import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Sponsorship } from '../models/sponsorship.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SponsorshipService {
  constructor(private http: HttpClient) {}

  active(placement: Sponsorship['placement']): Observable<Sponsorship[]> {
    return this.http.get<Sponsorship[]>(`${environment.apiUrl}/sponsorships/active?placement=${placement}`);
  }

  recordClick(id: number): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/sponsorships/${id}/click`, {});
  }

  // --- admin ---
  listAll(): Observable<Sponsorship[]> {
    return this.http.get<Sponsorship[]>(`${environment.apiUrl}/admin/sponsorships`);
  }

  create(payload: Partial<Sponsorship>): Observable<Sponsorship> {
    return this.http.post<Sponsorship>(`${environment.apiUrl}/admin/sponsorships`, payload);
  }

  update(id: number, payload: Partial<Sponsorship>): Observable<Sponsorship> {
    return this.http.put<Sponsorship>(`${environment.apiUrl}/admin/sponsorships/${id}`, payload);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/admin/sponsorships/${id}`);
  }
}
