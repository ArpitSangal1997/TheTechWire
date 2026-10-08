import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { StoryTrailSummary } from '../../core/models/story-trail.model';
import { StoryTrailService } from '../../core/services/story-trail.service';

@Component({
  selector: 'wb-story-trails',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './story-trails.component.html',
  styleUrl: './story-trails.component.scss'
})
export class StoryTrailsComponent implements OnInit {
  trails: StoryTrailSummary[] = [];
  loading = true;

  constructor(private storyTrailService: StoryTrailService) {}

  ngOnInit(): void {
    this.storyTrailService.list(0, 24).subscribe({
      next: (res) => {
        this.trails = res.content;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }
}
