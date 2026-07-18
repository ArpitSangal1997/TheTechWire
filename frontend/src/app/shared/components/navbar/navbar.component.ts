import { Component, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { NotificationItem } from '../../../core/models/notification.model';

@Component({
  selector: 'wb-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
  menuOpen = false;
  notificationsOpen = false;
  notifications: NotificationItem[] = [];

  constructor(public auth: AuthService, private notificationService: NotificationService) {
    effect(() => {
      if (this.auth.currentUser()) this.loadNotifications();
      else this.notifications = [];
    });
  }

  get unreadCount(): number {
    return this.notifications.filter((item) => !item.read).length;
  }

  toggleNotifications(): void {
    this.notificationsOpen = !this.notificationsOpen;
    if (this.notificationsOpen && this.unreadCount) {
      this.notificationService.markAllRead().subscribe(() => {
        this.notifications = this.notifications.map((item) => ({ ...item, read: true }));
      });
    }
  }

  private loadNotifications(): void {
    this.notificationService.list().subscribe({
      next: (items) => (this.notifications = items.slice(0, 20)),
      error: () => (this.notifications = [])
    });
  }

  logout(): void {
    this.auth.logout();
    window.location.href = '/';
  }
}
