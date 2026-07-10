import { useState, FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { AuthLayout } from '@/layouts/AuthLayout';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Alert } from '@/components/ui/Alert';
import { authApi } from '@/api/auth';
import { extractErrorMessage } from '@/api/client';

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const navigate = useNavigate();

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (password !== confirm) {
      setError('Podane hasła nie są identyczne');
      return;
    }
    if (!token) {
      setError('Brak tokenu resetującego w adresie URL. Skorzystaj ponownie z linku z e-maila.');
      return;
    }

    setLoading(true);
    try {
      await authApi.resetPassword({ token, newPassword: password });
      navigate('/login', { state: { passwordResetSuccess: true } });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout title="Ustaw nowe hasło" subtitle="Wprowadź nowe hasło do swojego konta.">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <Alert tone="danger">{error}</Alert>}
        {!token && <Alert tone="danger">Ten link resetujący wygląda na niepoprawny lub niekompletny.</Alert>}
        <Input
          label="Nowe hasło"
          type="password"
          required
          minLength={8}
          hint="Minimum 8 znaków"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <Input
          label="Powtórz nowe hasło"
          type="password"
          required
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
        />
        <Button type="submit" loading={loading} className="mt-2">
          Zapisz nowe hasło
        </Button>
      </form>
      <p className="text-sm text-slate mt-6 text-center">
        <Link to="/login" className="text-brand font-medium hover:underline">
          Wróć do logowania
        </Link>
      </p>
    </AuthLayout>
  );
}
