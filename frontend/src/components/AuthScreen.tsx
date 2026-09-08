import { useState, type FormEvent } from 'react';
import { useAuth } from '../auth';

const PILLARS = [
  { icon: '🛣️', title: 'Real road network', text: 'Mussoorie Road, NH-72 & city corridors modelled as a graph.' },
  { icon: '🔮', title: 'ML congestion forecasts', text: 'Hourly predictions per road, powered by the pipeline you trained.' },
  { icon: '⚡', title: 'Congestion-aware routing', text: 'Fastest path by live + predicted travel time, not just distance.' },
  { icon: '🏫', title: 'Campus coverage', text: 'DIT, IMS Unison, Graphic Era, Uttaranchal & UPES all on the map.' },
];

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
    <div className="hero">
      <div className="hero-bg" />
      <main className="hero-grid">
        <section className="hero-copy">
          <div className="hero-kicker">Dehradun · Saharanpur · Live Network</div>
          <h1 className="hero-title">
            Roads that <span className="grad-text">think</span>.
          </h1>
          <p className="hero-sub">
            A distributed traffic platform — microservices streaming congestion over Kafka,
            forecasting it with machine learning, and rerouting you in real time.
          </p>
          <div className="pillars">
            {PILLARS.map((p) => (
              <div className="pillar" key={p.title}>
                <span className="pillar-icon">{p.icon}</span>
                <span>
                  <strong>{p.title}</strong>
                  <small>{p.text}</small>
                </span>
              </div>
            ))}
          </div>
        </section>

        <section className="card auth-card glass">
          <div className="auth-tabs">
            <button type="button" className={mode === 'login' ? 'auth-tab active' : 'auth-tab'} onClick={() => { setMode('login'); setError(''); }}>
              Sign in
            </button>
            <button type="button" className={mode === 'register' ? 'auth-tab active' : 'auth-tab'} onClick={() => { setMode('register'); setError(''); }}>
              Create account
            </button>
          </div>
          <form onSubmit={onSubmit}>
            <h2>{mode === 'login' ? 'Welcome back' : 'Join the network'}</h2>
            <label>
              Email
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus placeholder="you@example.com" />
            </label>
            <label>
              Password
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} placeholder="••••••••" />
            </label>
            {error && <div className="error">{error}</div>}
            <button type="submit" className="btn-primary" disabled={busy}>
              {busy ? 'Please wait…' : mode === 'login' ? 'View live dashboard →' : 'Get started →'}
            </button>
            <p className="auth-hint muted">
              {mode === 'login' ? 'New here? Use “Create account” — registration takes 10 seconds.' : 'Free demo access. Watch the corridor move.'}
            </p>
          </form>
        </section>
      </main>
    </div>
  );
}