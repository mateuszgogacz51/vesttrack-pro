import { apiClient } from './client';
import type { AuthResponse } from '@/types/api';

export const authApi = {
  register: (payload: { email: string; password: string; firstName: string; lastName: string }) =>
    apiClient.post<AuthResponse>('/auth/register', payload).then((r) => r.data),

  login: (payload: { email: string; password: string }) =>
    apiClient.post<AuthResponse>('/auth/login', payload).then((r) => r.data),

  logout: (refreshToken: string) => apiClient.post('/auth/logout', { refreshToken }),

  forgotPassword: (email: string) => apiClient.post('/auth/forgot-password', { email }),

  resetPassword: (payload: { token: string; newPassword: string }) =>
    apiClient.post('/auth/reset-password', payload)
};
