import { useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../auth';
import { api } from '../api/client';
import type { CongestionLevel, RoadSegment, TrafficReading, TrafficSource } from '../api/types';
import { CONGESTION_COLOR } from '../api/types';

export function History() {
  const { token } = useAuth();
  const [segments, setSegments] = useState<RoadSegment[]>([]);
  const [selected, setSelected] = useState<number>(1);
  const [readings, setReadings] = useState<TrafficReading[] | null>(null);
  const [error, setError] = useState('');

  const [vehicleCount, setVehicleCount] = useState(60);
  const [avgSpeed, setAvgSpeed] = useState(35);
  const [congestion, setCongestion] = useState<CongestionLevel>('MEDIUM');
  const [ingestMsg, setIngestMsg] = useState('');

  useEffect(() => {
    if (!token) return;
    api.segments(token).then(setSegments).catch((err) => setError(err.message));
  }, [token]);

  useEffect(() => {
    if (!token) return;
    setReadings(null);
    api
      .historyForSegment(token, selected)
      .then(setReadings)
      .catch((err) => setError(err.message));
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
      setIngestMsg(`Ingested a new reading for segment ${selected}`);
      const fresh = await api.historyForSegment(token, selected);
      setReadings(fresh);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ingest failed');
    }
  }

  return (
    <section>
      <div className="section-head">
        <h2>Segment history</h2>
      </div>
      {error && <div className="error">{error}</div>}

      <div className="card">
        <label>
          Segment
          <select value={selected} onChange={(e) => setSelected(Number(e.target.value))}>
            {segments.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <div className="history-table-wrap">
          <table className="history-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Congestion</th>
                <th>Speed</th>
                <th>Vehicles</th>
                <th>Source</th>
              </tr>
            </thead>
            <tbody>
              {readings === null && (
                <tr>
                  <td colSpan={5} className="muted">
                    Loading…
                  </td>
                </tr>
              )}
              {readings?.length === 0 && (
                <tr>
                  <td colSpan={5} className="muted">
                    No readings recorded for this segment yet.
                  </td>
                </tr>
              )}
              {(readings ?? []).map((r) => (
                <tr key={r.id}>
                  <td>{new Date(r.recordedAt).toLocaleString()}</td>
                  <td>
                    <span className="badge" style={{ background: CONGESTION_COLOR[r.congestionLevel] }}>
                      {r.congestionLevel}
                    </span>
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

      <div className="card">
        <h3>Manually ingest a reading</h3>
        <form className="ingest-form" onSubmit={onIngest}>
          <label>
            Vehicle count
            <input type="number" min={0} value={vehicleCount} onChange={(e) => setVehicleCount(Number(e.target.value))} />
          </label>
          <label>
            Avg speed (km/h)
            <input type="number" min={0} step={0.5} value={avgSpeed} onChange={(e) => setAvgSpeed(Number(e.target.value))} />
          </label>
          <label>
            Congestion
            <select value={congestion} onChange={(e) => setCongestion(e.target.value as CongestionLevel)}>
              <option>LOW</option>
              <option>MEDIUM</option>
              <option>HIGH</option>
              <option>SEVERE</option>
            </select>
          </label>
          <button type="submit" className="primary">
            Ingest
          </button>
        </form>
        {ingestMsg && <div className="ok">{ingestMsg}</div>}
      </div>
    </section>
  );
}