import { SelectHTMLAttributes, forwardRef, ReactNode } from 'react';

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  children: ReactNode;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, className = '', id, children, ...rest }, ref) => {
    const selectId = id || rest.name;
    return (
      <div className="flex flex-col gap-1.5">
        {label && (
          <label htmlFor={selectId} className="text-sm font-medium text-ink">
            {label}
          </label>
        )}
        <select
          ref={ref}
          id={selectId}
          className={`px-3 py-2.5 rounded-md border border-slate/25 text-sm text-ink bg-white
            focus:outline-none focus:ring-2 focus:ring-brand/40 focus:border-brand ${className}`}
          {...rest}
        >
          {children}
        </select>
      </div>
    );
  }
);
Select.displayName = 'Select';
