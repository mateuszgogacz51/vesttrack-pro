import { useEffect, useState } from 'react';
import { Card, CardHeader } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { Alert } from '@/components/ui/Alert';
import { adminApi } from '@/api/admin';
import { extractErrorMessage } from '@/api/client';
import type { ApiUsageResponse, ProviderConfigResponse } from '@/types/api';

export function AdminApiUsagePage() {
  const [usage, setUsage] = useState<ApiUsageResponse[] | null>(null);
  const [configs, setConfigs] = useState<ProviderConfigResponse[] | null>(null);
  const [editing, setEditing] = useState<ProviderConfigResponse | null>(null);

  async function reload() {
    const [u, c] = await Promise.all([adminApi.apiUsageToday(), adminApi.providerConfigs()]);
    setUsage(u);
    setConfigs(c);
  }

  useEffect(() => {
    reload();
  }, []);

  return (
    <div>
      <div className="mb-8">
        <h1 className="font-display text-3xl text-ink">Limity zewnętrznych API</h1>
        <p className="text-slate mt-1">Zużycie limitów dostawców notowań (Yahoo Finance, Alpha Vantage, NBP)</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-5 mb-8">
        {configs?.map((config) => {
          const todayUsage = usage?.find((u) => u.provider === config.provider);
          const percentage = todayUsage?.usagePercentage ?? 0;
          return (
            <Card key={config.provider}>
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-display text-base text-ink">{config.provider.replace('_', ' ')}</h3>
                <Badge tone={config.active ? 'success' : 'danger'}>{config.active ? 'Aktywny' : 'Wyłączony'}</Badge>
              </div>
              <p className="text-2xl font-display num text-ink mb-1">
                {todayUsage?.callCount ?? 0}
                {config.dailyLimit && <span className="text-sm text-slate"> / {config.dailyLimit}</span>}
              </p>
              <p className="text-xs text-slate mb-3">wywołań dzisiaj{todayUsage?.errorCount ? ` · ${todayUsage.errorCount} błędów` : ''}</p>
              {config.dailyLimit && (
                <div className="h-1.5 bg-slate/10 rounded-full overflow-hidden mb-3">
                  <div
                    className={`h-full ${percentage > 80 ? 'bg-crimson' : 'bg-brand'}`}
                    style={{ width: `${Math.min(100, percentage)}%` }}
                  />
                </div>
              )}
              <Button size="sm" variant="secondary" onClick={() => setEditing(config)}>
                Konfiguruj klucz
              </Button>
            </Card>
          );
        })}
      </div>

      {editing && (
        <EditProviderModal provider={editing} onClose={() => setEditing(null)} onSaved={reload} />
      )}
    </div>
  );
}

function EditProviderModal({
  provider,
  onClose,
  onSaved
}: {
  provider: ProviderConfigResponse;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [apiKey, setApiKey] = useState('');
  const [dailyLimit, setDailyLimit] = useState(provider.dailyLimit?.toString() ?? '');
  const [active, setActive] = useState(provider.active);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await adminApi.updateProviderConfig(provider.provider, {
        apiKey: apiKey || undefined,
        dailyLimit: dailyLimit ? Number(dailyLimit) : undefined,
        active
      });
      onSaved();
      onClose();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Modal open onClose={onClose} title={`Konfiguracja: ${provider.provider}`}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <Alert tone="danger">{error}</Alert>}
        <Input
          label="Nowy klucz API"
          placeholder={provider.hasApiKey ? '•••••••• (zostaw puste, aby nie zmieniać)' : 'Wprowadź klucz API'}
          value={apiKey}
          onChange={(e) => setApiKey(e.target.value)}
        />
        <Input label="Dzienny limit zapytań" type="number" value={dailyLimit} onChange={(e) => setDailyLimit(e.target.value)} />
        <label className="flex items-center gap-2 text-sm text-ink">
          <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} className="rounded border-slate/30" />
          Dostawca aktywny
        </label>
        <Button type="submit" loading={loading} className="mt-2">
          Zapisz konfigurację
        </Button>
      </form>
    </Modal>
  );
}
