import { apiClient } from './client';
import type { AccountResponse, AccountType } from '@/types/api';

export const accountsApi = {
  list: () => apiClient.get<AccountResponse[]>('/accounts').then((r) => r.data),

  create: (payload: {
    name?: string | null;
    brokerageFirmId?: number | null;
    accountType: AccountType;
    currency: string;
    annualContributionLimit?: number | null;
  }) => apiClient.post<AccountResponse>('/accounts', payload).then((r) => r.data)
};
