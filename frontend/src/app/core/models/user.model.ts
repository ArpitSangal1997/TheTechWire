export interface AuthorRef {
  id: number;
  displayName: string;
  handle: string;
  avatarUrl?: string;
}

export interface AuthUser {
  token?: string;
  userId: number;
  displayName: string;
  handle: string;
  avatarUrl?: string;
  bio?: string;
  role: 'READER' | 'AUTHOR' | 'ADMIN';
  emailVerified: boolean;
}
