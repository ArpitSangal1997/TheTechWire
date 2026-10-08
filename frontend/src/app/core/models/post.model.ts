import { AuthorRef } from './user.model';
import type { StoryTrailRef } from './story-trail.model';

export interface PostSummary {
  id: number;
  title: string;
  slug: string;
  excerpt?: string;
  coverImageUrl?: string;
  sourceUrl?: string;
  tags: string[];
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  author: AuthorRef;
  viewCount: number;
  shareCount: number;
  commentCount: number;
  trail?: StoryTrailRef;
  publishedAt?: string;
}

export interface PostDetail {
  id: number;
  title: string;
  slug: string;
  excerpt?: string;
  content: string;
  coverImageUrl?: string;
  sourceUrl?: string;
  tags: string[];
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  author: AuthorRef;
  viewCount: number;
  shareCount: number;
  commentCount: number;
  trail?: StoryTrailRef;
  publishedAt?: string;
  updatedAt?: string;
}

export interface PostRequest {
  title: string;
  excerpt?: string;
  content: string;
  coverImageUrl?: string;
  tags: string[];
  trailTitle?: string;
  publish: boolean;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  last: boolean;
}
