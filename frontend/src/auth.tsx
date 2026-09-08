import { createContext, useContext, useCallback, useState, type ReactNode } from 'react';
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

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [email, setEmail] = useState<string | null>(() => localStorage.getItem(EMAIL_KEY));

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

  const signOut = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(EMAIL_KEY);
    setToken(null);
    setEmail(null);
  }, []);

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