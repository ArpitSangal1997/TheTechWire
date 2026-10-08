export type FriendshipState = 'FRIEND' | 'INCOMING_REQUEST' | 'OUTGOING_REQUEST' | 'NONE';

export interface FriendUser {
  id: number;
  displayName: string;
  handle: string;
  avatarUrl?: string;
  relationship: FriendshipState;
}

export interface FriendEntry extends FriendUser {
  createdAt: string;
}
