import { useState, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { AuthLayout } from '@/layouts/AuthLayout';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Alert } from '@/components/ui/Alert';
import { authApi } from '@/api/auth';
import { extractErrorMessage } from '@/api/client';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await authApi.forgotPassword(email);
      setSent(true);
    } catch (err) {
      // Ze wzgledow bezpieczenstwa (nie ujawniamy czy e-mail istnieje w bazie)
      // backend zawsze zwraca sukces - blad tutaj oznacza realny problem sieciowy.
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  if (sent) {
    return (
      <AuthLayout title="Sprawdź swoją skrzynkę">
        <Alert tone="success">
          Jeśli podany adres e-mail istnieje w naszej bazie, wysłaliśmy na niego link do zresetowania hasła. Link
          jest ważny przez 30 minut.
        </Alert>
        <p className="text-sm text-slate mt-6 text-center">
          <Link to="/login" className="text-brand font-medium hover:underline">
            Wróć do logowania
          </Link>
        </p>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout
      title="Nie pamiętasz hasła?"
      subtitle="Podaj adres e-mail powiązany z kontem, a wyślemy Ci link do ustawienia nowego hasła."
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <Alert tone="danger">{error}</Alert>}
        <Input label="Adres e-mail" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        <Button type="submit" loading={loading} className="mt-2">
          Wyślij link resetujący
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
