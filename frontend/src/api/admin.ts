import { apiClient } from './client';
import type {
  ApiUsageResponse,
  AuditLogResponse,
  EmployeeStatsResponse,
  ProviderConfigResponse,
  Role
} from '@/types/api';

export const adminApi = {
  createEmployee: (payload: { email: string; temporaryPassword: string; firstName: string; lastName: string; role: Role }) =>
    apiClient.post('/admin/employees', payload),

  setUserEnabled: (userId: number, enabled: boolean) =>
    apiClient.patch(`/admin/users/${userId}/enabled`, null, { params: { enabled } }),

  changeUserRole: (userId: number, role: Role) =>
    apiClient.patch(`/admin/users/${userId}/role`, null, { params: { role } }),

  blockInstrument: (instrumentId: number, reason: string) =>
    apiClient.post(`/admin/instruments/${instrumentId}/block`, null, { params: { reason } }),

  employeeStats: () => apiClient.get<EmployeeStatsResponse[]>('/admin/stats/employees').then((r) => r.data),

  auditLog: (page = 0, size = 50) =>
    apiClient.get<AuditLogResponse[]>('/admin/audit-log', { params: { page, size } }).then((r) => r.data),

  apiUsageToday: () => apiClient.get<ApiUsageResponse[]>('/admin/api-usage').then((r) => r.data),

  providerConfigs: () => apiClient.get<ProviderConfigResponse[]>('/admin/api-usage/providers').then((r) => r.data),

  updateProviderConfig: (provider: string, payload: { apiKey?: string; dailyLimit?: number; active?: boolean }) =>
    apiClient.patch(`/admin/api-usage/providers/${provider}`, payload)
};
