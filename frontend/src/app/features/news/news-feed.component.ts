import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NewsService } from '../../core/services/news.service';
import { NewsHeadline } from '../../core/models/news.model';

@Component({
  selector: 'wb-news-feed',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './news-feed.component.html',
  styleUrl: './news-feed.component.scss'
})
export class NewsFeedComponent implements OnInit {
  headlines: NewsHeadline[] = [];
  loading = true;
  error = '';
  categories = ['general', 'business', 'technology', 'sports', 'science', 'health'];
  activeCategory = 'general';

  constructor(private newsService: NewsService) {}

  ngOnInit(): void {
    this.load();
  }

  selectCategory(cat: string): void {
    this.activeCategory = cat;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.newsService.headlines(this.activeCategory === 'general' ? undefined : this.activeCategory).subscribe({
      next: (items) => {
        this.headlines = items;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'The live news service is unavailable. Please try again shortly.';
      }
    });
  }

  open(url: string): void {
    window.open(url, '_blank', 'noopener');
  }
}
