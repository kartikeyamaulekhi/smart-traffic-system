// Thin typed fetch wrapper. All calls are same-origin: the Vite dev server and
// the production nginx container both proxy /api and /auth to the gateway.

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

const UNAUTHORIZED_EVENT = 'smart-traffic:unauthorized';

// Any component (or the auto-logout timer) can fire this; the AuthProvider
// listens and clears the stored session, returning the user to the login screen.
export function signalUnauthorized(): void {
  window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
  token?: string | null;
}

async function request<T>(path: string, { method = 'GET', body, token }: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const data = await res.json();
      message = data.message || data.error || message;
    } catch {
      // non-JSON error body; keep the generic message
    }
    if (res.status === 401 || res.status === 403) {
      signalUnauthorized();
    }
    throw new ApiError(res.status, message);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  login: (email: string, password: string) =>
    request<import('./types').AuthResponse>('/auth/login', { method: 'POST', body: { email, password } }),
  register: (email: string, password: string) =>
    request<import('./types').AuthResponse>('/auth/register', { method: 'POST', body: { email, password } }),
  segments: (token: string) => request<import('./types').RoadSegment[]>('/api/road-segments', { token }),
  latestForSegment: (token: string, id: number) =>
    request<import('./types').TrafficReading>(`/api/traffic/segment/${id}/latest`, { token }),
  historyForSegment: (token: string, id: number) =>
    request<import('./types').TrafficReading[]>(`/api/traffic/segment/${id}`, { token }),
  predict: (token: string, id: number, timestamp: string) =>
    request<import('./types').Prediction>(`/api/predictions/${id}?timestamp=${encodeURIComponent(timestamp)}`, { token }),
  ingest: (token: string, body: import('./types').TrafficDataRequest) =>
    request<import('./types').TrafficReading>('/api/traffic', { method: 'POST', body, token }),
  route: (token: string, body: import('./types').RouteRequest) =>
    request<import('./types').RouteResult>('/api/routes', { method: 'POST', body, token }),
};