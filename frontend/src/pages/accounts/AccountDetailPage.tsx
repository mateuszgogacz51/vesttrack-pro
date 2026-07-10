import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { Table, Column } from '@/components/ui/Table';
import { EmptyState } from '@/components/ui/EmptyState';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Alert } from '@/components/ui/Alert';
import { AllocationRing, AllocationLegend } from '@/components/ui/AllocationRing';
import { InstrumentPicker } from '@/components/InstrumentPicker';
import { accountsApi } from '@/api/accounts';
import { transactionsApi } from '@/api/transactions';
import { portfolioApi } from '@/api/portfolio';
import { reportsApi } from '@/api/reports';
import { extractErrorMessage } from '@/api/client';
import { formatMoney, formatPercent, formatDate } from '@/utils/format';
import type {
  AccountResponse,
  TransactionResponse,
  PortfolioAllocationResponse,
  PerformanceResponse,
  FinancialInstrument,
  TransactionType
} from '@/types/api';

type Tab = 'overview' | 'transactions' | 'reports';

export function AccountDetailPage() {
  const { accountId } = useParams<{ accountId: string }>();
  const id = Number(accountId);

  const [account, setAccount] = useState<AccountResponse | null>(null);
  const [tab, setTab] = useState<Tab>('overview');
  const [transactions, setTransactions] = useState<TransactionResponse[] | null>(null);
  const [allocation, setAllocation] = useState<PortfolioAllocationResponse | null>(null);
  const [performance, setPerformance] = useState<PerformanceResponse | null>(null);
  const [txModalOpen, setTxModalOpen] = useState(false);

  async function loadAll() {
    const accounts = await accountsApi.list();
    setAccount(accounts.find((a) => a.id === id) ?? null);
    const [tx, alloc, perf] = await Promise.all([
      transactionsApi.listByAccount(id),
      portfolioApi.allocation(id).catch(() => null),
      portfolioApi.performance(id).catch(() => null)
    ]);
    setTransactions(tx);
    setAllocation(alloc);
    setPerformance(perf);
  }

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  if (!account) {
    return (
      <div className="flex justify-center py-24">
        <Spinner size={28} />
      </div>
    );
  }

  return (
    <div>
      <Link to="/dashboard" className="text-sm text-brand hover:underline mb-3 inline-block">
        ← Wszystkie rachunki
      </Link>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="font-display text-3xl text-ink">{account.name}</h1>
          <p className="text-slate mt-1">
            {account.accountType} · {account.currency}
          </p>
        </div>
        <Button onClick={() => setTxModalOpen(true)}>+ Nowa transakcja</Button>
      </div>

      <div className="flex gap-1 border-b border-slate/15 mb-6">
        {(
          [
            ['overview', 'Przegląd'],
            ['transactions', 'Transakcje'],
            ['reports', 'Raporty']
          ] as [Tab, string][]
        ).map(([key, label]) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
              tab === key ? 'border-brand text-brand' : 'border-transparent text-slate hover:text-ink'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'overview' && <OverviewTab allocation={allocation} performance={performance} currency={account.currency} />}
      {tab === 'transactions' && <TransactionsTab transactions={transactions} />}
      {tab === 'reports' && <ReportsTab accountId={id} />}

      <AddTransactionModal
        accountId={id}
        open={txModalOpen}
        onClose={() => setTxModalOpen(false)}
        onCreated={loadAll}
      />
    </div>
  );
}

function OverviewTab({
  allocation,
  performance,
  currency
}: {
  allocation: PortfolioAllocationResponse | null;
  performance: PerformanceResponse | null;
  currency: string;
}) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <Card className="lg:col-span-1 flex flex-col items-center justify-center">
        <CardHeader title="Alokacja Core & Satellite" />
        {allocation && allocation.assets.length > 0 ? (
          <>
            <AllocationRing corePercentage={allocation.coreActualWeight} satellitePercentage={allocation.satelliteActualWeight} />
            <AllocationLegend corePercentage={allocation.coreActualWeight} satellitePercentage={allocation.satelliteActualWeight} />
          </>
        ) : (
          <p className="text-sm text-slate text-center py-8">Brak aktywów przypisanych do strategii.</p>
        )}
      </Card>

      <div className="lg:col-span-2 flex flex-col gap-5">
        <div className="grid grid-cols-3 gap-4">
          <KpiCard label="Wartość portfela" value={formatMoney(performance?.currentMarketValue, currency)} />
          <KpiCard label="TWR" value={formatPercent(performance?.twrPercentage)} tone={twrTone(performance?.twrPercentage)} />
          <KpiCard label="MWR" value={formatPercent(performance?.mwrPercentage)} tone={twrTone(performance?.mwrPercentage)} />
        </div>

        {performance?.methodologyNote && (
          <Card>
            <p className="text-xs text-slate leading-relaxed">{performance.methodologyNote}</p>
          </Card>
        )}

        {allocation && allocation.assets.length > 0 && (
          <Card>
            <CardHeader title="Rebalancing" subtitle="Sugestie na podstawie odchylenia od celu" />
            <div className="flex flex-col gap-2">
              {allocation.assets.map((asset) => (
                <div key={asset.ticker} className="flex items-center justify-between text-sm py-1.5 border-b border-slate/8 last:border-0">
                  <div className="flex items-center gap-2">
                    <Badge tone={asset.strategyRole === 'CORE' ? 'brand' : 'clay'}>{asset.strategyRole}</Badge>
                    <span className="font-medium text-ink">{asset.ticker}</span>
                  </div>
                  <span className="text-slate text-right max-w-[60%]">{asset.rebalanceSuggestion}</span>
                </div>
              ))}
            </div>
          </Card>
        )}
      </div>
    </div>
  );
}

