import { apiClient } from './client';
import type { PortfolioAllocationResponse, PerformanceResponse } from '@/types/api';

export const portfolioApi = {
  allocation: (accountId: number) =>
    apiClient.get<PortfolioAllocationResponse>(`/portfolio/account/${accountId}/allocation`).then((r) => r.data),

  performance: (accountId: number) =>
    apiClient.get<PerformanceResponse>(`/portfolio/account/${accountId}/performance`).then((r) => r.data)
};
