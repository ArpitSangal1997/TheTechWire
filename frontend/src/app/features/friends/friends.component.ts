import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FriendEntry, FriendUser } from '../../core/models/friend.model';
import { FriendService } from '../../core/services/friend.service';

@Component({
  selector: 'wb-friends',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './friends.component.html',
  styleUrl: './friends.component.scss'
})
export class FriendsComponent implements OnInit {
  entries: FriendEntry[] = [];
  results: FriendUser[] = [];
  query = '';
  error = '';

  constructor(private friends: FriendService) {}

  ngOnInit(): void { this.load(); }

  get incoming(): FriendEntry[] { return this.entries.filter(entry => entry.relationship === 'INCOMING_REQUEST'); }
  get outgoing(): FriendEntry[] { return this.entries.filter(entry => entry.relationship === 'OUTGOING_REQUEST'); }
  get accepted(): FriendEntry[] { return this.entries.filter(entry => entry.relationship === 'FRIEND'); }

  search(): void {
    const value = this.query.trim();
    if (value.length < 2) {
      this.results = [];
      return;
    }
    this.friends.searchUsers(value).subscribe({
      next: users => this.results = users,
      error: err => this.error = err?.error?.message ?? 'People could not be searched.'
    });
  }

  add(user: FriendUser): void {
    this.friends.request(user.id).subscribe({
      next: () => this.refresh(),
      error: err => this.error = err?.error?.message ?? 'Friend request could not be sent.'
    });
  }

  accept(user: FriendEntry): void {
    this.friends.accept(user.id).subscribe({
      next: () => this.refresh(),
      error: err => this.error = err?.error?.message ?? 'Friend request could not be accepted.'
    });
  }

  remove(user: FriendEntry): void {
    const action = user.relationship === 'FRIEND' ? 'Remove' : 'Decline or cancel';
    if (!confirm(`${action} @${user.handle}?`)) return;
    this.friends.remove(user.id).subscribe({
      next: () => this.refresh(),
      error: err => this.error = err?.error?.message ?? 'Friend connection could not be removed.'
    });
  }

  private load(): void {
    this.friends.list().subscribe({
      next: entries => this.entries = entries,
      error: err => this.error = err?.error?.message ?? 'Your friends could not be loaded.'
    });
  }

  private refresh(): void {
    this.error = '';
    this.load();
    if (this.query.trim().length >= 2) this.search();
  }
}
