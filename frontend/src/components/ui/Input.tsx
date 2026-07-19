import { InputHTMLAttributes, forwardRef, ReactNode } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, className = '', id, ...rest }, ref) => {
    const inputId = id || rest.name;
    return (
      <div className="flex flex-col gap-1.5">
        {label && (
          <label htmlFor={inputId} className="text-sm font-medium text-ink">
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          className={`px-3 py-2.5 rounded-md border text-sm text-ink placeholder:text-slate-light bg-white
            focus:outline-none focus:ring-2 focus:ring-brand/40 focus:border-brand
            ${error ? 'border-crimson' : 'border-slate/25'} ${className}`}
          {...rest}
        />
        {error && <span className="text-xs text-crimson">{error}</span>}
        {hint && !error && <span className="text-xs text-slate">{hint}</span>}
      </div>
    );
  }
);
Input.displayName = 'Input';
