import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'wb-account',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './account.component.html',
  styleUrl: './account.component.scss'
})
export class AccountComponent implements OnInit {
  displayName = '';
  avatarUrl = '';
  bio = '';
  saving = false;
  message = '';
  error = '';

  constructor(public auth: AuthService) {}

  ngOnInit(): void {
    this.auth.me().subscribe({
      next: (user) => {
        this.displayName = user.displayName;
        this.avatarUrl = user.avatarUrl ?? '';
        this.bio = user.bio ?? '';
      },
      error: () => (this.error = 'Profile could not be loaded.')
    });
  }

  save(): void {
    if (!this.displayName.trim()) {
      this.error = 'Display name is required.';
      return;
    }
    this.saving = true;
    this.error = '';
    this.message = '';
    this.auth.updateProfile({
      displayName: this.displayName.trim(),
      avatarUrl: this.avatarUrl.trim() || undefined,
      bio: this.bio.trim() || undefined
    }).subscribe({
      next: () => {
        this.saving = false;
        this.message = 'Profile updated.';
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message ?? 'Profile could not be updated.';
      }
    });
  }
}
