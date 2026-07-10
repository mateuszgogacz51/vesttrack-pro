import { apiClient } from './client';
import type { TransactionResponse, TransactionType } from '@/types/api';

export const transactionsApi = {
  listByAccount: (accountId: number) =>
    apiClient.get<TransactionResponse[]>(`/transactions/account/${accountId}`).then((r) => r.data),

  create: (payload: {
    accountId: number;
    instrumentId: number;
    transactionType: TransactionType;
    quantity: number;
    price: number;
    fee?: number;
    instrumentCurrency: string;
    exchangeRate: number;
    transactionDate: string;
  }) => apiClient.post<TransactionResponse>('/transactions', payload).then((r) => r.data)
};
