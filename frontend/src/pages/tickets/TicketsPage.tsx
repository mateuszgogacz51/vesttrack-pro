import { useEffect, useState, FormEvent } from 'react';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Alert } from '@/components/ui/Alert';
import { EmptyState } from '@/components/ui/EmptyState';
import { Spinner } from '@/components/ui/Spinner';
import { ticketsApi } from '@/api/tickets';
import { extractErrorMessage } from '@/api/client';
import { formatDateTime } from '@/utils/format';
import type { TicketResponse, TicketStatus } from '@/types/api';

const statusTone: Record<TicketStatus, 'brand' | 'clay' | 'success' | 'neutral'> = {
  OPEN: 'brand',
  IN_PROGRESS: 'clay',
  RESOLVED: 'success',
  CLOSED: 'neutral'
};

const statusLabel: Record<TicketStatus, string> = {
  OPEN: 'Otwarte',
  IN_PROGRESS: 'W toku',
  RESOLVED: 'Rozwiązane',
  CLOSED: 'Zamknięte'
};

export function TicketsPage() {
  const [tickets, setTickets] = useState<TicketResponse[] | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  async function reload() {
    setTickets(await ticketsApi.myTickets());
  }

  useEffect(() => {
    reload();
  }, []);

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-display text-3xl text-ink">Zgłoszenia wsparcia</h1>
          <p className="text-slate mt-1">Brakujący instrument, błąd w rozliczeniu — zgłoś się do zespołu</p>
        </div>
        <Button onClick={() => setModalOpen(true)}>+ Nowe zgłoszenie</Button>
      </div>

      {tickets === null && (
        <div className="flex justify-center py-20">
          <Spinner size={28} />
        </div>
      )}

      {tickets !== null && tickets.length === 0 && (
        <Card>
          <EmptyState title="Brak zgłoszeń" description="Nie masz jeszcze żadnych zgłoszeń do zespołu wsparcia." />
        </Card>
      )}

      <div className="flex flex-col gap-3">
        {tickets?.map((ticket) => (
          <Card key={ticket.id}>
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <div className="flex items-center gap-2 mb-1.5">
                  <Badge tone={statusTone[ticket.status]}>{statusLabel[ticket.status]}</Badge>
                  <span className="text-xs text-slate">{formatDateTime(ticket.createdAt)}</span>
                </div>
                <h3 className="font-display text-base text-ink mb-1">{ticket.subject}</h3>
                <p className="text-sm text-slate">{ticket.description}</p>
                {ticket.assignedEmployeeEmail && (
                  <p className="text-xs text-slate-light mt-2">Obsługuje: {ticket.assignedEmployeeEmail}</p>
                )}
              </div>
            </div>
          </Card>
        ))}
      </div>

      <NewTicketModal open={modalOpen} onClose={() => setModalOpen(false)} onCreated={reload} />
    </div>
  );
}

function NewTicketModal({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await ticketsApi.create({ subject, description });
      onCreated();
      onClose();
      setSubject('');
      setDescription('');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Nowe zgłoszenie">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <Alert tone="danger">{error}</Alert>}
        <Input label="Temat" required value={subject} onChange={(e) => setSubject(e.target.value)} />
        <div className="flex flex-col gap-1.5">
          <label className="text-sm font-medium text-ink">Opis</label>
          <textarea
            required
            rows={4}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="px-3 py-2.5 rounded-md border border-slate/25 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-brand/40 focus:border-brand resize-none"
          />
        </div>
        <Button type="submit" loading={loading} className="mt-2">
          Wyślij zgłoszenie
        </Button>
      </form>
    </Modal>
  );
}
