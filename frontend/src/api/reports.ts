import { apiClient } from './client';

export const reportsApi = {
  downloadCsv: async (accountId: number, year?: number) => {
    const response = await apiClient.get(`/reports/account/${accountId}/transactions.csv`, {
      params: year ? { year } : undefined,
      responseType: 'blob'
    });
    triggerDownload(response.data, `transakcje_konto_${accountId}${year ? `_${year}` : ''}.csv`);
  },

  downloadPdf: async (accountId: number, year?: number) => {
    const response = await apiClient.get(`/reports/account/${accountId}/annual-summary.pdf`, {
      params: year ? { year } : undefined,
      responseType: 'blob'
    });
    triggerDownload(response.data, `roczne_zestawienie_konto_${accountId}${year ? `_${year}` : ''}.pdf`);
  }
};

function triggerDownload(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
