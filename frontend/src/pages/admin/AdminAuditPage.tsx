import { useEffect, useState } from 'react';
import { Card, CardHeader } from '@/components/ui/Card';
import { Table, Column } from '@/components/ui/Table';
import { adminApi } from '@/api/admin';
import { formatDateTime } from '@/utils/format';
import type { AuditLogResponse } from '@/types/api';

export function AdminAuditPage() {
  const [logs, setLogs] = useState<AuditLogResponse[] | null>(null);
  const [page, setPage] = useState(0);

  async function reload() {
    setLogs(await adminApi.auditLog(page, 50));
  }

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const columns: Column<AuditLogResponse>[] = [
    { header: 'Data', accessor: (l) => formatDateTime(l.createdAt) },
    { header: 'Wykonał', accessor: (l) => l.actorEmail },
    { header: 'Akcja', accessor: (l) => <span className="font-medium">{l.action}</span> },
    { header: 'Szczegóły', accessor: (l) => l.details ?? '—' },
    { header: 'IP', accessor: (l) => l.ipAddress ?? '—' }
  ];

  return (
    <div>
      <div className="mb-8">
        <h1 className="font-display text-3xl text-ink">Dziennik audytu</h1>
        <p className="text-slate mt-1">Rejestr zdarzeń wrażliwych — logowania, zmiany uprawnień, dostęp administracyjny</p>
      </div>

      <Card>
        <CardHeader
          title="Ostatnie zdarzenia"
          action={
            <div className="flex gap-2">
              <button
                className="text-sm text-brand disabled:text-slate-light"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                ← Poprzednia
              </button>
              <button className="text-sm text-brand" onClick={() => setPage((p) => p + 1)}>
                Następna →
              </button>
            </div>
          }
        />
        {logs && logs.length > 0 ? (
          <Table columns={columns} rows={logs} rowKey={(l) => l.id} />
        ) : (
          <p className="text-sm text-slate py-6 text-center">Brak zdarzeń na tej stronie.</p>
        )}
      </Card>
    </div>
  );
}
