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
  userDisplayName?: string;
  userEmail?: string;
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

export type SplitType = 'EQUAL' | 'PERCENTAGE' | 'EXACT' | 'SHARES' | 'ITEMIZED';

export interface ExpenseSplitResponse {
  id: string;
  userId: string;
  shareAmount: number;
}

export interface ExpenseResponse {
  id: string;
  groupId: string;
  paidByUserId: string;
  amount: number;
  currency: string;
  description: string;
  category: string;
  splitType: SplitType;
  splits: ExpenseSplitResponse[];
  createdAt: string;
}

export interface ItemizedItem {
  itemName: string;
  amount: number;
  participantUserIds: string[];
}

export interface CreateExpensePayload {
  paidByUserId: string;
  amount?: number;
  currency?: string;
  description: string;
  category?: string;
  splitType: SplitType;
  participantUserIds?: string[];
  percentages?: Record<string, number>;
  exactAmounts?: Record<string, number>;
  shares?: Record<string, number>;
  items?: ItemizedItem[];
  taxAndTip?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export interface SettlementTransaction {
  fromUserId: string;
  toUserId: string;
  amount: number;
}

export interface SettlementPlanResponse {
  greedyPlan: SettlementTransaction[];
  greedyTransactionCount: number;
  optimalPlan: SettlementTransaction[] | null;
  optimalTransactionCount: number | null;
  optimalCalculated: boolean;
}

export interface RecordSettlementPayload {
  fromUserId: string;
  toUserId: string;
  amount: number;
  idempotencyKey: string;
}

export interface SettlementResponse {
  id: string;
  groupId: string;
  fromUserId: string;
  toUserId: string;
  amount: number;
  idempotencyKey: string;
  status: string;
  createdAt: string;
}
