import { useState, useRef, useEffect } from 'react';
import { institutionsApi } from '@/api/institutions';
import type { BrokerageFirm } from '@/types/api';

const categoryLabel: Record<BrokerageFirm['category'], string> = {
  BANK: 'Bank',
  BROKER: 'Biuro maklerskie',
  TFI: 'TFI'
};

export function InstitutionPicker({
  selected,
  onSelect,
  onUseCustom
}: {
  selected: BrokerageFirm | null;
  onSelect: (firm: BrokerageFirm) => void;
  onUseCustom: () => void;
}) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<BrokerageFirm[]>([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  // Doladowuje liste przy otwarciu (nawet bez wpisanego tekstu, zeby mozna bylo
  // po prostu przegladac dostepne instytucje) i przy kazdej zmianie zapytania.
  useEffect(() => {
    if (!open) return;
    setLoading(true);
    const handle = setTimeout(async () => {
      try {
        const data = await institutionsApi.search(query.trim());
        setResults(data);
      } catch {
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, 200);
    return () => clearTimeout(handle);
  }, [query, open]);

  return (
    <div className="relative flex flex-col gap-1.5" ref={containerRef}>
      <label className="text-sm font-medium text-ink">Instytucja (bank / biuro maklerskie / TFI)</label>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="px-3 py-2.5 rounded-md border border-slate/25 text-sm text-left bg-white
          focus:outline-none focus:ring-2 focus:ring-brand/40 focus:border-brand
          flex items-center justify-between gap-2"
      >
        <span className={selected ? 'text-ink' : 'text-slate-light'}>
          {selected ? selected.name : 'Wybierz z listy…'}
        </span>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" className="text-slate shrink-0">
          <path strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" d="M6 9l6 6 6-6" />
        </svg>
      </button>

      {open && (
        <div className="absolute top-full mt-1 w-full bg-white border border-slate/15 rounded-md shadow-card z-10 overflow-hidden">
          <input
            autoFocus
            className="w-full px-3 py-2 text-sm border-b border-slate/15 focus:outline-none"
            placeholder="Szukaj po nazwie…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <div className="max-h-56 overflow-y-auto">
            {loading && <div className="px-3 py-3 text-sm text-slate">Szukam…</div>}
            {!loading && results.length === 0 && (
              <div className="px-3 py-3 text-sm text-slate">Brak instytucji pasujących do wyszukiwania.</div>
            )}
            {!loading &&
              results.map((firm) => (
                <button
                  type="button"
                  key={firm.id}
                  onClick={() => {
                    onSelect(firm);
                    setQuery('');
                    setOpen(false);
                  }}
                  className="w-full text-left px-3 py-2 text-sm hover:bg-brand-50 flex justify-between items-center"
                >
                  <span className="text-ink">{firm.name}</span>
                  <span className="text-xs text-slate-light">{categoryLabel[firm.category]}</span>
                </button>
              ))}
          </div>
          <button
            type="button"
            onClick={() => {
              onUseCustom();
              setOpen(false);
            }}
            className="w-full text-left px-3 py-2.5 text-sm text-brand hover:bg-brand-50 border-t border-slate/15"
          >
            Nie widzę swojej instytucji — wpiszę nazwę ręcznie
          </button>
        </div>
      )}
    </div>
  );
}
