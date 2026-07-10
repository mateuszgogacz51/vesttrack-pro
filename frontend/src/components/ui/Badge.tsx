type BadgeTone = 'brand' | 'clay' | 'success' | 'danger' | 'neutral';

const toneClasses: Record<BadgeTone, string> = {
  brand: 'bg-brand-50 text-brand-dark',
  clay: 'bg-clay-light/40 text-clay-dark',
  success: 'bg-emerald-50 text-emerald-700',
  danger: 'bg-red-50 text-crimson',
  neutral: 'bg-slate/10 text-slate'
};

export function Badge({ children, tone = 'neutral' }: { children: string; tone?: BadgeTone }) {
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${toneClasses[tone]}`}>
      {children}
    </span>
  );
}
