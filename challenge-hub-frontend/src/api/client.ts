import axios, { AxiosError, AxiosRequestConfig } from 'axios';
import { useAuthStore } from '../store/authStore';
import type { ApiResponse } from '../types/api';
import type { AuthData } from '../types/auth';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1',
  withCredentials: true,
});

let isRefreshing = false;
let queue: Array<() => void> = [];

client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as AxiosRequestConfig & { _retry?: boolean };
    const status = error.response?.status;

    if (status === 401 && !original?._retry) {
      original._retry = true;

      if (!isRefreshing) {
        isRefreshing = true;
        try {
          const refreshResponse = await client.post<ApiResponse<AuthData>>('/auth/refresh');
          const authData = refreshResponse.data.data;
          if (authData) {
            useAuthStore.getState().setAuth({
              accessToken: authData.access_token,
              expiresIn: authData.expires_in,
              user: useAuthStore.getState().user ?? authData.user,
            });
          }
          queue.forEach((fn) => fn());
          queue = [];
        } catch (refreshError) {
          useAuthStore.getState().clearAuth();
          queue = [];
          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
        }
      }

      return new Promise((resolve, reject) => {
        queue.push(() => {
          client(original).then(resolve).catch(reject);
        });
      });
    }

    return Promise.reject(error);
  }
);

export default client;
