import { useState, type FormEvent } from 'react';
import { useAuth } from '../auth';

export function AuthScreen() {
  const { signIn, signUp } = useAuth();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      if (mode === 'login') await signIn(email, password);
      else await signUp(email, password);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-screen">
      <form className="card auth-card" onSubmit={onSubmit}>
        <h1 className="brand-title">
          <span className="brand-mark">🚦</span> Smart Traffic
        </h1>
        <p className="muted">Saharanpur road network — live congestion, forecasts & routing.</p>

        <div className="tabs">
          <button type="button" className={mode === 'login' ? 'tab active' : 'tab'} onClick={() => { setMode('login'); setError(''); }}>
            Sign in
          </button>
          <button type="button" className={mode === 'register' ? 'tab active' : 'tab'} onClick={() => { setMode('register'); setError(''); }}>
            Register
          </button>
        </div>

        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
        </label>

        {error && <div className="error">{error}</div>}

        <button type="submit" className="primary" disabled={busy}>
          {busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
        </button>
      </form>
    </div>
  );
}