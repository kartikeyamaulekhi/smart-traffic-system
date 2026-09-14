import { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './auth';
import { AuthScreen } from './components/AuthScreen';
import { Dashboard } from './components/Dashboard';
import { RoutePlanner } from './components/RoutePlanner';
import { History } from './components/History';
import { ConnectionStatus } from './components/ConnectionStatus';
import { ToastProvider } from './components/Toast';
import { ErrorBoundary } from './components/ErrorBoundary';

type Tab = 'dashboard' | 'routes' | 'history';

const TABS: Array<{ id: Tab; label: string; icon: string }> = [
  { id: 'dashboard', label: 'Live map', icon: '📡' },
  { id: 'routes', label: 'Routes', icon: '🧭' },
  { id: 'history', label: 'History', icon: '📈' },
];

function Shell() {
  const { token, email, signOut } = useAuth();
  const [tab, setTab] = useState<Tab>('dashboard');
  const [viewKey, setViewKey] = useState(0);

  const switchTab = (t: Tab) => {
    if (t !== tab) {
      setTab(t);
      setViewKey((k) => k + 1);
    }
  };

  useEffect(() => {
    document.body.classList.add('app-loaded');
  }, []);

  if (!token) return <AuthScreen />;

  return (
    <div className="app">
      <header className="app-header glass">
        <div className="app-brand">
          <span className="brand-logo">🚦</span>
          <span className="brand-text">Smart<span className="grad-text">Traffic</span></span>
        </div>
        <nav className="nav-tabs desktop-only">
          {TABS.map((t) => (
            <button key={t.id} className={tab === t.id ? 'nav-tab active' : 'nav-tab'} onClick={() => switchTab(t.id)}>
              <span className="nav-icon">{t.icon}</span>
              {t.label}
            </button>
          ))}
        </nav>
        <div className="header-right">
          <ConnectionStatus />
          <div className="user-chip">
            <span className="avatar">{email?.charAt(0).toUpperCase() ?? '?'}</span>
            <span className="user-email muted">{email}</span>
            <button className="ghost-btn" onClick={signOut}>Sign out</button>
          </div>
        </div>
      </header>

      <main className="app-main" key={viewKey}>
        <div className="page-enter">
          <ErrorBoundary>
            {tab === 'dashboard' && <Dashboard />}
            {tab === 'routes' && <RoutePlanner />}
            {tab === 'history' && <History />}
          </ErrorBoundary>
        </div>
      </main>

      <footer className="app-footer muted">
        Microservices · Kafka · Redis · Flyway · Dijkstra routing · ML forecasts on{' '}
        <span className="grad-text">ghcr.io/kartikeyamaulekhi/smart-traffic-system</span>
      </footer>

      <nav className="mobile-nav">
        {TABS.map((t) => (
          <button key={t.id} className={tab === t.id ? 'mobile-nav-item active' : 'mobile-nav-item'} onClick={() => switchTab(t.id)}>
            <span className="mobile-nav-icon">{t.icon}</span>
            <span className="mobile-nav-label">{t.label}</span>
          </button>
        ))}
      </nav>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <Shell />
      </ToastProvider>
    </AuthProvider>
  );
}
