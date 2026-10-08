import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PostService } from '../../core/services/post.service';
import { CommentService } from '../../core/services/comment.service';
import { AuthService } from '../../core/services/auth.service';
import { PostDetail } from '../../core/models/post.model';
import { CommentItem } from '../../core/models/comment.model';
import { ShareDialogComponent } from '../../shared/components/share-dialog/share-dialog.component';
import { SponsorshipService } from '../../core/services/sponsorship.service';
import { Sponsorship } from '../../core/models/sponsorship.model';
import { ReaderPulse } from '../../core/models/reader-pulse.model';
import { ReaderPulseService } from '../../core/services/reader-pulse.service';

@Component({
  selector: 'wb-post-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ShareDialogComponent],
  templateUrl: './post-detail.component.html',
  styleUrl: './post-detail.component.scss'
})
export class PostDetailComponent implements OnInit {
  post?: PostDetail;
  comments: CommentItem[] = [];
  loading = true;
  newComment = '';
  replyTo?: CommentItem;
  replyBody = '';
  showShare = false;
  posting = false;
  inlineAds: Sponsorship[] = [];
  pulse?: ReaderPulse;
  selectedPulse?: string;
  pulseLoading = false;

  constructor(
    private route: ActivatedRoute,
    private postService: PostService,
    private commentService: CommentService,
    private sponsorshipService: SponsorshipService,
    private readerPulseService: ReaderPulseService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug')!;
    this.postService.getBySlug(slug).subscribe({
      next: (post) => {
        this.post = post;
        this.loading = false;
        this.selectedPulse = sessionStorage.getItem(this.pulseStorageKey(post.id)) ?? undefined;
        this.loadPulse();
        this.loadComments();
      },
      error: () => (this.loading = false)
    });
    this.sponsorshipService.active('POST_INLINE').subscribe({
      next: (ads) => (this.inlineAds = ads),
      error: () => (this.inlineAds = [])
    });
  }

  openAd(ad: Sponsorship, event: Event): void {
    event.preventDefault();
    this.sponsorshipService.recordClick(ad.id).subscribe();
    window.open(ad.targetUrl, '_blank', 'noopener');
  }

  loadComments(): void {
    if (!this.post) return;
    const postId = this.post.id;
    this.commentService.forPost(postId).subscribe((comments) => {
      this.comments = comments;
      if (this.post?.id === postId) {
        this.post.commentCount = comments.reduce((total, comment) => total + 1 + comment.replies.length, 0);
      }
    });
  }

  loadPulse(): void {
    if (!this.post) return;
    this.readerPulseService.get(this.post.id).subscribe((pulse) => (this.pulse = pulse));
  }

  votePulse(choice: string): void {
    if (!this.post || this.selectedPulse || this.pulseLoading) return;
    this.pulseLoading = true;
    this.readerPulseService.vote(this.post.id, choice).subscribe({
      next: (pulse) => {
        this.pulse = pulse;
        this.selectedPulse = choice;
        sessionStorage.setItem(this.pulseStorageKey(this.post!.id), choice);
        this.pulseLoading = false;
      },
      error: () => (this.pulseLoading = false)
    });
  }

  private pulseStorageKey(postId: number): string {
    return `wire-pulse-${postId}`;
  }

  submitComment(): void {
    if (!this.post || !this.newComment.trim()) return;
    this.posting = true;
    this.commentService.add(this.post.id, this.newComment.trim()).subscribe({
      next: () => {
        this.newComment = '';
        this.posting = false;
        this.loadComments();
      },
      error: () => (this.posting = false)
    });
  }

  startReply(comment: CommentItem): void {
    this.replyTo = comment;
    this.replyBody = '';
  }

  cancelReply(): void {
    this.replyTo = undefined;
  }

  submitReply(): void {
    if (!this.post || !this.replyTo || !this.replyBody.trim()) return;
    this.commentService.add(this.post.id, this.replyBody.trim(), this.replyTo.id).subscribe(() => {
      this.replyTo = undefined;
      this.replyBody = '';
      this.loadComments();
    });
  }

  deleteComment(comment: CommentItem): void {
    this.commentService.delete(comment.id).subscribe(() => this.loadComments());
  }

  reportComment(comment: CommentItem): void {
    this.commentService.flag(comment.id).subscribe();
  }

  canDelete(comment: CommentItem): boolean {
    const me = this.auth.currentUser();
    return !!me && (me.userId === comment.author.id || me.role === 'ADMIN');
  }
}
