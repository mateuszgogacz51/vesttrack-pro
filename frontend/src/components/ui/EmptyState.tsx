import { ReactNode } from 'react';

export function EmptyState({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-14 px-6">
      <div className="w-12 h-12 rounded-full bg-brand-50 flex items-center justify-center mb-4">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" className="text-brand">
          <path strokeWidth="1.6" strokeLinecap="round" d="M4 7h16M4 12h10M4 17h6" />
        </svg>
      </div>
      <h3 className="font-display text-base text-ink mb-1">{title}</h3>
      {description && <p className="text-sm text-slate max-w-sm">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
