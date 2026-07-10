import { apiClient } from './client';
import type { TicketResponse, TicketStatus } from '@/types/api';

export const ticketsApi = {
  myTickets: () => apiClient.get<TicketResponse[]>('/tickets/my').then((r) => r.data),

  create: (payload: { subject: string; description: string }) =>
    apiClient.post<TicketResponse>('/tickets', payload).then((r) => r.data),

  addNote: (ticketId: number, payload: { note: string; internal: boolean }) =>
    apiClient.post(`/tickets/${ticketId}/notes`, payload),

  // Panel pracownika
  allTickets: (status?: TicketStatus) =>
    apiClient.get<TicketResponse[]>('/employee/tickets', { params: status ? { status } : undefined }).then((r) => r.data),

  assignToMe: (ticketId: number) =>
    apiClient.post<TicketResponse>(`/employee/tickets/${ticketId}/assign-to-me`).then((r) => r.data),

  updateStatus: (ticketId: number, status: TicketStatus) =>
    apiClient.patch<TicketResponse>(`/employee/tickets/${ticketId}/status`, { status }).then((r) => r.data)
};
