import type { PostSummary } from './post.model';

export interface StoryTrailRef {
  id: number;
  title: string;
  slug: string;
}

export interface StoryTrailSummary extends StoryTrailRef {
  description?: string;
  storyCount: number;
}

export interface GroupPostTrailItem {
  id: number;
  groupId: number;
  groupName: string;
  title: string;
  body: string;
  authorId: number;
  authorName: string;
  authorHandle: string;
  createdAt: string;
  commentCount: number;
  canOpenGroup: boolean;
}

export interface StoryTrailDetail extends StoryTrailSummary {
  updatedAt?: string;
  stories: PostSummary[];
  communityPosts: GroupPostTrailItem[];
}
