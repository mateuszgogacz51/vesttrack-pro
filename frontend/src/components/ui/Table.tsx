import { ReactNode } from 'react';

export interface Column<T> {
  header: string;
  accessor: (row: T) => ReactNode;
  align?: 'left' | 'right' | 'center';
  mono?: boolean;
}

export function Table<T>({ columns, rows, rowKey }: { columns: Column<T>[]; rows: T[]; rowKey: (row: T) => string | number }) {
  return (
    <div className="overflow-x-auto -mx-5 px-5">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-slate/15 text-left">
            {columns.map((col, i) => (
              <th
                key={i}
                className={`pb-2.5 font-medium text-slate text-xs uppercase tracking-wide whitespace-nowrap
                  ${col.align === 'right' ? 'text-right' : col.align === 'center' ? 'text-center' : 'text-left'}`}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={rowKey(row)} className="border-b border-slate/8 last:border-0 hover:bg-brand-50/40">
              {columns.map((col, i) => (
                <td
                  key={i}
                  className={`py-3 text-ink whitespace-nowrap
                    ${col.mono ? 'num' : ''}
                    ${col.align === 'right' ? 'text-right' : col.align === 'center' ? 'text-center' : 'text-left'}`}
                >
                  {col.accessor(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
