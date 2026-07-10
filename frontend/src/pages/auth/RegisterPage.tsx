import { useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthLayout } from '@/layouts/AuthLayout';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Alert } from '@/components/ui/Alert';
import { useAuth } from '@/auth/AuthContext';
import { extractErrorMessage } from '@/api/client';

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function update(field: keyof typeof form) {
    return (e: React.ChangeEvent<HTMLInputElement>) => setForm((f) => ({ ...f, [field]: e.target.value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await register(form);
      navigate('/dashboard');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout title="Utwórz konto" subtitle="Zacznij śledzić swój portfel inwestycyjny w kilka minut.">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <Alert tone="danger">{error}</Alert>}
        <div className="grid grid-cols-2 gap-3">
          <Input label="Imię" required value={form.firstName} onChange={update('firstName')} />
          <Input label="Nazwisko" required value={form.lastName} onChange={update('lastName')} />
        </div>
        <Input label="Adres e-mail" type="email" required value={form.email} onChange={update('email')} />
        <Input
          label="Hasło"
          type="password"
          required
          minLength={8}
          hint="Minimum 8 znaków"
          value={form.password}
          onChange={update('password')}
        />
        <Button type="submit" loading={loading} className="mt-2">
          Utwórz konto
        </Button>
      </form>
      <p className="text-sm text-slate mt-6 text-center">
        Masz już konto?{' '}
        <Link to="/login" className="text-brand font-medium hover:underline">
          Zaloguj się
        </Link>
      </p>
    </AuthLayout>
  );
}
