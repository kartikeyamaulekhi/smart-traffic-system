import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../auth';
import { api } from '../api/client';
import type { RoadSegment, RouteResult } from '../api/types';
import { NetworkMap } from './NetworkMap';

export function RoutePlanner() {
  const { token } = useAuth();
  const [segments, setSegments] = useState<RoadSegment[]>([]);
  const [originId, setOriginId] = useState<number>(1);
  const [destId, setDestId] = useState<number>(4);
  const [route, setRoute] = useState<RouteResult | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!token) return;
    api.segments(token).then(setSegments).catch((err) => setError(err.message));
  }, [token]);

  const points = useMemo(() => {
    const byId = new Map(segments.map((s) => [s.id, s]));
    const origin = byId.get(originId);
    const dest = byId.get(destId);
    if (!origin || !dest) return null;
    // Route from the midpoint of the chosen segments.
    return {
      originLat: (origin.startLat + origin.endLat) / 2,
      originLng: (origin.startLng + origin.endLng) / 2,
      destinationLat: (dest.startLat + dest.endLat) / 2,
      destinationLng: (dest.startLng + dest.endLng) / 2,
    };
  }, [segments, originId, destId]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!points || !token) return;
    setError('');
    setBusy(true);
    try {
      setRoute(await api.route(token, points));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Routing failed');
      setRoute(null);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section>
      <div className="section-head">
        <h2>Route planner</h2>
      </div>
      <div className="card">
        <form className="route-form" onSubmit={onSubmit}>
          <label>
            From
            <select value={originId} onChange={(e) => setOriginId(Number(e.target.value))}>
              {segments.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            To
            <select value={destId} onChange={(e) => setDestId(Number(e.target.value))}>
              {segments.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" className="primary" disabled={busy || !points}>
            {busy ? 'Routing…' : 'Find route'}
          </button>
        </form>
      </div>

      {error && <div className="error">{error}</div>}

      {route && (
        <>
          <div className="card route-summary">
            <div>
              <span className="mutted-label">Distance</span>
              <h3>{route.totalDistanceKm.toFixed(2)} km</h3>
            </div>
            <div>
              <span className="mutted-label">Travel time</span>
              <h3>{route.totalTravelTimeMinutes.toFixed(1)} min</h3>
            </div>
            <div>
              <span className="mutted-label">Roads</span>
              <h3>{route.segments.length}</h3>
            </div>
          </div>

          <div className="card">
            <NetworkMap
              segments={segments}
              highlights={new Set(route.segments.map((s) => s.roadSegmentId))}
            />
          </div>

          <div className="card">
            <ul className="route-legs">
              {route.segments.map((s) => (
                <li key={s.roadSegmentId}>
                  <strong>{s.roadSegmentName}</strong>
                  <span>
                    {s.distanceKm.toFixed(2)} km @ {s.effectiveSpeedKmh.toFixed(0)} km/h →{' '}
                    {s.travelTimeMinutes.toFixed(1)} min
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </>
      )}
    </section>
  );
}