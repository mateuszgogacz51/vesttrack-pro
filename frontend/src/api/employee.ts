import { apiClient } from './client';
import type { AnonymizedPortfolioView, AssetType, FinancialInstrument } from '@/types/api';

export const employeeApi = {
  searchInstruments: (query: string) =>
    apiClient.get<FinancialInstrument[]>('/employee/instruments/search', { params: { query } }).then((r) => r.data),

  createInstrument: (payload: {
    ticker: string;
    name: string;
    assetType: AssetType;
    isin?: string;
    exchange?: string;
    quoteCurrency: string;
    accumulating?: boolean | null;
  }) => apiClient.post<FinancialInstrument>('/employee/instruments', payload).then((r) => r.data),

  anonymizedView: (accountId: number) =>
    apiClient.get<AnonymizedPortfolioView>(`/employee/accounts/${accountId}/anonymized-view`).then((r) => r.data)
};
