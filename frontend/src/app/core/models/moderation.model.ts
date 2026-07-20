export interface ModerationComment {
  id: number; postId: number; postTitle: string; body: string;
  author: { id: number; displayName: string; handle: string; avatarUrl?: string };
  flagged: boolean; createdAt: string;
}

export interface AdminUser {
  id: number; displayName: string; handle: string; email: string;
  role: string; enabled: boolean; createdAt: string;
}
