import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Select } from '@/components/ui/Select';
import { Spinner } from '@/components/ui/Spinner';
import { EmptyState } from '@/components/ui/EmptyState';
import { ticketsApi } from '@/api/tickets';
import { formatDateTime } from '@/utils/format';
import type { TicketResponse, TicketStatus } from '@/types/api';

const statusTone: Record<TicketStatus, 'brand' | 'clay' | 'success' | 'neutral'> = {
  OPEN: 'brand',
  IN_PROGRESS: 'clay',
  RESOLVED: 'success',
  CLOSED: 'neutral'
};

export function EmployeeTicketsPage() {
  const [tickets, setTickets] = useState<TicketResponse[] | null>(null);
  const [filter, setFilter] = useState<TicketStatus | 'ALL'>('ALL');

  async function reload() {
    setTickets(await ticketsApi.allTickets(filter === 'ALL' ? undefined : filter));
  }

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  async function assignToMe(id: number) {
    await ticketsApi.assignToMe(id);
    reload();
  }

  async function updateStatus(id: number, status: TicketStatus) {
    await ticketsApi.updateStatus(id, status);
    reload();
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-display text-3xl text-ink">Kolejka zgłoszeń</h1>
          <p className="text-slate mt-1">Zgłoszenia od użytkowników wymagające obsługi</p>
        </div>
        <Select value={filter} onChange={(e) => setFilter(e.target.value as TicketStatus | 'ALL')} className="w-48">
          <option value="ALL">Wszystkie statusy</option>
          <option value="OPEN">Otwarte</option>
          <option value="IN_PROGRESS">W toku</option>
          <option value="RESOLVED">Rozwiązane</option>
          <option value="CLOSED">Zamknięte</option>
        </Select>
      </div>

      {tickets === null && (
        <div className="flex justify-center py-20">
          <Spinner size={28} />
        </div>
      )}

      {tickets !== null && tickets.length === 0 && (
        <Card>
          <EmptyState title="Brak zgłoszeń" description="Aktualnie nie ma zgłoszeń pasujących do wybranego filtra." />
        </Card>
      )}

      <div className="flex flex-col gap-3">
        {tickets?.map((ticket) => (
          <Card key={ticket.id}>
            <div className="flex items-start justify-between gap-4 mb-3">
              <div className="min-w-0">
                <div className="flex items-center gap-2 mb-1.5">
                  <Badge tone={statusTone[ticket.status]}>{ticket.status}</Badge>
                  <span className="text-xs text-slate">{formatDateTime(ticket.createdAt)}</span>
                </div>
                <h3 className="font-display text-base text-ink mb-1">{ticket.subject}</h3>
                <p className="text-sm text-slate">{ticket.description}</p>
                {ticket.assignedEmployeeEmail && (
                  <p className="text-xs text-slate-light mt-2">Przypisano: {ticket.assignedEmployeeEmail}</p>
                )}
              </div>
            </div>
            <div className="flex gap-2 flex-wrap">
              {!ticket.assignedEmployeeEmail && (
                <Button size="sm" variant="secondary" onClick={() => assignToMe(ticket.id)}>
                  Przypisz do mnie
                </Button>
              )}
              {ticket.status !== 'IN_PROGRESS' && (
                <Button size="sm" variant="ghost" onClick={() => updateStatus(ticket.id, 'IN_PROGRESS')}>
                  Oznacz: W toku
                </Button>
              )}
              {ticket.status !== 'RESOLVED' && (
                <Button size="sm" variant="ghost" onClick={() => updateStatus(ticket.id, 'RESOLVED')}>
                  Oznacz: Rozwiązane
                </Button>
              )}
              {ticket.status !== 'CLOSED' && (
                <Button size="sm" variant="ghost" onClick={() => updateStatus(ticket.id, 'CLOSED')}>
                  Zamknij
                </Button>
              )}
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