function twrTone(value: number | null | undefined): 'success' | 'danger' | 'neutral' {
  if (value === null || value === undefined) return 'neutral';
  return value >= 0 ? 'success' : 'danger';
}

function KpiCard({ label, value, tone = 'neutral' }: { label: string; value: string; tone?: 'success' | 'danger' | 'neutral' }) {
  const toneClass = tone === 'success' ? 'text-emerald-600' : tone === 'danger' ? 'text-crimson' : 'text-ink';
  return (
    <Card>
      <p className="text-xs text-slate uppercase tracking-wide mb-1.5">{label}</p>
      <p className={`font-display text-2xl num ${toneClass}`}>{value}</p>
    </Card>
  );
}

function TransactionsTab({ transactions }: { transactions: TransactionResponse[] | null }) {
  if (transactions === null) {
    return (
      <div className="flex justify-center py-16">
        <Spinner size={24} />
      </div>
    );
  }

  if (transactions.length === 0) {
    return (
      <Card>
        <EmptyState title="Brak transakcji" description="Dodaj pierwszą transakcję kupna, sprzedaży lub dywidendy dla tego konta." />
      </Card>
    );
  }

  const columns: Column<TransactionResponse>[] = [
    { header: 'Data', accessor: (t) => formatDate(t.transactionDate) },
    { header: 'Ticker', accessor: (t) => <span className="font-medium">{t.instrumentTicker}</span> },
    {
      header: 'Typ',
      accessor: (t) => (
        <Badge tone={t.transactionType === 'BUY' ? 'brand' : t.transactionType === 'SELL' ? 'clay' : 'success'}>
          {t.transactionType}
        </Badge>
      )
    },
    { header: 'Ilość', accessor: (t) => t.quantity, align: 'right', mono: true },
    { header: 'Cena', accessor: (t) => formatMoney(t.price, t.instrumentCurrency), align: 'right', mono: true },
    { header: 'Prowizja', accessor: (t) => formatMoney(t.fee, t.instrumentCurrency), align: 'right', mono: true },
    {
      header: 'Zysk zrealizowany',
      accessor: (t) =>
        t.realizedGain !== null ? (
          <span className={t.realizedGain >= 0 ? 'text-emerald-600' : 'text-crimson'}>
            {formatMoney(t.realizedGain, t.realizedGainCurrency ?? undefined)}
          </span>
        ) : (
          '—'
        ),
      align: 'right',
      mono: true
    }
  ];

  return (
    <Card>
      <Table columns={columns} rows={transactions} rowKey={(t) => t.id} />
    </Card>
  );
}

