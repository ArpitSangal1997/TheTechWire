export interface ModerationComment {
  id: number; postId: number; postTitle: string; body: string;
  author: { id: number; displayName: string; handle: string; avatarUrl?: string };
  flagged: boolean; createdAt: string;
}

export interface AdminUser {
  id: number; displayName: string; handle: string; email: string;
  role: string; enabled: boolean; createdAt: string;
}

export interface AdminPost {
  id: number;
  title: string;
  slug: string;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  author: { id: number; displayName: string; handle: string; avatarUrl?: string };
  trail?: { id: number; title: string; slug: string };
  viewCount: number;
  shareCount: number;
  commentCount: number;
  createdAt: string;
  publishedAt?: string;
  updatedAt?: string;
}
