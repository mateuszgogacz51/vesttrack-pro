import { useState, useRef, useEffect } from 'react';
import { instrumentsApi } from '@/api/instruments';
import type { FinancialInstrument } from '@/types/api';

export function InstrumentPicker({
  onSelect,
  selected
}: {
  onSelect: (instrument: FinancialInstrument) => void;
  selected: FinancialInstrument | null;
}) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<FinancialInstrument[]>([]);
  const [open, setOpen] = useState(false);
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

  useEffect(() => {
    if (query.trim().length < 1) {
      setResults([]);
      return;
    }
    const handle = setTimeout(async () => {
      try {
        const data = await instrumentsApi.search(query.trim());
        setResults(data);
      } catch {
        setResults([]);
      }
    }, 250);
    return () => clearTimeout(handle);
  }, [query]);

  return (
    <div className="relative flex flex-col gap-1.5" ref={containerRef}>
      <label className="text-sm font-medium text-ink">Instrument</label>
      <input
        className="px-3 py-2.5 rounded-md border border-slate/25 text-sm text-ink bg-white
          focus:outline-none focus:ring-2 focus:ring-brand/40 focus:border-brand"
        placeholder={selected ? `${selected.ticker} — ${selected.name}` : 'Wpisz ticker lub nazwę spółki/ETF...'}
        value={query}
        onChange={(e) => {
          setQuery(e.target.value);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
      />
      {open && results.length > 0 && (
        <div className="absolute top-full mt-1 w-full bg-white border border-slate/15 rounded-md shadow-card z-10 max-h-56 overflow-y-auto">
          {results.map((instrument) => (
            <button
              type="button"
              key={instrument.id}
              onClick={() => {
                onSelect(instrument);
                setQuery('');
                setOpen(false);
              }}
              className="w-full text-left px-3 py-2 text-sm hover:bg-brand-50 flex justify-between items-center"
            >
              <span>
                <span className="font-medium text-ink">{instrument.ticker}</span>{' '}
                <span className="text-slate">{instrument.name}</span>
              </span>
              <span className="text-xs text-slate-light">{instrument.assetType}</span>
            </button>
          ))}
        </div>
      )}
      {open && query.length >= 1 && results.length === 0 && (
        <div className="absolute top-full mt-1 w-full bg-white border border-slate/15 rounded-md shadow-card z-10 px-3 py-2 text-sm text-slate">
          Brak wyników. Poproś Pracownika o dodanie instrumentu przez zgłoszenie.
        </div>
      )}
    </div>
  );
}
