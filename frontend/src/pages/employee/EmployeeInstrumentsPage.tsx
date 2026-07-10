import { useState, FormEvent } from 'react';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Alert } from '@/components/ui/Alert';
import { Badge } from '@/components/ui/Badge';
import { Table, Column } from '@/components/ui/Table';
import { Modal } from '@/components/ui/Modal';
import { employeeApi } from '@/api/employee';
import { extractErrorMessage } from '@/api/client';
import { formatMoney } from '@/utils/format';
import type { AssetType, FinancialInstrument } from '@/types/api';

export function EmployeeInstrumentsPage() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<FinancialInstrument[] | null>(null);
  const [createOpen, setCreateOpen] = useState(false);

  async function search(e?: FormEvent) {
    e?.preventDefault();
    if (!query.trim()) return;
    setResults(await employeeApi.searchInstruments(query.trim()));
  }

  const columns: Column<FinancialInstrument>[] = [
    { header: 'Ticker', accessor: (i) => <span className="font-medium">{i.ticker}</span> },
    { header: 'Nazwa', accessor: (i) => i.name },
    { header: 'Typ', accessor: (i) => <Badge tone="neutral">{i.assetType}</Badge> },
    { header: 'Giełda', accessor: (i) => i.exchange ?? '—' },
    { header: 'ISIN', accessor: (i) => i.isin ?? '—' },
    { header: 'Ostatnia cena', accessor: (i) => formatMoney(i.lastPrice, i.quoteCurrency), align: 'right', mono: true },
    {
      header: 'Status',
      accessor: (i) => (i.blocked ? <Badge tone="danger">Zablokowany</Badge> : <Badge tone="success">Aktywny</Badge>)
    }
  ];

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-display text-3xl text-ink">Słownik instrumentów</h1>
          <p className="text-slate mt-1">Weryfikacja i uzupełnianie bazy tickerów zgłaszanych przez użytkowników</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>+ Dodaj instrument</Button>
      </div>

      <Card className="mb-6">
        <form onSubmit={search} className="flex gap-3 items-end">
          <div className="flex-1">
            <Input
              label="Szukaj po tickerze lub nazwie"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="np. XTB, PZU, MSCI ACWI"
            />
          </div>
          <Button type="submit">Szukaj</Button>
        </form>
      </Card>

      {results && (
        <Card>
          {results.length === 0 ? (
            <p className="text-sm text-slate py-6 text-center">Brak wyników dla zapytania „{query}”.</p>
          ) : (
            <Table columns={columns} rows={results} rowKey={(i) => i.id} />
          )}
        </Card>
      )}

      <CreateInstrumentModal open={createOpen} onClose={() => setCreateOpen(false)} onCreated={() => search()} />
    </div>
  );
}

function CreateInstrumentModal({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
  const [ticker, setTicker] = useState('');
  const [name, setName] = useState('');
  const [assetType, setAssetType] = useState<AssetType>('STOCK');
  const [isin, setIsin] = useState('');
  const [exchange, setExchange] = useState('');
  const [quoteCurrency, setQuoteCurrency] = useState('PLN');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await employeeApi.createInstrument({
        ticker,
        name,
        assetType,
        isin: isin || undefined,
        exchange: exchange || undefined,
        quoteCurrency
      });
      onCreated();
      onClose();
      setTicker('');
      setName('');
      setIsin('');
      setExchange('');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Dodaj instrument do słownika">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <Alert tone="danger">{error}</Alert>}
        <div className="grid grid-cols-2 gap-3">
          <Input label="Ticker" required value={ticker} onChange={(e) => setTicker(e.target.value)} />
          <Select label="Typ aktywa" value={assetType} onChange={(e) => setAssetType(e.target.value as AssetType)}>
            <option value="STOCK">Akcja</option>
            <option value="ETF">ETF</option>
            <option value="BOND">Obligacja</option>
            <option value="OTHER">Inne</option>
          </Select>
        </div>
        <Input label="Nazwa" required value={name} onChange={(e) => setName(e.target.value)} />
        <div className="grid grid-cols-2 gap-3">
          <Input label="ISIN (opcjonalnie)" value={isin} onChange={(e) => setIsin(e.target.value)} />
          <Input label="Giełda (opcjonalnie)" value={exchange} onChange={(e) => setExchange(e.target.value)} placeholder="np. GPW" />
        </div>
        <Select label="Waluta notowania" value={quoteCurrency} onChange={(e) => setQuoteCurrency(e.target.value)}>
          <option value="PLN">PLN</option>
          <option value="USD">USD</option>
          <option value="EUR">EUR</option>
        </Select>
        <Button type="submit" loading={loading} className="mt-2">
          Dodaj instrument
        </Button>
      </form>
    </Modal>
  );
}
