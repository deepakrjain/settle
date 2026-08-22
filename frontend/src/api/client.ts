/**
 * SECURITY NOTE ON TOKEN STORAGE (XSS TRADEOFF):
 * Storing JWT tokens in `localStorage` or `sessionStorage` exposes them to XSS (Cross-Site Scripting) attacks.
 * If an attacker successfully injects a script into the DOM, they can execute `localStorage.getItem('token')`
 * and instantly steal the user's authentication token.
 * 
 * Storing the token in-memory (within React closure state) prevents scripts from inspecting storage keys.
 * The security tradeoff is that reloading the page clears the token state (in a full production system,
 * this is solved by pairing in-memory access tokens with HttpOnly, SameSite=Strict refresh cookies).
 */

import { CreateExpensePayload, ExpenseResponse, Group, PageResponse, User, UserBalance } from './types';

let inMemoryToken: string | null = null;

export const setAuthToken = (token: string | null) => {
  inMemoryToken = token;
};

export const getAuthToken = () => inMemoryToken;

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {})
  };

  if (inMemoryToken) {
    headers['Authorization'] = `Bearer ${inMemoryToken}`;
  }

  const response = await fetch(endpoint, {
    ...options,
    headers
  });

  if (!response.ok) {
    const errorText = await response.text();
    let errorMessage = `HTTP Error ${response.status}`;
    try {
      const errorJson = JSON.parse(errorText);
      errorMessage = errorJson.message || errorMessage;
    } catch {
      errorMessage = errorText || errorMessage;
    }
    throw new Error(errorMessage);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

export const api = {
  register: (email: string, password: string, displayName: string) =>
    request<User>('/api/users/register', {
      method: 'POST',
      body: JSON.stringify({ email, password, displayName })
    }),

  login: (email: string, password: string) =>
    request<{ token: string }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    }),

  getMe: () => request<User>('/api/users/me'),

  getGroups: () => request<Group[]>('/api/groups'),

  createGroup: (name: string) =>
    request<Group>('/api/groups', {
      method: 'POST',
      body: JSON.stringify({ name })
    }),

  getGroupDetails: (id: string) => request<Group>(`/api/groups/${id}`),

  addMember: (groupId: string, userId: string) =>
    request<Group>(`/api/groups/${groupId}/members`, {
      method: 'POST',
      body: JSON.stringify({ userId })
    }),

  getBalances: (groupId: string) => request<UserBalance[]>(`/api/groups/${groupId}/balances`),

  createExpense: (groupId: string, payload: CreateExpensePayload) =>
    request<ExpenseResponse>(`/api/groups/${groupId}/expenses`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),

  getGroupExpenses: (groupId: string, page = 0, size = 10) =>
    request<PageResponse<ExpenseResponse>>(`/api/groups/${groupId}/expenses?page=${page}&size=${size}`)
};
