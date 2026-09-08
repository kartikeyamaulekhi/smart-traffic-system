import { useState } from 'react';
import { AuthProvider, useAuth } from './auth';
import { AuthScreen } from './components/AuthScreen';
import { Dashboard } from './components/Dashboard';
import { RoutePlanner } from './components/RoutePlanner';
import { History } from './components/History';

type Tab = 'dashboard' | 'routes' | 'history';

const TABS: Array<{ id: Tab; label: string; icon: string }> = [
  { id: 'dashboard', label: 'Live map', icon: '🛰️' },
  { id: 'routes', label: 'Route planner', icon: '🧭' },
  { id: 'history', label: 'History', icon: '📈' },
];

function Shell() {
  const { token, email, signOut } = useAuth();
  const [tab, setTab] = useState<Tab>('dashboard');

  if (!token) return <AuthScreen />;

  return (
    <div className="app">
      <header className="app-header glass">
        <div className="app-brand">
          <span className="brand-logo">🚦</span>
          <span>Smart<span className="grad-text">Traffic</span></span>
        </div>
        <nav className="nav-tabs">
          {TABS.map((t) => (
            <button key={t.id} className={tab === t.id ? 'nav-tab active' : 'nav-tab'} onClick={() => setTab(t.id)}>
              <span className="nav-icon">{t.icon}</span>
              {t.label}
            </button>
          ))}
        </nav>
        <div className="user-chip">
          <span className="avatar">{email?.charAt(0).toUpperCase() ?? '?'}</span>
          <span className="user-email muted">{email}</span>
          <button className="ghost-btn" onClick={signOut}>Sign out</button>
        </div>
      </header>

      <main className="app-main">
        {tab === 'dashboard' && <Dashboard />}
        {tab === 'routes' && <RoutePlanner />}
        {tab === 'history' && <History />}
      </main>

      <footer className="app-footer muted">
        Microservices · Kafka · Redis · Flyway · Dijkstra routing · ML forecasts on <span className="grad-text">ghcr.io/kartikeyamaulekhi/smart-traffic-system</span>
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <Shell />
    </AuthProvider>
  );
}