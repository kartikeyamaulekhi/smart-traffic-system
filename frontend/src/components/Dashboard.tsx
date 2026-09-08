import { useEffect, useState } from 'react';
import { useAuth } from '../auth';
import { api } from '../api/client';
import type { SegmentStatus } from '../api/types';
import { CONGESTION_COLOR } from '../api/types';
import { NetworkMap } from './NetworkMap';

const REFRESH_MS = 30000;

function hourNow(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:00:00`;
}

function SegmentRow({ seg }: { seg: SegmentStatus }) {
  const level = seg.latest?.congestionLevel ?? null;
  return (
    <div className="card segment-row">
      <div className="segment-head">
        <strong>{seg.name}</strong>
        <span className="muted">{seg.city}</span>
      </div>
      <div className="segment-body">
        {level ? (
          <span className="badge" style={{ background: CONGESTION_COLOR[level] }}>
            {level}
          </span>
        ) : (
          <span className="badge empty">NO DATA</span>
        )}
        <span>
          {seg.latest ? (
            <>
              <b>{seg.latest.avgSpeedKmh}</b> km/h · {seg.latest.vehicleCount} veh
            </>
          ) : (
            <em className="muted">no live reading yet</em>
          )}
        </span>
      </div>
      <div className="segment-prediction">
        {seg.prediction ? (
          <>
            <span className="pulse-dot" style={{ background: CONGESTION_COLOR[seg.prediction.predictedCongestionLevel] }} />
            Forecast: <b>{seg.prediction.predictedCongestionLevel}</b>{' '}
            <span className="muted">({Math.round(seg.prediction.confidence * 100)}% conf.)</span>
          </>
        ) : (
          <span className="muted">forecast unavailable</span>
        )}
      </div>
    </div>
  );
}

export function Dashboard() {
  const { token } = useAuth();
  const [statuses, setStatuses] = useState<SegmentStatus[] | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) return;
    const tk = token;
    let cancelled = false;

    async function load() {
      try {
        const segments = await api.segments(tk);
        const now = hourNow();
        const enriched = await Promise.all(
          segments.map(async (s) => {
            const [latest, prediction] = await Promise.all([
              api.latestForSegment(tk, s.id).catch(() => null),
              api.predict(tk, s.id, now).catch(() => null),
            ]);
            return { ...s, latest, prediction };
          }),
        );
        if (!cancelled) {
          setStatuses(enriched);
          setError('');
        }
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load dashboard');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    const timer = setInterval(load, REFRESH_MS);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [token]);

  if (loading && !statuses) return <div className="muted">Loading network…</div>;

  return (
    <section>
      <div className="section-head">
        <h2>Live network</h2>
        <span className="muted">refreshes every {REFRESH_MS / 1000}s</span>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="card">
        <NetworkMap
          segments={statuses ?? []}
          colorBy={(id) => statuses?.find((s) => s.id === id)?.latest?.congestionLevel ?? null}
        />
      </div>
      <div className="segment-list">
        {(statuses ?? []).map((s) => (
          <SegmentRow key={s.id} seg={s} />
        ))}
      </div>
    </section>
  );
}