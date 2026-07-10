import { ReactNode } from 'react';

export function AuthLayout({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <div className="min-h-screen flex">
      <div className="hidden lg:flex lg:w-1/2 bg-ink text-white flex-col justify-between p-12">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-md bg-clay flex items-center justify-center font-display font-semibold text-ink">
            V
          </div>
          <span className="font-display text-xl">VestTrack Pro</span>
        </div>
        <div>
          <h1 className="font-display text-4xl leading-tight mb-4 max-w-md">
            Zarządzaj portfelem wielu klas aktywów w jednym miejscu.
          </h1>
          <p className="text-white/60 max-w-md">
            Rachunki maklerskie, IKE/IKZE, strategia Core &amp; Satellite, rozliczenie FIFO i wskaźniki TWR/MWR —
            wszystko w jednej, spójnej platformie.
          </p>
        </div>
        <p className="text-xs text-white/30">© {new Date().getFullYear()} VestTrack Pro</p>
      </div>

      <div className="flex-1 flex items-center justify-center px-6 py-12 bg-paper">
        <div className="w-full max-w-sm">
          <h2 className="font-display text-2xl text-ink mb-1.5">{title}</h2>
          {subtitle && <p className="text-sm text-slate mb-8">{subtitle}</p>}
          {children}
        </div>
      </div>
    </div>
  );
}
