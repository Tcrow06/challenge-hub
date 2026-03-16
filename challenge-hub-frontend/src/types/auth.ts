export type UserRole = 'USER' | 'CREATOR' | 'MODERATOR' | 'ADMIN';

export interface AuthUser {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  avatar_url: string | null;
  display_name: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthData {
  access_token: string;
  token_type: 'Bearer';
  expires_in: number;
  user: AuthUser;
}
