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
  const [showPw, setShowPw] = useState(false);
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

  const pwValid = password.length >= 8;
  const emailValid = email.includes('@') && email.includes('.');

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
          <div className="hero-tech-badges">
            <span className="tech-badge">Spring Boot 3</span>
            <span className="tech-badge">Kafka</span>
            <span className="tech-badge">Redis</span>
            <span className="tech-badge">Resilience4j</span>
            <span className="tech-badge">Grafana</span>
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
              <span>Email</span>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoFocus
                placeholder="you@example.com"
                autoComplete="email"
                className={email.length > 0 && !emailValid ? 'input-error' : ''}
              />
              {email.length > 0 && !emailValid && <span className="field-hint error-hint">Enter a valid email address</span>}
            </label>
            <label>
              <span>Password</span>
              <div className="password-field">
                <input
                  type={showPw ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={8}
                  placeholder="At least 8 characters"
                  autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                  className={password.length > 0 && !pwValid ? 'input-error' : ''}
                />
                <button
                  type="button"
                  className="pw-toggle"
                  onClick={() => setShowPw((v) => !v)}
                  tabIndex={-1}
                  aria-label={showPw ? 'Hide password' : 'Show password'}
                >
                  {showPw ? '🙈' : '👁️'}
                </button>
              </div>
              {mode === 'register' && password.length > 0 && (
                <span className={`field-hint ${pwValid ? 'valid-hint' : 'error-hint'}`}>
                  {pwValid ? '✓ Password looks good' : 'Minimum 8 characters required'}
                </span>
              )}
            </label>
            {error && <div className="error">{error}</div>}
            <button type="submit" className="btn-primary" disabled={busy || !emailValid || (mode === 'register' && !pwValid)}>
              {busy ? (
                <span className="btn-loading">
                  <span className="spinner" />
                  Please wait…
                </span>
              ) : mode === 'login' ? 'View live dashboard →' : 'Get started →'}
            </button>
            <p className="auth-hint muted">
              {mode === 'login'
                ? 'New here? Use "Create account" — registration takes 10 seconds.'
                : 'Free demo access. Watch the corridor move.'}
            </p>
          </form>
        </section>
      </main>
    </div>
  );
}
