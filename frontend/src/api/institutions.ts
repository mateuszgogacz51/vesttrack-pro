import { apiClient } from './client';
import type { BrokerageFirm } from '@/types/api';

export const institutionsApi = {
  search: (query: string) =>
    apiClient
      .get<BrokerageFirm[]>('/institutions/search', { params: query ? { query } : {} })
      .then((r) => r.data)
};
