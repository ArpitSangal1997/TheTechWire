import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NewsService } from '../../../core/services/news.service';
import { NewsHeadline } from '../../../core/models/news.model';
import { SponsorshipService } from '../../../core/services/sponsorship.service';
import { Sponsorship } from '../../../core/models/sponsorship.model';

@Component({
  selector: 'wb-ticker',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './ticker.component.html',
  styleUrl: './ticker.component.scss'
})
export class TickerComponent implements OnInit {
  headlines: NewsHeadline[] = [];
  tickerAds: Sponsorship[] = [];

  constructor(private newsService: NewsService, private sponsorshipService: SponsorshipService) {}

  ngOnInit(): void {
    this.newsService.headlines().subscribe({
      next: (items) => (this.headlines = items.slice(0, 12)),
      error: () => (this.headlines = [])
    });
    this.sponsorshipService.active('NEWS_TICKER').subscribe((ads) => (this.tickerAds = ads));
  }

  openAd(ad: Sponsorship, event: Event): void {
    event.preventDefault();
    this.sponsorshipService.recordClick(ad.id).subscribe();
    window.open(ad.targetUrl, '_blank', 'noopener');
  }

  open(url: string): void {
    window.open(url, '_blank', 'noopener');
  }
}
