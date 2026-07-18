import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SponsorshipService } from '../../core/services/sponsorship.service';
import { Sponsorship } from '../../core/models/sponsorship.model';

@Component({
  selector: 'wb-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit {
  sponsorships: Sponsorship[] = [];
  loading = true;
  showForm = false;
  editingId?: number;

  form: {
    brandName: string;
    headline: string;
    targetUrl: string;
    creativeImageUrl: string;
    placement: Sponsorship['placement'];
    status: Sponsorship['status'];
    startDate: string;
    endDate: string;
  } = this.emptyForm();

  placements: Sponsorship['placement'][] = ['FEED_BANNER', 'SIDEBAR', 'NEWS_TICKER', 'POST_INLINE'];
  statuses: Sponsorship['status'][] = ['DRAFT', 'ACTIVE', 'PAUSED', 'ENDED'];

  constructor(private sponsorshipService: SponsorshipService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.sponsorshipService.listAll().subscribe({
      next: (items) => {
        this.sponsorships = items;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  get totalImpressions(): number {
    return this.sponsorships.reduce((sum, s) => sum + s.impressions, 0);
  }

  get totalClicks(): number {
    return this.sponsorships.reduce((sum, s) => sum + s.clicks, 0);
  }

  get activeDeals(): number {
    return this.sponsorships.filter((s) => s.status === 'ACTIVE').length;
  }

  openNew(): void {
    this.form = this.emptyForm();
    this.editingId = undefined;
    this.showForm = true;
  }

  openEdit(s: Sponsorship): void {
    this.editingId = s.id;
    this.form = {
      brandName: s.brandName,
      headline: s.headline,
      targetUrl: s.targetUrl,
      creativeImageUrl: s.creativeImageUrl ?? '',
      placement: s.placement,
      status: s.status,
      startDate: s.startDate,
      endDate: s.endDate
    };
    this.showForm = true;
  }

  save(): void {
    const payload = { ...this.form };
    const req = this.editingId
      ? this.sponsorshipService.update(this.editingId, payload)
      : this.sponsorshipService.create(payload);

    req.subscribe(() => {
      this.showForm = false;
      this.load();
    });
  }

  remove(s: Sponsorship): void {
    if (!confirm(`Remove the "${s.brandName}" deal?`)) return;
    this.sponsorshipService.remove(s.id).subscribe(() => this.load());
  }

  private emptyForm() {
    const today = new Date().toISOString().slice(0, 10);
    const nextMonth = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10);
    return {
      brandName: '',
      headline: '',
      targetUrl: '',
      creativeImageUrl: '',
      placement: 'FEED_BANNER' as Sponsorship['placement'],
      status: 'DRAFT' as Sponsorship['status'],
      startDate: today,
      endDate: nextMonth
    };
  }
}
