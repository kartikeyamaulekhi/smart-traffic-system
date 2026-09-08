import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../auth';
import { api } from '../api/client';
import type { CongestionLevel, RoadSegment, TrafficReading, TrafficSource } from '../api/types';
import { CONGESTION_COLOR } from '../api/types';
import { formatInstant } from '../lib/geo';

function HistoryChart({ readings, color }: { readings: TrafficReading[]; color: string }) {
  const [w, h] = [520, 170];
  const pts = useMemo(() => {
    if (readings.length < 2) return '';
    const max = Math.max(...readings.map((r) => r.avgSpeedKmh), 1);
    const min = Math.min(...readings.map((r) => r.avgSpeedKmh), 0);
    const range = max - min || 1;
    const step = w / (readings.length - 1);
    return readings.map((r, i) => `${(i * step).toFixed(1)},${(h - 14 - ((r.avgSpeedKmh - min) / range) * (h - 28)).toFixed(1)}`).join(' ');
  }, [readings]);
  if (!pts)
    return <div className="empty-hint glass"><span>📉</span>Not enough readings to chart yet.</div>;
  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="history-chart" preserveAspectRatio="none" role="img" aria-label="Speed over time">
      <polyline points={pts} fill="none" stroke={color} strokeWidth={2.5} strokeLinejoin="round" strokeLinecap="round" />
      <polygon points={`0,${h} ${pts} ${w},${h}`} fill={color} opacity={0.12} />
      {readings.map((r, i) => (
        <circle key={r.id} cx={readings.length > 1 ? (i * w) / (readings.length - 1) : 0} cy={h - 14 - ((r.avgSpeedKmh - Math.min(...readings.map((x) => x.avgSpeedKmh))) / (Math.max(...readings.map((x) => x.avgSpeedKmh)) - Math.min(...readings.map((x) => x.avgSpeedKmh)) || 1)) * (h - 28)} r={2.6} fill={color} />
      ))}
    </svg>
  );
}

export function History() {
  const { token } = useAuth();
  const [segments, setSegments] = useState<RoadSegment[]>([]);
  const [selected, setSelected] = useState<number>(7);
  const [readings, setReadings] = useState<TrafficReading[] | null>(null);
  const [error, setError] = useState('');

  const [vehicleCount, setVehicleCount] = useState(140);
  const [avgSpeed, setAvgSpeed] = useState(36);
  const [congestion, setCongestion] = useState<CongestionLevel>('MEDIUM');
  const [ingestMsg, setIngestMsg] = useState('');

  useEffect(() => {
    if (!token) return;
    api.segments(token).then(setSegments).catch((err) => setError(err.message));
  }, [token]);

  useEffect(() => {
    if (!token) return;
    setReadings(null);
    api.historyForSegment(token, selected).then(setReadings).catch((err) => setError(err.message));
  }, [token, selected]);

  async function onIngest(e: FormEvent) {
    e.preventDefault();
    if (!token) return;
    setError('');
    setIngestMsg('');
    try {
      await api.ingest(token, {
        roadSegmentId: selected,
        vehicleCount,
        avgSpeedKmh: avgSpeed,
        congestionLevel: congestion,
        source: 'MANUAL' as TrafficSource,
      });
      setIngestMsg(`Ingested a new reading — segment ${selected}`);
      setReadings(await api.historyForSegment(token, selected));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ingest failed');
    }
  }

  const chartColor = readings?.length ? CONGESTION_COLOR[readings[readings.length - 1].congestionLevel] : '#7c5cff';

  return (
    <section className="view">
      <div className="view-head">
        <div>
          <h2>Segment history</h2>
          <p className="muted">Speed trends & past congestion for a single road.</p>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      <div className="card glass">
        <div className="pick-row">
          <label className="field">
            <span>Road</span>
            <div className="select-wrap">
              <span className="select-emoji">🛣️</span>
              <select value={selected} onChange={(e) => setSelected(Number(e.target.value))}>
                {segments.map((s) => (
                  <option key={s.id} value={s.id}>{s.name} · {s.city}</option>
                ))}
              </select>
            </div>
          </label>
          <div className="kpi-compact">
            <div><small className="muted">Readings</small><b>{readings?.length ?? '…'}</b></div>
            <div><small className="muted">Latest avg speed</small><b>{readings?.at(-1)?.avgSpeedKmh ?? '…'} km/h</b></div>
            <div><small className="muted">Latest level</small>
              {readings?.at(-1) ? (
                <span className="badge" style={{ background: CONGESTION_COLOR[readings.at(-1)!.congestionLevel] }}>
                  {readings.at(-1)!.congestionLevel}
                </span>
              ) : '…'}
            </div>
          </div>
        </div>

        <div className="chart-card">
          <HistoryChart readings={readings ?? []} color={chartColor} />
        </div>

        <div className="history-table-wrap">
          <table className="history-table">
            <thead>
              <tr><th>Time</th><th>Congestion</th><th>Speed</th><th>Vehicles</th><th>Source</th></tr>
            </thead>
            <tbody>
              {readings === null && (
                <tr><td colSpan={5} className="muted">Loading…</td></tr>
              )}
              {readings?.length === 0 && (
                <tr><td colSpan={5} className="muted">No readings recorded for this segment yet.</td></tr>
              )}
              {(readings ?? []).slice().reverse().map((r) => (
                <tr key={r.id}>
                  <td>{formatInstant(r.recordedAt)}</td>
                  <td>
                    <span className="badge" style={{ background: CONGESTION_COLOR[r.congestionLevel] }}>{r.congestionLevel}</span>
                  </td>
                  <td>{r.avgSpeedKmh} km/h</td>
                  <td>{r.vehicleCount}</td>
                  <td>{r.source}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card glass">
        <h3>Manually ingest a reading</h3>
        <form className="ingest-form" onSubmit={onIngest}>
          <label className="field">
            <span>Vehicle count</span>
            <input type="number" min={0} value={vehicleCount} onChange={(e) => setVehicleCount(Number(e.target.value))} />
          </label>
          <label className="field">
            <span>Avg speed (km/h)</span>
            <input type="number" min={0} step={0.5} value={avgSpeed} onChange={(e) => setAvgSpeed(Number(e.target.value))} />
          </label>
          <label className="field">
            <span>Congestion</span>
            <select value={congestion} onChange={(e) => setCongestion(e.target.value as CongestionLevel)}>
              <option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>SEVERE</option>
            </select>
          </label>
          <button type="submit" className="btn-primary">Ingest</button>
        </form>
        {ingestMsg && <div className="ok">{ingestMsg}</div>}
      </div>
    </section>
  );
}