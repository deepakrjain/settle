export interface User {
  id: string;
  email: string;
  displayName: string;
  createdAt: string;
}

export interface GroupMember {
  id: string;
  groupId: string;
  userId: string;
  joinedAt: string;
}

export interface Group {
  id: string;
  name: string;
  createdBy: string;
  createdAt: string;
  members: GroupMember[];
}

export interface UserBalance {
  userId: string;
  netBalance: number;
}
