import { AuthorRef } from './user.model';

export interface CommentItem {
  id: number;
  body: string;
  author: AuthorRef;
  parentId?: number;
  createdAt: string;
  replies: CommentItem[];
}
