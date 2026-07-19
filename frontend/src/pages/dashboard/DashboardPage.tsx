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
import { InstitutionPicker } from '@/components/InstitutionPicker';
import { accountsApi } from '@/api/accounts';
import { extractErrorMessage } from '@/api/client';
import { formatMoney } from '@/utils/format';
import type { AccountResponse, AccountType, BrokerageFirm } from '@/types/api';

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
          <p className="text-slate mt-1">Wszystkie Twoje rachunki inwestycyjne w jednym miejscu 👋</p>
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
                <p className="text-sm text-slate mb-1">
                  {account.brokerageFirmName && account.brokerageFirmName !== account.name
                    ? `${account.brokerageFirmName} · ${account.currency}`
                    : `Waluta bazowa: ${account.currency}`}
                </p>

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
  const [firm, setFirm] = useState<BrokerageFirm | null>(null);
  const [useCustomName, setUseCustomName] = useState(false);
  const [customName, setCustomName] = useState('');
  const [nickname, setNickname] = useState('');
  const [accountType, setAccountType] = useState<AccountType>('REGULAR');
  const [currency, setCurrency] = useState('PLN');
  const [limit, setLimit] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function resetForm() {
    setFirm(null);
    setUseCustomName(false);
    setCustomName('');
    setNickname('');
    setLimit('');
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!firm && !useCustomName) {
      setError('Wybierz instytucję z listy albo wpisz nazwę ręcznie.');
      return;
    }
    if (useCustomName && !customName.trim()) {
      setError('Podaj nazwę instytucji lub konta.');
      return;
    }

    setLoading(true);
    try {
      await accountsApi.create({
        name: useCustomName ? customName.trim() : nickname.trim() || null,
        brokerageFirmId: useCustomName ? null : firm?.id,
        accountType,
        currency,
        annualContributionLimit: limit ? Number(limit) : null
      });
      onCreated();
      onClose();
      resetForm();
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

        <p className="text-sm text-slate -mt-1">
          Wybierz bank, biuro maklerskie lub TFI, w którym masz swój rachunek — nie musisz wpisywać nazwy ręcznie.
        </p>

        {!useCustomName && (
          <InstitutionPicker selected={firm} onSelect={setFirm} onUseCustom={() => setUseCustomName(true)} />
        )}

        {useCustomName && (
          <Input
            label="Nazwa instytucji / konta"
            required
            value={customName}
            onChange={(e) => setCustomName(e.target.value)}
            placeholder="np. Konto maklerskie XTB"
            hint={
              <button type="button" className="text-brand hover:underline" onClick={() => setUseCustomName(false)}>
                Wróć do listy instytucji
              </button>
            }
          />
        )}

        {!useCustomName && firm && (
          <Input
            label="Własna nazwa konta (opcjonalnie)"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            placeholder={`np. „${firm.name} — emerytura”`}
            hint="Jeśli zostawisz puste, użyjemy nazwy instytucji."
          />
        )}

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
