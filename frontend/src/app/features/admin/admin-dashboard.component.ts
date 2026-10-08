import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SponsorshipService } from '../../core/services/sponsorship.service';
import { Sponsorship } from '../../core/models/sponsorship.model';
import { ModerationService } from '../../core/services/moderation.service';
import { AdminPost, AdminUser, ModerationComment } from '../../core/models/moderation.model';
import { Group, GroupService } from '../../core/services/group.service';

@Component({
  selector: 'wb-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit {
  sponsorships: Sponsorship[] = [];
  flaggedComments: ModerationComment[] = [];
  users: AdminUser[] = [];
  posts: AdminPost[] = [];
  groups: Group[] = [];
  userSearch = '';
  postSearch = '';
  groupSearch = '';
  moderationError = '';
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

  constructor(private sponsorshipService: SponsorshipService, private moderation: ModerationService, private groupService: GroupService) {}

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
    this.moderation.flaggedComments().subscribe({ next: (items) => this.flaggedComments = items });
    this.searchUsers('');
    this.searchPosts('');
    this.groupService.list().subscribe({ next: items => this.groups = items, error: () => this.moderationError = 'Groups could not be loaded.' });
  }

  searchUsers(query: string): void {
    this.userSearch = query;
    this.moderation.users(query).subscribe({
      next: items => this.users = items,
      error: () => this.moderationError = 'Users could not be searched.'
    });
  }

  searchPosts(query: string): void {
    this.postSearch = query;
    this.moderation.posts(0, 20, query).subscribe({
      next: result => this.posts = result.content,
      error: () => this.moderationError = 'Posts could not be searched.'
    });
  }

  get filteredGroups(): Group[] {
    const query = this.groupSearch.trim().toLowerCase();
    return query ? this.groups.filter(group => `${group.name} ${group.description ?? ''} ${group.topics.join(' ')}`.toLowerCase().includes(query)) : this.groups;
  }

  deleteGroup(group: Group): void {
    if (!confirm(`Delete “${group.name}” and all of its group posts?`)) return;
    this.groupService.delete(group.id).subscribe({
      next: () => this.groups = this.groups.filter(item => item.id !== group.id),
      error: err => this.moderationError = err?.error?.message ?? 'Group could not be deleted.'
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

  dismissFlag(c: ModerationComment): void {
    this.moderation.setFlag(c.id, false).subscribe(() => this.flaggedComments = this.flaggedComments.filter(x => x.id !== c.id));
  }

  deleteFlaggedComment(c: ModerationComment): void {
    if (!confirm('Delete this comment?')) return;
    this.moderation.deleteComment(c.id).subscribe(() => this.flaggedComments = this.flaggedComments.filter(x => x.id !== c.id));
  }

  toggleUser(user: AdminUser): void {
    this.moderation.setEnabled(user.id, !user.enabled).subscribe(updated => user.enabled = updated.enabled);
  }

  setRole(user: AdminUser, role: string): void {
    if (user.role === role) return;
    if (!confirm(`Change ${user.displayName}'s role to ${role}?`)) return;
    this.moderation.setRole(user.id, role).subscribe((updated) => user.role = updated.role);
  }

  deleteUser(user: AdminUser): void {
    if (!confirm(`Delete ${user.displayName}'s account and all their posts?`)) return;
    this.moderation.deleteUser(user.id).subscribe(() => {
      this.users = this.users.filter((item) => item.id !== user.id);
      this.posts = this.posts.filter((post) => post.author.id !== user.id);
    }, err => {
      this.moderationError = err?.error?.message ?? 'This account could not be deleted.';
    });
  }

  publishPost(post: AdminPost): void {
    this.moderation.publishPost(post.id).subscribe((updated) => Object.assign(post, updated));
  }

  archivePost(post: AdminPost): void {
    this.moderation.archivePost(post.id).subscribe((updated) => Object.assign(post, updated));
  }

  deletePost(post: AdminPost): void {
    if (!confirm(`Delete "${post.title}"?`)) return;
    this.moderation.deletePost(post.id).subscribe(() => this.posts = this.posts.filter((item) => item.id !== post.id));
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
