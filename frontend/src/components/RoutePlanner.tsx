import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../auth';
import { api } from '../api/client';
import type { CongestionLevel, RoadSegment, RouteResult, TrafficReading } from '../api/types';
import { CONGESTION_COLOR } from '../api/types';
import { LANDMARKS, LANDMARK_ICON } from '../config/landmarks';
import { RealMap } from './RealMap';
import type { SegmentStatusBrief } from './NetworkMap';
import { traceRoute, formatEta, formatKm } from '../lib/geo';
import { useToast } from './Toast';

const REFRESH_MS = 30000;

export function RoutePlanner() {
  const { token } = useAuth();
  const { push } = useToast();
  const [segments, setSegments] = useState<RoadSegment[]>([]);
  const [live, setLive] = useState<Map<number, { level: CongestionLevel; speed: number }>>(new Map());
  const [originId, setOriginId] = useState('geu');
  const [destId, setDestId] = useState('upes');
  const [route, setRoute] = useState<RouteResult | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    const load = async () => {
      try {
        const segs = await api.segments(token);
        if (cancelled) return;
        setSegments(segs);
        const latest = new Map<number, { level: CongestionLevel; speed: number }>();
        await Promise.all(
          segs.map(async (s) => {
            const r: TrafficReading | null = await api.latestForSegment(token, s.id).catch(() => null);
            if (r && !cancelled) latest.set(s.id, { level: r.congestionLevel, speed: r.avgSpeedKmh });
          }),
        );
        if (!cancelled) setLive(latest);
      } catch {
        // dashboard-level errors handled there; keep map usable with segments only
      }
    };
    load();
    const t = setInterval(load, REFRESH_MS);
    return () => { cancelled = true; clearInterval(t); };
  }, [token]);

  const origin = useMemo(() => LANDMARKS.find((l) => l.id === originId), [originId]);
  const destination = useMemo(() => LANDMARKS.find((l) => l.id === destId), [destId]);

  const trace = useMemo(
    () => (route && origin && destination ? traceRoute(segments, route, origin.lat, origin.lng) : []),
    [route, segments, origin, destination],
  );

  const colorBy = useCallback(
    (id: number) => live.get(id)?.level ?? null,
    [live],
  );

  const brief = useCallback(
    (id: number): SegmentStatusBrief | null => {
      const s = live.get(id);
      return s ? { level: s.level, speed: s.speed } : null;
    },
    [live],
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

      <div className="card glass map-card">
        <div className="map-card-head">
          <div>
            <h3>Dehradun road network</h3>
            <span className="muted">Live OpenStreetMap · colored by real-time congestion</span>
          </div>
          <div className="legend">
            {(['LOW', 'MEDIUM', 'HIGH', 'SEVERE'] as CongestionLevel[]).map((l) => (
              <span key={l} className="legend-item">
                <i style={{ background: CONGESTION_COLOR[l], boxShadow: `0 0 6px ${CONGESTION_COLOR[l]}` }} />
                {l}
              </span>
            ))}
          </div>
        </div>
        <RealMap
          segments={segments}
          colorBy={colorBy}
          brief={brief}
          routeTrace={route ? trace : []}
          origin={origin ? [origin.lat, origin.lng] : undefined}
          destination={destination ? [destination.lat, destination.lng] : undefined}
        />
      </div>

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