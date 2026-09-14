import { useEffect, useState } from 'react';

type Status = 'ok' | 'degraded' | 'down' | 'checking';

const POLL_MS = 30_000;

export function ConnectionStatus() {
  const [status, setStatus] = useState<Status>('checking');

  useEffect(() => {
    let mounted = true;
    const check = async () => {
      try {
        const res = await fetch('/actuator/health', { signal: AbortSignal.timeout(5000) });
        if (!mounted) return;
        if (res.ok) {
          const body = await res.json().catch(() => null);
          if (body?.status === 'UP') setStatus('ok');
          else if (body?.status === 'DEGRADED') setStatus('degraded');
          else setStatus('down');
        } else {
          setStatus('down');
        }
      } catch {
        if (mounted) setStatus('down');
      }
    };
    check();
    const t = setInterval(check, POLL_MS);
    return () => { mounted = false; clearInterval(t); };
  }, []);

  const color = status === 'ok' ? '#22c55e' : status === 'degraded' ? '#f59e0b' : status === 'checking' ? '#7d8db0' : '#ef4444';
  const label = status === 'ok' ? 'All systems operational' : status === 'degraded' ? 'Partial degradation' : status === 'checking' ? 'Checking…' : 'Backend unreachable';
  const pulseClass = status !== 'ok' ? 'health-pulse' : '';

  return (
    <div className="health-indicator" title={label}>
      <span className={`health-dot ${pulseClass}`} style={{ background: color, boxShadow: `0 0 6px ${color}` }} />
      <span className="health-label">{label}</span>
    </div>
  );
}
