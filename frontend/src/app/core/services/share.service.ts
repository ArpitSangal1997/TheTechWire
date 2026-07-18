import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type ShareChannel = 'IN_APP' | 'WHATSAPP' | 'TWITTER' | 'FACEBOOK' | 'LINKEDIN' | 'COPY_LINK' | 'EMAIL';

@Injectable({ providedIn: 'root' })
export class ShareService {
  constructor(private http: HttpClient) {}

  share(postId: number, channel: ShareChannel, sharedToHandle?: string): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/posts/${postId}/share`, { channel, sharedToHandle });
  }

  /** Builds the public, copy-pasteable URL for a post. */
  publicUrl(slug: string): string {
    return `${window.location.origin}/post/${slug}`;
  }
}
