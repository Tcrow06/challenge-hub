import client from '../../../api/client';
import type { ApiResponse } from '../../../types/api';
import type { AuthData, LoginRequest, RegisterRequest } from '../../../types/auth';

export const login = async (payload: LoginRequest): Promise<AuthData> => {
  const response = await client.post<ApiResponse<AuthData>>('/auth/login', payload);
  if (!response.data.data) {
    throw new Error('Missing login payload');
  }
  return response.data.data;
};

export const register = async (payload: RegisterRequest): Promise<AuthData> => {
  const response = await client.post<ApiResponse<AuthData>>('/auth/register', payload);
  if (!response.data.data) {
    throw new Error('Missing register payload');
  }
  return response.data.data;
};

export const logout = async (): Promise<void> => {
  await client.post('/auth/logout');
};
