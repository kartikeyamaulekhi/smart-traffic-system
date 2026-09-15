import { useEffect, useState } from 'react';
import type { CongestionLevel } from '../api/types';
import { CONGESTION_COLOR } from '../api/types';

const LEVELS: CongestionLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'SEVERE'];

interface Props {
  avgSpeedKmh: number | null;
  counts: Partial<Record<CongestionLevel, number>>;
  updatedAt: number | null;
  loading?: boolean;
}

export function MapStatusChip({ avgSpeedKmh, counts, updatedAt, loading }: Props) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, []);

  const age = updatedAt ? Math.max(0, Math.round((now - updatedAt) / 1000)) : null;
  const any = LEVELS.some((l) => (counts[l] ?? 0) > 0);

  return (
    <div className="map-status-chip" title="Real-time feed">
      <span className={`status-chip-dot${loading ? ' pulse' : ''}`} />
      <span className="status-chip-live">LIVE</span>
      {avgSpeedKmh != null && <b className="status-chip-speed">{avgSpeedKmh.toFixed(0)} km/h</b>}
      {any && (
        <span className="status-chip-legs">
          {LEVELS.filter((l) => (counts[l] ?? 0) > 0).map((l) => (
            <span key={l} className="status-chip-leg" title={`${counts[l]} ${l}`}>
              <i style={{ background: CONGESTION_COLOR[l], boxShadow: `0 0 6px ${CONGESTION_COLOR[l]}` }} />
              {counts[l]}
            </span>
          ))}
        </span>
      )}
      {age != null && <span className="status-chip-age">updated {age}s ago</span>}
    </div>
  );
}