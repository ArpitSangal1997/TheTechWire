import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { StoryTrailDetail } from '../../core/models/story-trail.model';
import { StoryTrailService } from '../../core/services/story-trail.service';

@Component({
  selector: 'wb-story-trail-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './story-trail-detail.component.html',
  styleUrl: './story-trail-detail.component.scss'
})
export class StoryTrailDetailComponent implements OnInit {
  trail?: StoryTrailDetail;
  loading = true;
  error = '';

  constructor(private route: ActivatedRoute, private storyTrailService: StoryTrailService) {}

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug')!;
    this.storyTrailService.get(slug).subscribe({
      next: (trail) => {
        this.trail = trail;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'This trail is unavailable or private to its community.';
      }
    });
  }
}
