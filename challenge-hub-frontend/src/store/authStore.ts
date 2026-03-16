import { create } from 'zustand';
import type { AuthUser } from '../types/auth';

interface AuthState {
  accessToken: string | null;
  expiresAt: number | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  setAuth: (payload: { accessToken: string; expiresIn: number; user: AuthUser }) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  expiresAt: null,
  user: null,
  isAuthenticated: false,
  setAuth: ({ accessToken, expiresIn, user }) =>
    set({
      accessToken,
      expiresAt: Date.now() + expiresIn * 1000,
      user,
      isAuthenticated: true,
    }),
  clearAuth: () =>
    set({
      accessToken: null,
      expiresAt: null,
      user: null,
      isAuthenticated: false,
    }),
}));
