import { AfterViewInit, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import Quill from 'quill';
import { PostService } from '../../core/services/post.service';
import { UploadService } from '../../core/services/upload.service';

@Component({
  selector: 'wb-post-editor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './post-editor.component.html',
  styleUrl: './post-editor.component.scss'
})
export class PostEditorComponent implements OnInit, AfterViewInit {
  @ViewChild('editor') editorElement?: ElementRef<HTMLDivElement>;

  postId?: number;
  mode: 'STORY' | 'LINK' = 'STORY';
  title = '';
  linkUrl = '';
  linkTitle = '';
  linkNote = '';
  excerpt = '';
  content = '';
  coverImageUrl = '';
  tagsInput = '';
  trailTitle = '';
  saving = false;
  uploading = false;
  loading = false;
  error = '';
  private quill?: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private postService: PostService,
    private uploadService: UploadService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.postId = Number(idParam);
      if (!Number.isSafeInteger(this.postId) || this.postId <= 0) {
        this.error = 'This edit link is invalid.';
        return;
      }
      this.loadPost(this.postId);
    }
  }

  ngAfterViewInit(): void {
    if (!this.editorElement) return;
    this.quill = new Quill(this.editorElement.nativeElement, {
      theme: 'snow',
      modules: {
        toolbar: [
          ['bold', 'italic', 'underline'],
          ['blockquote', 'code-block'],
          [{ header: [1, 2, 3, false] }],
          [{ list: 'ordered' }, { list: 'bullet' }],
          ['link', 'clean']
        ]
      },
      placeholder: 'Tell your story...'
    });
    this.quill.root.innerHTML = this.content;
    this.quill.on('text-change', () => {
      this.content = this.quill?.root.innerHTML ?? '';
    });
  }

  get wordCount(): number {
    const text = this.quill?.getText() ?? this.content.replace(/<[^>]*>/g, ' ');
    return text.trim().split(/\s+/).filter(Boolean).length;
  }

  private loadPost(id: number): void {
    this.loading = true;
    this.postService.getForEditing(id).subscribe({
      next: (post) => {
        if (post.sourceUrl) {
          this.mode = 'LINK';
          this.linkUrl = post.sourceUrl;
          this.linkTitle = post.title;
          this.linkNote = post.excerpt ?? post.content;
        }
        this.title = post.title;
        this.excerpt = post.excerpt ?? '';
        this.content = post.content;
        this.coverImageUrl = post.coverImageUrl ?? '';
        this.tagsInput = post.tags.join(', ');
        this.trailTitle = post.trail?.title ?? '';
        if (this.quill) this.quill.root.innerHTML = this.content;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message ?? 'This post could not be loaded for editing.';
      }
    });
  }

  save(publish: boolean): void {
    if (this.mode === 'LINK') {
      this.saveLink();
      return;
    }
    if (this.loading) return;
    this.content = this.quill?.root.innerHTML ?? this.content;
    if (!this.title.trim() || !this.content.trim()) {
      this.error = 'Give your post a title and some content before saving.';
      return;
    }
    this.saving = true;
    this.error = '';

    const tags = this.tagsInput.split(',').map((t) => t.trim()).filter(Boolean);
    const payload = {
      title: this.title.trim(),
      excerpt: this.excerpt.trim(),
      content: this.content,
      coverImageUrl: this.coverImageUrl.trim() || undefined,
      tags,
      trailTitle: this.trailTitle.trim() || undefined,
      publish
    };

    const request = this.postId
      ? this.postService.update(this.postId, payload)
      : this.postService.create(payload);

    request.subscribe({
      next: (post) => {
        this.saving = false;
        this.router.navigate(['/post', post.slug]);
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message ?? 'Something went wrong saving your post.';
      }
    });
  }

  saveLink(): void {
    const url = this.linkUrl.trim();
    if (!url || this.saving || this.loading) return;
    this.saving = true;
    this.error = '';
    const request = { url, title: this.linkTitle.trim(), note: this.linkNote.trim() };
    const operation = this.postId
      ? this.postService.updateSharedLink(this.postId, request)
      : this.postService.shareLink(request);
    operation.subscribe({
      next: post => {
        this.saving = false;
        this.router.navigate(['/post', post.slug]);
      },
      error: err => {
        this.saving = false;
        this.error = err?.error?.message ?? 'This URL could not be shared.';
      }
    });
  }

  uploadCoverImage(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploading = true;
    this.uploadService.upload(file).subscribe({
      next: (res) => {
        this.coverImageUrl = res.url;
        this.uploading = false;
      },
      error: () => {
        this.uploading = false;
        this.error = 'Image upload failed.';
      }
    });
  }
}
