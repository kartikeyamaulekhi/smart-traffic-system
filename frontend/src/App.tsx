import { useState } from 'react';
import { AuthProvider, useAuth } from './auth';
import { AuthScreen } from './components/AuthScreen';
import { Dashboard } from './components/Dashboard';
import { RoutePlanner } from './components/RoutePlanner';
import { History } from './components/History';

type Tab = 'dashboard' | 'routes' | 'history';

function Shell() {
  const { token, email, signOut } = useAuth();
  const [tab, setTab] = useState<Tab>('dashboard');

  if (!token) return <AuthScreen />;

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-brand">🚦 Smart Traffic</div>
        <nav className="tabs">
          <button className={tab === 'dashboard' ? 'tab active' : 'tab'} onClick={() => setTab('dashboard')}>
            Dashboard
          </button>
          <button className={tab === 'routes' ? 'tab active' : 'tab'} onClick={() => setTab('routes')}>
            Routes
          </button>
          <button className={tab === 'history' ? 'tab active' : 'tab'} onClick={() => setTab('history')}>
            History
          </button>
        </nav>
        <div className="user">
          <span className="muted">{email}</span>
          <button onClick={signOut}>Sign out</button>
        </div>
      </header>

      <main className="app-main">
        {tab === 'dashboard' && <Dashboard />}
        {tab === 'routes' && <RoutePlanner />}
        {tab === 'history' && <History />}
      </main>
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