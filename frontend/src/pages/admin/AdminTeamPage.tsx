import { useEffect, useState, FormEvent } from 'react';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Alert } from '@/components/ui/Alert';
import { Table, Column } from '@/components/ui/Table';
import { Modal } from '@/components/ui/Modal';
import { adminApi } from '@/api/admin';
import { extractErrorMessage } from '@/api/client';
import type { EmployeeStatsResponse, Role } from '@/types/api';

export function AdminTeamPage() {
  const [stats, setStats] = useState<EmployeeStatsResponse[] | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  async function reload() {
    setStats(await adminApi.employeeStats());
  }

  useEffect(() => {
    reload();
  }, []);

  const columns: Column<EmployeeStatsResponse>[] = [
    { header: 'E-mail', accessor: (e) => e.email },
    { header: 'Zamknięte zgłoszenia', accessor: (e) => e.resolvedTickets, align: 'right', mono: true },
    { header: 'W toku', accessor: (e) => e.inProgressTickets, align: 'right', mono: true }
  ];

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-display text-3xl text-ink">Zespół</h1>
          <p className="text-slate mt-1">Statystyki wydajności pracowników i zarządzanie kontami</p>
        </div>
        <Button onClick={() => setModalOpen(true)}>+ Nowe konto pracownika</Button>
      </div>

      <Card>
        <CardHeader title="Statystyki obsługi zgłoszeń" />
        {stats && stats.length > 0 ? (
          <Table columns={columns} rows={stats} rowKey={(e) => e.employeeId} />
        ) : (
          <p className="text-sm text-slate py-6 text-center">Brak jeszcze żadnych pracowników z przypisanymi zgłoszeniami.</p>
        )}
      </Card>

      <CreateEmployeeModal open={modalOpen} onClose={() => setModalOpen(false)} onCreated={reload} />
    </div>
  );
}

function CreateEmployeeModal({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
  const [form, setForm] = useState({ email: '', temporaryPassword: '', firstName: '', lastName: '', role: 'EMPLOYEE' as Role });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function update<K extends keyof typeof form>(field: K) {
    return (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm((f) => ({ ...f, [field]: e.target.value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await adminApi.createEmployee(form);
      onCreated();
      onClose();
      setForm({ email: '', temporaryPassword: '', firstName: '', lastName: '', role: 'EMPLOYEE' });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Nowe konto pracownika">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <Alert tone="danger">{error}</Alert>}
        <div className="grid grid-cols-2 gap-3">
          <Input label="Imię" required value={form.firstName} onChange={update('firstName')} />
          <Input label="Nazwisko" required value={form.lastName} onChange={update('lastName')} />
        </div>
        <Input label="Adres e-mail" type="email" required value={form.email} onChange={update('email')} />
        <Input
          label="Hasło tymczasowe"
          type="text"
          required
          minLength={8}
          hint="Pracownik powinien je zmienić po pierwszym logowaniu"
          value={form.temporaryPassword}
          onChange={update('temporaryPassword')}
        />
        <Select label="Rola" value={form.role} onChange={update('role')}>
          <option value="EMPLOYEE">Pracownik</option>
          <option value="ADMIN">Administrator</option>
        </Select>
        <Button type="submit" loading={loading} className="mt-2">
          Utwórz konto
        </Button>
      </form>
    </Modal>
  );
}
