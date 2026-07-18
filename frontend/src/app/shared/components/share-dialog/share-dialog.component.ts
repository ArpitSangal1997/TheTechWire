import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ShareService, ShareChannel } from '../../../core/services/share.service';

@Component({
  selector: 'wb-share-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './share-dialog.component.html',
  styleUrl: './share-dialog.component.scss'
})
export class ShareDialogComponent {
  @Input({ required: true }) postId!: number;
  @Input({ required: true }) slug!: string;
  @Input({ required: true }) title!: string;
  @Output() closed = new EventEmitter<void>();

  handleInput = '';
  copied = false;
  sendingTo = false;

  constructor(private shareService: ShareService) {}

  get publicUrl(): string {
    return this.shareService.publicUrl(this.slug);
  }

  shareExternal(channel: Exclude<ShareChannel, 'IN_APP'>): void {
    const url = encodeURIComponent(this.publicUrl);
    const text = encodeURIComponent(this.title);
    const targets: Record<string, string> = {
      WHATSAPP: `https://wa.me/?text=${text}%20${url}`,
      TWITTER: `https://twitter.com/intent/tweet?text=${text}&url=${url}`,
      FACEBOOK: `https://www.facebook.com/sharer/sharer.php?u=${url}`,
      LINKEDIN: `https://www.linkedin.com/sharing/share-offsite/?url=${url}`,
      EMAIL: `mailto:?subject=${text}&body=${url}`
    };
    window.open(targets[channel], '_blank', 'noopener');
    this.shareService.share(this.postId, channel).subscribe();
  }

  copyLink(): void {
    navigator.clipboard.writeText(this.publicUrl).then(() => {
      this.copied = true;
      this.shareService.share(this.postId, 'COPY_LINK').subscribe();
      setTimeout(() => (this.copied = false), 2000);
    });
  }

  sendInApp(): void {
    if (!this.handleInput.trim()) return;
    this.sendingTo = true;
    this.shareService.share(this.postId, 'IN_APP', this.handleInput.replace('@', '')).subscribe({
      next: () => {
        this.sendingTo = false;
        this.handleInput = '';
        this.close();
      },
      error: () => (this.sendingTo = false)
    });
  }

  close(): void {
    this.closed.emit();
  }
}
