import { useMutation } from '@tanstack/react-query';
import { login, logout, register } from '../api/authApi';
import { useAuthStore } from '../../../store/authStore';

export const useLoginMutation = () => {
  return useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      useAuthStore.getState().setAuth({
        accessToken: data.access_token,
        expiresIn: data.expires_in,
        user: data.user,
      });
    },
  });
};

export const useRegisterMutation = () => {
  return useMutation({
    mutationFn: register,
    onSuccess: (data) => {
      useAuthStore.getState().setAuth({
        accessToken: data.access_token,
        expiresIn: data.expires_in,
        user: data.user,
      });
    },
  });
};

export const useLogoutMutation = () => {
  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      useAuthStore.getState().clearAuth();
    },
  });
};
