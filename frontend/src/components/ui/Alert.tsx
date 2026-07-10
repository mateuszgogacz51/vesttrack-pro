type AlertTone = 'danger' | 'success' | 'info';

const toneClasses: Record<AlertTone, string> = {
  danger: 'bg-red-50 border-crimson/30 text-crimson',
  success: 'bg-emerald-50 border-emerald-300 text-emerald-700',
  info: 'bg-brand-50 border-brand/30 text-brand-dark'
};

export function Alert({ tone = 'info', children }: { tone?: AlertTone; children: string }) {
  return <div className={`text-sm px-3.5 py-2.5 rounded-md border ${toneClasses[tone]}`}>{children}</div>;
}
