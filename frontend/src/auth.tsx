import { createContext, useContext, useCallback, useEffect, useState, type ReactNode } from 'react';
import { api } from './api/client';

interface AuthState {
  token: string | null;
  email: string | null;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string) => Promise<void>;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

const TOKEN_KEY = 'smart-traffic-token';
const EMAIL_KEY = 'smart-traffic-email';

function decodeExpiry(token: string): number | null {
  try {
    const payload = token.split('.')[1];
    const b64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = JSON.parse(atob(b64));
    return typeof json.exp === 'number' ? json.exp * 1000 : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [email, setEmail] = useState<string | null>(() => localStorage.getItem(EMAIL_KEY));

  const signOut = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(EMAIL_KEY);
    setToken(null);
    setEmail(null);
  }, []);

  // React to a 401/403 from any API call — and to an expired JWT — by
  // clearing the session so the user lands back on the login screen.
  useEffect(() => {
    const onUnauthorized = () => signOut();
    window.addEventListener('smart-traffic:unauthorized', onUnauthorized);
    return () => window.removeEventListener('smart-traffic:unauthorized', onUnauthorized);
  }, [signOut]);

  useEffect(() => {
    if (!token) return;
    const exp = decodeExpiry(token);
    if (exp == null) return;
    const delay = Math.max(0, exp - Date.now());
    const t = setTimeout(signOut, delay + 1000);
    return () => clearTimeout(t);
  }, [token, signOut]);

  const adoptSession = useCallback((newToken: string, newEmail: string) => {
    localStorage.setItem(TOKEN_KEY, newToken);
    localStorage.setItem(EMAIL_KEY, newEmail);
    setToken(newToken);
    setEmail(newEmail);
  }, []);

  const signIn = useCallback(
    async (loginEmail: string, password: string) => {
      const res = await api.login(loginEmail, password);
      adoptSession(res.token, res.email);
    },
    [adoptSession],
  );

  const signUp = useCallback(
    async (registerEmail: string, password: string) => {
      const res = await api.register(registerEmail, password);
      adoptSession(res.token, res.email);
    },
    [adoptSession],
  );

  return (
    <AuthContext.Provider value={{ token, email, signIn, signUp, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}