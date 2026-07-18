export interface NotificationItem {
  id: number;
  type: 'SHARE' | 'REPLY' | string;
  message: string;
  link?: string;
  read: boolean;
  createdAt: string;
}
