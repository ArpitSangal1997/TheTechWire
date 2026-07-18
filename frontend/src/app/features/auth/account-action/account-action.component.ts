import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'wb-account-action',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './account-action.component.html',
  styleUrl: '../auth.component.scss'
})
export class AccountActionComponent implements OnInit {
  mode: 'forgot' | 'reset' | 'verify' = 'forgot';
  email = '';
  newPassword = '';
  token = '';
  loading = false;
  success = '';
  error = '';

  constructor(private route: ActivatedRoute, private auth: AuthService) {}

  ngOnInit(): void {
    const path = this.route.snapshot.routeConfig?.path;
    this.mode = path === 'reset-password' ? 'reset' : path === 'verify-email' ? 'verify' : 'forgot';
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (this.mode === 'verify') this.verify();
  }

  submit(): void {
    if (this.mode === 'forgot') this.requestReset();
    if (this.mode === 'reset') this.resetPassword();
  }

  private requestReset(): void {
    if (!this.email.trim()) return;
    this.run(this.auth.requestPasswordReset(this.email.trim()),
      'If that email is registered, a password-reset link has been sent.');
  }

  private resetPassword(): void {
    if (!this.token || this.newPassword.length < 8) {
      this.error = 'Use a valid reset link and a password of at least 8 characters.';
      return;
    }
    this.run(this.auth.confirmPasswordReset(this.token, this.newPassword),
      'Your password has been changed. You can sign in now.');
  }

  private verify(): void {
    if (!this.token) {
      this.error = 'This verification link is invalid.';
      return;
    }
    this.run(this.auth.verifyEmail(this.token), 'Your email is verified. You can sign in now.');
  }

  private run(request: import('rxjs').Observable<void>, message: string): void {
    this.loading = true;
    this.error = '';
    request.subscribe({
      next: () => { this.loading = false; this.success = message; },
      error: (err) => { this.loading = false; this.error = err?.error?.message ?? 'That request could not be completed.'; }
    });
  }
}
