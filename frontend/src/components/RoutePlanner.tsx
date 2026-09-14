import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../auth';
import { api } from '../api/client';
import type { RoadSegment, RouteResult } from '../api/types';
import { LANDMARKS, LANDMARK_ICON } from '../config/landmarks';
import { NetworkMap } from './NetworkMap';
import { traceRoute, formatEta, formatKm } from '../lib/geo';
import { useToast } from './Toast';

export function RoutePlanner() {
  const { token } = useAuth();
  const { push } = useToast();
  const [segments, setSegments] = useState<RoadSegment[]>([]);
  const [originId, setOriginId] = useState('geu');
  const [destId, setDestId] = useState('upes');
  const [route, setRoute] = useState<RouteResult | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!token) return;
    api.segments(token).then(setSegments).catch(() => undefined);
  }, [token]);

  const origin = useMemo(() => LANDMARKS.find((l) => l.id === originId), [originId]);
  const destination = useMemo(() => LANDMARKS.find((l) => l.id === destId), [destId]);

  const trace = useMemo(
    () => (route && origin && destination ? traceRoute(segments, route, origin.lat, origin.lng) : []),
    [route, segments, origin, destination],
  );

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!origin || !destination || !token) return;
    setError('');
    setBusy(true);
    try {
      setRoute(
        await api.route(token, {
          originLat: origin.lat,
          originLng: origin.lng,
          destinationLat: destination.lat,
          destinationLng: destination.lng,
        }),
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Routing failed');
      push(err instanceof Error ? err.message : 'Routing failed', 'error');
      setRoute(null);
    } finally {
      setBusy(false);
    }
  }

  const swap = () => {
    setOriginId(destId);
    setDestId(originId);
  };

  const byName = (a: { name: string }, b: { name: string }) => a.name.localeCompare(b.name);

  return (
    <section className="view">
      <div className="view-head">
        <div>
          <h2>Route planner</h2>
          <p className="muted">Fastest path by live travel time — snapped to the modelled road network.</p>
        </div>
      </div>

      <div className="card glass planner-card">
        <form className="route-form" onSubmit={onSubmit}>
          <label className="field">
            <span>From</span>
            <div className="select-wrap">
              <span className="select-emoji">{LANDMARK_ICON[origin?.type ?? 'hub']}</span>
              <select value={originId} onChange={(e) => setOriginId(e.target.value)}>
                <optgroup label="🎓 Universities">
                  {LANDMARKS.filter((l) => l.type === 'university').sort(byName).map((l) => (
                    <option key={l.id} value={l.id}>{l.name}</option>
                  ))}
                </optgroup>
                <optgroup label="🚏 Transit & hubs">
                  {LANDMARKS.filter((l) => l.type !== 'university').sort(byName).map((l) => (
                    <option key={l.id} value={l.id}>{l.name}</option>
                  ))}
                </optgroup>
              </select>
            </div>
            <small className="muted">{origin?.city} · {origin?.note ?? origin?.type}</small>
          </label>

          <button type="button" className="swap-btn" onClick={swap} title="Swap origin & destination">⇄</button>

          <label className="field">
            <span>To</span>
            <div className="select-wrap">
              <span className="select-emoji">{LANDMARK_ICON[destination?.type ?? 'university']}</span>
              <select value={destId} onChange={(e) => setDestId(e.target.value)}>
                <optgroup label="🎓 Universities">
                  {LANDMARKS.filter((l) => l.type === 'university').sort(byName).map((l) => (
                    <option key={l.id} value={l.id}>{l.name}</option>
                  ))}
                </optgroup>
                <optgroup label="🚏 Transit & hubs">
                  {LANDMARKS.filter((l) => l.type !== 'university').sort(byName).map((l) => (
                    <option key={l.id} value={l.id}>{l.name}</option>
                  ))}
                </optgroup>
              </select>
            </div>
            <small className="muted">{destination?.city} · {destination?.note ?? destination?.type}</small>
          </label>

          <button type="submit" className="btn-primary" disabled={busy || !origin || !destination || origin.id === destId}>
            {busy ? 'Routing…' : 'Find fastest route'}
          </button>
        </form>
      </div>

      {error && <div className="error">{error}</div>}

      {route && origin && destination ? (
        <>
          <div className="route-summary-grid">
            <div className="kpi glass">
              <span className="kpi-label">Distance</span>
              <strong>{formatKm(route.totalDistanceKm)}</strong>
              <span className="kpi-sub">{route.segments.length} road segments</span>
            </div>
            <div className="kpi glass">
              <span className="kpi-label">Estimated travel time</span>
              <strong className="grad-text">{formatEta(route.totalTravelTimeMinutes)}</strong>
              <span className="kpi-sub">at current congestion</span>
            </div>
            <div className="kpi glass route-od">
              <span className="kpi-label">{origin.name} → {destination.name}</span>
              <div className="route-arrows">
                <span className="route-pin origin" />
                <span className="route-line" />
                <span className="route-pin dest" />
              </div>
              <span className="kpi-sub">{destination.city}</span>
            </div>
          </div>

          <div className="card glass">
            <NetworkMap
              segments={segments}
              city="ALL"
              routeTrace={trace}
              origin={[origin.lat, origin.lng]}
              destination={[destination.lat, destination.lng]}
            />
          </div>

          <div className="card glass">
            <h3>Turn-by-segment</h3>
            <ul className="route-legs">
              {route.segments.map((s, i) => (
                <li key={`${s.roadSegmentId}-${i}`}>
                  <span className="leg-idx">{i + 1}</span>
                  <span className="leg-main">
                    <strong>{s.roadSegmentName}</strong>
                    <small className="muted">@ {s.effectiveSpeedKmh} km/h effective speed</small>
                  </span>
                  <span className="leg-meta">
                    <b>{formatKm(s.distanceKm)}</b>
                    <small>{formatEta(s.travelTimeMinutes)}</small>
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </>
      ) : (
        <div className="empty-hint glass">
          <span>🧭</span>
          Pick two places — try <b>DIT University → UPES</b> across the Mussoorie Rd corridor.
        </div>
      )}
    </section>
  );
}