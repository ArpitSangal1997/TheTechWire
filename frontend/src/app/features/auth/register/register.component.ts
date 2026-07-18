import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UploadService } from '../../../core/services/upload.service';

@Component({
  selector: 'wb-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrl: '../auth.component.scss'
})
export class RegisterComponent {
  displayName = '';
  handle = '';
  email = '';
  password = '';
  avatarUrl = '';
  loading = false;
  uploadingAvatar = false;
  error = '';

  constructor(private auth: AuthService, private router: Router, private uploadService: UploadService) {}

  submit(): void {
    this.loading = true;
    this.error = '';
    this.auth.register({
      displayName: this.displayName,
      handle: this.handle.toLowerCase().replace(/[^a-z0-9_]/g, ''),
      email: this.email,
      password: this.password,
      avatarUrl: this.avatarUrl
    }).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message ?? 'Could not create your account.';
      }
    });
  }

  uploadAvatar(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploadingAvatar = true;
    this.uploadService.upload(file).subscribe({
      next: (res) => {
        this.avatarUrl = res.url;
        this.uploadingAvatar = false;
      },
      error: () => {
        this.uploadingAvatar = false;
        this.error = 'Avatar upload failed.';
      }
    });
  }
}
