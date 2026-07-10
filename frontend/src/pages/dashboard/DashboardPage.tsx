import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { EmptyState } from '@/components/ui/EmptyState';
import { Spinner } from '@/components/ui/Spinner';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Alert } from '@/components/ui/Alert';
import { accountsApi } from '@/api/accounts';
import { extractErrorMessage } from '@/api/client';
import { formatMoney } from '@/utils/format';
import type { AccountResponse, AccountType } from '@/types/api';

const accountTypeLabel: Record<AccountType, string> = {
  REGULAR: 'Standardowe',
  IKE: 'IKE',
  IKZE: 'IKZE'
};

export function DashboardPage() {
  const [accounts, setAccounts] = useState<AccountResponse[] | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  async function reload() {
    const data = await accountsApi.list();
    setAccounts(data);
  }

  useEffect(() => {
    reload();
  }, []);

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-display text-3xl text-ink">Pulpit</h1>
          <p className="text-slate mt-1">Przegląd Twoich rachunków inwestycyjnych</p>
        </div>
        <Button onClick={() => setModalOpen(true)}>+ Nowe konto</Button>
      </div>

      {accounts === null && (
        <div className="flex justify-center py-20">
          <Spinner size={28} />
        </div>
      )}

      {accounts !== null && accounts.length === 0 && (
        <Card>
          <EmptyState
            title="Nie masz jeszcze żadnego rachunku"
            description="Utwórz swoje pierwsze konto inwestycyjne (standardowe, IKE lub IKZE), aby zacząć rejestrować transakcje."
            action={<Button onClick={() => setModalOpen(true)}>Utwórz pierwsze konto</Button>}
          />
        </Card>
      )}

      {accounts !== null && accounts.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {accounts.map((account) => (
            <Link key={account.id} to={`/accounts/${account.id}`}>
              <Card className="hover:shadow-md hover:-translate-y-0.5 transition-all duration-150 cursor-pointer h-full">
                <div className="flex items-center justify-between mb-3">
                  <Badge tone={account.accountType === 'REGULAR' ? 'neutral' : 'brand'}>
                    {accountTypeLabel[account.accountType]}
                  </Badge>
                  {!account.active && <Badge tone="danger">Nieaktywne</Badge>}
                </div>
                <h3 className="font-display text-lg text-ink mb-1">{account.name}</h3>
                <p className="text-sm text-slate mb-4">Waluta bazowa: {account.currency}</p>

                {account.accountType !== 'REGULAR' && account.annualContributionLimit && (
                  <div>
                    <div className="flex justify-between text-xs text-slate mb-1">
                      <span>Wykorzystanie limitu wpłat</span>
                      <span className="num">
                        {formatMoney(account.contributedThisYear, account.currency)} /{' '}
                        {formatMoney(account.annualContributionLimit, account.currency)}
                      </span>
                    </div>
                    <div className="h-1.5 bg-slate/10 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-clay"
                        style={{
                          width: `${Math.min(
                            100,
                            (account.contributedThisYear / account.annualContributionLimit) * 100
                          )}%`
                        }}
                      />
                    </div>
                  </div>
                )}
              </Card>
            </Link>
          ))}
        </div>
      )}

      <CreateAccountModal open={modalOpen} onClose={() => setModalOpen(false)} onCreated={reload} />
    </div>
  );
}

function CreateAccountModal({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState('');
  const [accountType, setAccountType] = useState<AccountType>('REGULAR');
  const [currency, setCurrency] = useState('PLN');
  const [limit, setLimit] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await accountsApi.create({
        name,
        accountType,
        currency,
        annualContributionLimit: limit ? Number(limit) : null
      });
      onCreated();
      onClose();
      setName('');
      setLimit('');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Nowe konto inwestycyjne">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <Alert tone="danger">{error}</Alert>}
        <Input label="Nazwa konta" required value={name} onChange={(e) => setName(e.target.value)} placeholder="np. Konto maklerskie XTB" />
        <Select label="Typ konta" value={accountType} onChange={(e) => setAccountType(e.target.value as AccountType)}>
          <option value="REGULAR">Standardowe</option>
          <option value="IKE">IKE</option>
          <option value="IKZE">IKZE</option>
        </Select>
        <Select label="Waluta bazowa" value={currency} onChange={(e) => setCurrency(e.target.value)}>
          <option value="PLN">PLN</option>
          <option value="USD">USD</option>
          <option value="EUR">EUR</option>
        </Select>
        {accountType !== 'REGULAR' && (
          <Input
            label="Roczny limit wpłat (opcjonalnie)"
            type="number"
            step="0.01"
            value={limit}
            onChange={(e) => setLimit(e.target.value)}
            placeholder="np. 26019.00"
          />
        )}
        <Button type="submit" loading={loading} className="mt-2">
          Utwórz konto
        </Button>
      </form>
    </Modal>
  );
}
