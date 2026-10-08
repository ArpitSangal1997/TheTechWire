import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { FriendService } from '../../core/services/friend.service';
import { FriendUser } from '../../core/models/friend.model';
import { GroupDetail, GroupJoinRequest, GroupJoinPolicy, GroupPost, GroupPostComment, GroupService, GroupVisibility } from '../../core/services/group.service';

@Component({
  selector: 'wb-group-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './group-detail.component.html',
  styleUrl: './group-detail.component.scss'
})
export class GroupDetailComponent implements OnInit {
  group?: GroupDetail;
  groupId = 0;
  loading = true;
  error = '';
  title = '';
  body = '';
  trailTitle = '';
  commentDrafts: Record<number, string> = {};
  replyDrafts: Record<number, string> = {};
  replyingTo?: number;
  memberQuery = '';
  searchResults: FriendUser[] = [];
  working = false;
  editingSettings = false;
  settingsVisibility: GroupVisibility = 'PUBLIC';
  settingsJoinPolicy: GroupJoinPolicy = 'OPEN';
  settingsDescription = '';
  settingsTopics = '';
  settingsRules = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private groups: GroupService,
    private friends: FriendService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(id) || id <= 0) {
      this.error = 'This group link is invalid.';
      this.loading = false;
      return;
    }
    this.groupId = id;
    this.load();
  }

  get addableUsers(): FriendUser[] {
    return this.searchResults.filter(user => !this.group?.members.some(member => member.id === user.id));
  }

  get canDeleteGroup(): boolean {
    const me = this.auth.currentUser();
    return !!this.group && (!!me && (me.userId === this.group.creatorId || me.role === 'ADMIN'));
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.groups.detail(this.groupId).subscribe({
      next: group => {
        this.group = group;
        this.settingsVisibility = group.visibility;
        this.settingsJoinPolicy = group.joinPolicy;
        this.settingsDescription = group.description ?? '';
        this.settingsTopics = group.topics.join(', ');
        this.settingsRules = group.rules ?? '';
        this.loading = false;
      },
      error: err => {
        this.error = err?.error?.message ?? 'This group could not be loaded.';
        this.loading = false;
      }
    });
  }

  visibilityChanged(): void {
    this.settingsJoinPolicy = this.settingsVisibility === 'PUBLIC' ? 'OPEN' : 'INVITE_ONLY';
  }

  follow(): void {
    this.groups.follow(this.groupId).subscribe({
      next: group => this.group = group,
      error: err => this.showError(err, 'Could not follow this group.')
    });
  }

  requestToJoin(): void {
    this.groups.requestToJoin(this.groupId).subscribe({
      next: group => this.group = group,
      error: err => this.showError(err, 'Could not request access to this group.')
    });
  }

  decideJoinRequest(request: GroupJoinRequest, status: 'APPROVED' | 'DECLINED'): void {
    this.groups.decideJoinRequest(this.groupId, request.id, status).subscribe({
      next: group => this.group = group,
      error: err => this.showError(err, 'Could not update the join request.')
    });
  }

  updateSettings(): void {
    this.groups.update(this.groupId, {
      visibility: this.settingsVisibility,
      joinPolicy: this.settingsVisibility === 'PUBLIC' ? 'OPEN' : this.settingsJoinPolicy,
      description: this.settingsDescription.trim(),
      topics: this.settingsTopics.split(',').map(topic => topic.trim()).filter(Boolean),
      rules: this.settingsRules.trim()
    }).subscribe({
      next: group => {
        this.group = group;
        this.editingSettings = false;
      },
      error: err => this.showError(err, 'Could not update group settings.')
    });
  }

  searchMembers(): void {
    const query = this.memberQuery.trim();
    if (!this.group?.canManage || query.length < 2) {
      this.searchResults = [];
      return;
    }
    this.friends.searchUsers(query).subscribe({
      next: users => this.searchResults = users,
      error: () => this.searchResults = []
    });
  }

  leave(): void {
    const userId = this.auth.currentUser()?.userId;
    if (!userId || !confirm('Leave this group?')) return;
    this.groups.removeMember(this.groupId, userId).subscribe({
      next: () => this.router.navigate(['/groups']),
      error: err => this.showError(err, 'Could not leave this group.')
    });
  }

  addMember(user: FriendUser): void {
    this.groups.addMember(this.groupId, user.id).subscribe({
      next: () => {
        this.memberQuery = '';
        this.searchResults = [];
        this.load();
      },
      error: err => this.showError(err, 'Could not add this person.')
    });
  }

  removeMember(member: { id: number; handle: string }): void {
    if (!confirm(`Remove @${member.handle} from this group?`)) return;
    this.groups.removeMember(this.groupId, member.id).subscribe({ next: () => this.load(), error: err => this.showError(err, 'Could not remove this member.') });
  }

  createPost(): void {
    if (!this.title.trim() || !this.body.trim() || this.working) return;
    this.working = true;
    this.groups.createPost(this.groupId, this.title.trim(), this.body.trim(), this.trailTitle.trim()).subscribe({
      next: () => {
        this.title = '';
        this.body = '';
        this.trailTitle = '';
        this.working = false;
        this.load();
      },
      error: err => {
        this.working = false;
        this.showError(err, 'Could not publish your group post.');
      }
    });
  }

  addComment(post: GroupPost, parentId?: number): void {
    const body = (parentId ? this.replyDrafts[parentId] : this.commentDrafts[post.id])?.trim();
    if (!body) return;
    this.groups.addComment(this.groupId, post.id, body, parentId).subscribe({
      next: () => {
        if (parentId) this.replyDrafts[parentId] = '';
        else this.commentDrafts[post.id] = '';
        this.replyingTo = undefined;
        this.load();
      },
      error: err => this.showError(err, 'Could not add your comment.')
    });
  }

  deleteComment(post: GroupPost, comment: GroupPostComment): void {
    if (!confirm('Delete this comment and its replies?')) return;
    this.groups.deleteComment(this.groupId, post.id, comment.id).subscribe({
      next: () => this.load(),
      error: err => this.showError(err, 'Could not delete this comment.')
    });
  }

  deletePost(post: { id: number; title: string }): void {
    if (!confirm(`Delete “${post.title}” from this group?`)) return;
    this.groups.deletePost(this.groupId, post.id).subscribe({ next: () => this.load(), error: err => this.showError(err, 'Could not delete this group post.') });
  }

  deleteGroup(): void {
    if (!this.group || !this.canDeleteGroup || !confirm(`Delete “${this.group.name}” and its group posts?`)) return;
    this.groups.delete(this.groupId).subscribe({
      next: () => this.router.navigate(['/groups']),
      error: err => this.showError(err, 'Could not delete this group.')
    });
  }

  private showError(error: any, fallback: string): void {
    this.error = error?.error?.message ?? fallback;
  }
}
