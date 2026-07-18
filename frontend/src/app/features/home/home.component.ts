import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PostService } from '../../core/services/post.service';
import { PostSummary } from '../../core/models/post.model';
import { ShareDialogComponent } from '../../shared/components/share-dialog/share-dialog.component';
import { SponsorshipService } from '../../core/services/sponsorship.service';
import { Sponsorship } from '../../core/models/sponsorship.model';

@Component({
  selector: 'wb-home',
  standalone: true,
  imports: [CommonModule, RouterModule, ShareDialogComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  featured?: PostSummary;
  posts: PostSummary[] = [];
  loading = true;
  page = 0;
  hasMore = true;
  activeShare?: PostSummary;
  feedAds: Sponsorship[] = [];
  sidebarAds: Sponsorship[] = [];

  constructor(private postService: PostService, private sponsorshipService: SponsorshipService) {}

  ngOnInit(): void {
    this.load();
    this.loadAds();
  }

  load(): void {
    this.loading = true;
    this.postService.list({ page: this.page, size: 13 }).subscribe({
      next: (res) => {
        const all = res.content;
        if (this.page === 0) {
          this.featured = all[0];
          this.posts = all.slice(1);
        } else {
          this.posts = [...this.posts, ...all];
        }
        this.hasMore = !res.last;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  loadMore(): void {
    this.page++;
    this.load();
  }

  loadAds(): void {
    this.sponsorshipService.active('FEED_BANNER').subscribe((ads) => (this.feedAds = ads));
    this.sponsorshipService.active('SIDEBAR').subscribe((ads) => (this.sidebarAds = ads));
  }

  openAd(ad: Sponsorship, event: Event): void {
    event.preventDefault();
    this.sponsorshipService.recordClick(ad.id).subscribe();
    window.open(ad.targetUrl, '_blank', 'noopener');
  }

  openShare(post: PostSummary, evt: Event): void {
    evt.preventDefault();
    evt.stopPropagation();
    this.activeShare = post;
  }
}
