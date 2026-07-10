import { apiClient } from './client';
import type { FinancialInstrument } from '@/types/api';

export const instrumentsApi = {
  search: (query: string) =>
    apiClient.get<FinancialInstrument[]>('/instruments/search', { params: { query } }).then((r) => r.data)
};
