import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PostService } from '../../core/services/post.service';
import { PostSummary } from '../../core/models/post.model';

@Component({
  selector: 'wb-my-stories',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-stories.component.html',
  styleUrl: './my-stories.component.scss'
})
export class MyStoriesComponent implements OnInit {
  posts: PostSummary[] = [];
  page = 0;
  hasMore = false;
  loading = true;
  error = '';
  workingId?: number;

  constructor(private postService: PostService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.postService.mine(this.page).subscribe({
      next: (res) => {
        this.posts = this.page ? [...this.posts, ...res.content] : res.content;
        this.hasMore = !res.last;
        this.loading = false;
      },
      error: () => {
        this.error = 'Your stories could not be loaded.';
        this.loading = false;
      }
    });
  }

  loadMore(): void {
    this.page++;
    this.load();
  }

  publish(post: PostSummary): void {
    this.run(post, () => this.postService.publish(post.id));
  }

  archive(post: PostSummary): void {
    this.run(post, () => this.postService.archive(post.id));
  }

  delete(post: PostSummary): void {
    if (!confirm(`Delete "${post.title}"?`)) return;
    this.workingId = post.id;
    this.postService.delete(post.id).subscribe({
      next: () => {
        this.posts = this.posts.filter((item) => item.id !== post.id);
        this.workingId = undefined;
      },
      error: () => {
        this.error = 'That story could not be deleted.';
        this.workingId = undefined;
      }
    });
  }

  private run(post: PostSummary, action: () => ReturnType<PostService['publish']>): void {
    this.workingId = post.id;
    action().subscribe({
      next: (updated) => {
        this.posts = this.posts.map((item) => (item.id === updated.id ? updated : item));
        this.workingId = undefined;
      },
      error: () => {
        this.error = 'That story could not be updated.';
        this.workingId = undefined;
      }
    });
  }
}
