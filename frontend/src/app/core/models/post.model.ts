import { AuthorRef } from './user.model';

export interface PostSummary {
  id: number;
  title: string;
  slug: string;
  excerpt?: string;
  coverImageUrl?: string;
  tags: string[];
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  author: AuthorRef;
  viewCount: number;
  shareCount: number;
  commentCount: number;
  publishedAt?: string;
}

export interface PostDetail {
  id: number;
  title: string;
  slug: string;
  excerpt?: string;
  content: string;
  coverImageUrl?: string;
  tags: string[];
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  author: AuthorRef;
  viewCount: number;
  shareCount: number;
  commentCount: number;
  publishedAt?: string;
  updatedAt?: string;
}

export interface PostRequest {
  title: string;
  excerpt?: string;
  content: string;
  coverImageUrl?: string;
  tags: string[];
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
