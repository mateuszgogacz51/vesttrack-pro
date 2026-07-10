import { useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthLayout } from '@/layouts/AuthLayout';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Alert } from '@/components/ui/Alert';
import { useAuth } from '@/auth/AuthContext';
import { extractErrorMessage } from '@/api/client';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout title="Zaloguj się" subtitle="Wprowadź swoje dane, aby przejść do panelu inwestora.">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <Alert tone="danger">{error}</Alert>}
        <Input
          label="Adres e-mail"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <div>
          <Input
            label="Hasło"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <div className="text-right mt-1.5">
            <Link to="/forgot-password" className="text-xs text-brand hover:underline">
              Zapomniałeś hasła?
            </Link>
          </div>
        </div>
        <Button type="submit" loading={loading} className="mt-2">
          Zaloguj się
        </Button>
      </form>
      <p className="text-sm text-slate mt-6 text-center">
        Nie masz konta?{' '}
        <Link to="/register" className="text-brand font-medium hover:underline">
          Zarejestruj się
        </Link>
      </p>
    </AuthLayout>
  );
}
