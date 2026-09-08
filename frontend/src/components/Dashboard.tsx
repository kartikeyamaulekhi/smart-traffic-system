import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '../auth';
import { api } from '../api/client';
import type { CongestionLevel, Prediction, RoadSegment, TrafficReading } from '../api/types';
import { CONGESTION_COLOR } from '../api/types';
import { NetworkMap, type SegmentStatusBrief } from './NetworkMap';
import { Sparkline } from './Sparkline';
import { SkeletonBars, SkeletonRow } from './Skeleton';
import { formatInstant } from '../lib/geo';

const REFRESH_MS = 30000;

const LEVEL_WEIGHT: Record<CongestionLevel, number> = { LOW: 0, MEDIUM: 1, HIGH: 2, SEVERE: 3 };

interface SegmentCard extends RoadSegment {
  latest: TrafficReading | null;
  prediction: Prediction | null;
  history: number[];
}

function hourLocal(offsetHours: number): string {
  const d = new Date(Date.now() + offsetHours * 3600_000);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:00:00`;
}

function hourLabel(offset: number): string {
  if (offset === 0) return 'Now';
  const d = new Date(Date.now() + offset * 3600_000);
  return d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
}

function CityBar({ cities, city, onChange }: { cities: string[]; city: string; onChange: (c: string) => void }) {
  return (
    <div className="city-bar">
      {['ALL', ...cities].map((c) => (
        <button key={c} className={city === c ? 'chip active' : 'chip'} onClick={() => onChange(c)}>
          {c === 'ALL' ? '🌍 All cities' : c === 'Dehradun' ? '🏔️ Dehradun' : c === 'Saharanpur' ? '🌾 Saharanpur' : c}
        </button>
      ))}
    </div>
  );
}

export function Dashboard() {
  const { token } = useAuth();
  const [cards, setCards] = useState<SegmentCard[] | null>(null);
  const [city, setCity] = useState<string>('ALL');
  const [forecastOffset, setForecastOffset] = useState(2);
  const [error, setError] = useState('');
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);

  const load = useCallback(async () => {
    if (!token) return;
    try {
      const segments = await api.segments(token);
      const ts = hourLocal(forecastOffset);
      const enriched = await Promise.all(
        segments.map(async (s) => {
          const [latest, prediction, history] = await Promise.all([
            api.latestForSegment(token, s.id).catch(() => null),
            api.predict(token, s.id, ts).catch(() => null),
            api.historyForSegment(token, s.id).catch(() => []),
          ]);
          return { ...s, latest, prediction, history: history.map((h) => h.avgSpeedKmh).slice(-12) };
        }),
      );
      setCards(enriched);
      setLastUpdate(new Date());
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load dashboard');
    }
  }, [token, forecastOffset]);

  useEffect(() => {
    load();
    const t = setInterval(load, REFRESH_MS);
    return () => clearInterval(t);
  }, [load]);

  const cities = useMemo(() => [...new Set(cards?.map((c) => c.city) ?? [])], [cards]);
  const filtered = useMemo(() => (city === 'ALL' ? cards ?? [] : (cards ?? []).filter((c) => c.city === city)), [cards, city]);

  const kpis = useMemo(() => {
    const withData = (cards ?? []).filter((c) => c.latest);
    const coverageKm = (cards ?? []).reduce((sum, c) => sum + Math.sqrt(distance2(c)), 0);
    const idx = withData.length
      ? (withData.reduce((s, c) => s + LEVEL_WEIGHT[c.latest!.congestionLevel], 0) / (withData.length * 3)) * 100
      : 0;
    const avgSpeed = withData.length
      ? withData.reduce((s, c) => s + (c.latest?.avgSpeedKmh ?? 0), 0) / withData.length
      : 0;
    const conf = (cards ?? []).filter((c) => c.prediction).length
      ? (cards ?? []).reduce((s, c) => s + (c.prediction?.confidence ?? 0), 0) / (cards ?? []).filter((c) => c.prediction).length
      : 0;
    return { coverageKm, idx, avgSpeed, conf };
  }, [cards]);

  const brief = useCallback(
    (id: number): SegmentStatusBrief | null => {
      const c = filtered.find((f) => f.id === id);
      if (!c?.latest) return null;
      return { level: c.latest.congestionLevel, speed: c.latest.avgSpeedKmh };
    },
    [filtered],
  );

  if (!cards) {
    return (
      <section>
        <SkeletonBars />
        <div className="card">
          <div className="map-skeleton" />
        </div>
        <div className="segment-list">
          <SkeletonRow />
          <SkeletonRow />
          <SkeletonRow />
        </div>
      </section>
    );
  }

  return (
    <section className="view">
      <div className="view-head">
        <div>
          <h2>Live congestion</h2>
          <p className="muted">Streaming from Kafka · refreshed {lastUpdate ? formatInstant(lastUpdate.toISOString()) : ''} · every {REFRESH_MS / 1000}s</p>
        </div>
        <CityBar cities={cities} city={city} onChange={setCity} />
      </div>

      {error && <div className="error">{error}</div>}

      <div className="kpi-grid">
        <div className="kpi glass">
          <span className="kpi-label">Network coverage</span>
          <strong>{kpis.coverageKm.toFixed(1)} km</strong>
          <span className="kpi-sub">{cards.length} monitored roads</span>
        </div>
        <div className="kpi glass">
          <span className="kpi-label">Congestion index</span>
          <strong style={{ color: kpis.idx > 55 ? '#ef4444' : kpis.idx > 30 ? '#f59e0b' : '#22c55e' }}>
            {kpis.idx.toFixed(0)} / 100
          </strong>
          <span className="kpi-sub">weighted across live roads</span>
        </div>
        <div className="kpi glass">
          <span className="kpi-label">Average speed</span>
          <strong>{kpis.avgSpeed.toFixed(0)} km/h</strong>
          <span className="kpi-sub">across segments with live data</span>
        </div>
        <div className="kpi glass">
          <span className="kpi-label">Forecast confidence</span>
          <strong>{(kpis.conf * 100).toFixed(0)}%</strong>
          <span className="kpi-sub">mean ML prediction score</span>
        </div>
      </div>

      <div className="card glass">
        <div className="map-title-row">
          <h3>{city === 'ALL' ? 'Road network' : `${city} road network`}</h3>
          <div className="legend">
            {(['LOW', 'MEDIUM', 'HIGH', 'SEVERE'] as CongestionLevel[]).map((l) => (
              <span key={l} className="legend-item">
                <i style={{ background: CONGESTION_COLOR[l], boxShadow: `0 0 6px ${CONGESTION_COLOR[l]}` }} />
                {l}
              </span>
            ))}
          </div>
        </div>
        <NetworkMap segments={filtered} city={city} colorBy={(id) => filtered.find((f) => f.id === id)?.latest?.congestionLevel ?? null} brief={brief} />
        <div className="forecast-slider-row">
          <span className="muted">🔮 Forecast for</span>
          <input type="range" min={0} max={12} value={forecastOffset} onChange={(e) => setForecastOffset(Number(e.target.value))} />
          <span className="forecast-hour">{hourLabel(forecastOffset)}</span>
        </div>
      </div>

      <div className="segment-list">
        {filtered.map((c) => {
          const lvl = c.latest?.congestionLevel ?? null;
          return (
            <div className="card glass seg-card" key={c.id}>
              <div className="seg-top">
                <div className="seg-name-box">
                  <span className="seg-road">🛣️</span>
                  <div>
                    <strong>{c.name}</strong>
                    <small className="muted">{c.city}</small>
                  </div>
                </div>
                {lvl ? (
                  <span className="badge glow" style={{ background: CONGESTION_COLOR[lvl], boxShadow: `0 0 12px ${CONGESTION_COLOR[lvl]}66` }}>
                    {lvl}
                  </span>
                ) : (
                  <span className="badge empty">NO DATA</span>
                )}
              </div>
              <div className="seg-stats">
                <span><small>Speed</small><b>{c.latest ? `${c.latest.avgSpeedKmh} km/h` : '—'}</b></span>
                <span><small>Vehicles</small><b>{c.latest?.vehicleCount ?? '—'}</b></span>
                <span><small>Source</small><b>{c.latest?.source ?? '—'}</b></span>
              </div>
              <div className="seg-footer">
                <Sparkline values={c.history} color={lvl ? CONGESTION_COLOR[lvl] : '#4b5a7a'} />
                <div className="forecast-chip">
                  <span className="pulse-dot" style={{ background: CONGESTION_COLOR[c.prediction?.predictedCongestionLevel ?? 'LOW'] }} />
                  <span>
                    {c.prediction ? `Forecast ${c.prediction.predictedCongestionLevel}` : 'Forecast n/a'}{' '}
                    {c.prediction && <small className="muted">({Math.round(c.prediction.confidence * 100)}%)</small>}
                  </span>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}

function distance2(c: SegmentCard): number {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(c.endLat - c.startLat);
  const dLng = toRad(c.endLng - c.startLng);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(c.startLat)) * Math.cos(toRad(c.endLat)) * Math.sin(dLng / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}