function ReportsTab({ accountId }: { accountId: number }) {
  const [year, setYear] = useState<string>(String(new Date().getFullYear()));
  const [downloading, setDownloading] = useState<'csv' | 'pdf' | null>(null);

  async function download(type: 'csv' | 'pdf') {
    setDownloading(type);
    try {
      if (type === 'csv') await reportsApi.downloadCsv(accountId, year ? Number(year) : undefined);
      else await reportsApi.downloadPdf(accountId, Number(year));
    } finally {
      setDownloading(null);
    }
  }

  return (
    <Card className="max-w-lg">
      <CardHeader title="Eksport raportów" subtitle="Pomoc przy rocznym rozliczeniu podatkowym (PIT-38)" />
      <div className="flex flex-col gap-4">
        <Input label="Rok podatkowy" type="number" value={year} onChange={(e) => setYear(e.target.value)} />
        <div className="flex gap-3">
          <Button variant="secondary" onClick={() => download('csv')} loading={downloading === 'csv'}>
            Pobierz CSV
          </Button>
          <Button variant="secondary" onClick={() => download('pdf')} loading={downloading === 'pdf'}>
            Pobierz PDF
          </Button>
        </div>
        <p className="text-xs text-slate">
          Raport PDF zawiera szacowany podatek od dochodów kapitałowych (19%). Ma charakter pomocniczy — skonsultuj
          się z księgowym przed złożeniem deklaracji.
        </p>
      </div>
    </Card>
  );
}

function AddTransactionModal({
  accountId,
  open,
  onClose,
  onCreated
}: {
  accountId: number;
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}) {
  const [instrument, setInstrument] = useState<FinancialInstrument | null>(null);
  const [type, setType] = useState<TransactionType>('BUY');
  const [quantity, setQuantity] = useState('');
  const [price, setPrice] = useState('');
  const [fee, setFee] = useState('0');
  const [exchangeRate, setExchangeRate] = useState('1');
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!instrument) {
      setError('Wybierz instrument z listy podpowiedzi');
      return;
    }
    setLoading(true);
    try {
      await transactionsApi.create({
        accountId,
        instrumentId: instrument.id,
        transactionType: type,
        quantity: Number(quantity),
        price: Number(price),
        fee: Number(fee || 0),
        instrumentCurrency: instrument.quoteCurrency,
        exchangeRate: Number(exchangeRate || 1),
        transactionDate: date
      });
      onCreated();
      onClose();
      setInstrument(null);
      setQuantity('');
      setPrice('');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Nowa transakcja">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <Alert tone="danger">{error}</Alert>}
        <InstrumentPicker selected={instrument} onSelect={setInstrument} />
        <Select label="Typ transakcji" value={type} onChange={(e) => setType(e.target.value as TransactionType)}>
          <option value="BUY">Kupno</option>
          <option value="SELL">Sprzedaż</option>
          <option value="DIVIDEND">Dywidenda</option>
        </Select>
        <div className="grid grid-cols-2 gap-3">
          <Input label="Ilość" type="number" step="0.000001" required value={quantity} onChange={(e) => setQuantity(e.target.value)} />
          <Input label="Cena jednostkowa" type="number" step="0.000001" required value={price} onChange={(e) => setPrice(e.target.value)} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Input label="Prowizja" type="number" step="0.01" value={fee} onChange={(e) => setFee(e.target.value)} />
          <Input
            label="Kurs wymiany do PLN"
            type="number"
            step="0.0001"
            value={exchangeRate}
            onChange={(e) => setExchangeRate(e.target.value)}
            hint="1.0 jeśli instrument w PLN"
          />
        </div>
        <Input label="Data transakcji" type="date" required value={date} onChange={(e) => setDate(e.target.value)} />
        <Button type="submit" loading={loading} className="mt-2">
          Zapisz transakcję
        </Button>
      </form>
    </Modal>
  );
}
