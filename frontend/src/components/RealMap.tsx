import { useEffect, useMemo, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { CongestionLevel, RoadSegment, SegmentStatusBrief } from '../api/types';
import { CONGESTION_COLOR } from '../api/types';
import { LANDMARKS, LANDMARK_ICON } from '../config/landmarks';
import { distanceKm } from '../lib/geo';

const CONGESTION_OPACITY: Record<CongestionLevel, number> = {
  LOW: 0.6,
  MEDIUM: 0.75,
  HIGH: 0.88,
  SEVERE: 1,
};

const LEVEL_FLOW_SECONDS: Record<CongestionLevel, number> = {
  LOW: 1.0,
  MEDIUM: 1.5,
  HIGH: 2.1,
  SEVERE: 2.7,
};

const LEVEL_GLOW: Record<CongestionLevel, string> = {
  LOW: 'rgba(22,163,74,.35)',
  MEDIUM: 'rgba(217,119,6,.5)',
  HIGH: 'rgba(234,88,12,.7)',
  SEVERE: 'rgba(220,38,38,.95)',
};

const CASING = '#0b1120';
const DEFAULTS_LOCATION: [number, number] = [30.345, 78.02];

interface Props {
  segments: RoadSegment[];
  colorBy?: (segmentId: number) => CongestionLevel | null;
  brief?: (segmentId: number) => SegmentStatusBrief | null;
  routeTrace?: Array<[number, number]>;
  origin?: [number, number];
  destination?: [number, number];
  city?: string | 'ALL';
  routeSpeedKmh?: number | null;
  height?: number;
}

function landmarkDivIcon(emoji: string, highlight: boolean, name: string) {
  return L.divIcon({
    html: `<div class="lm-pin"><div class="lm-badge${highlight ? ' edge' : ''}" style="--lm-bc:${highlight ? '#7c5cff' : '#2f3b5c'}">${emoji}</div><div class="lm-label">${name}</div></div>`,
    className: '',
    iconSize: [46, 60],
    iconAnchor: [23, 30],
  });
}

function pinIcon(letter: string, color: string) {
  return L.divIcon({
    html: `<div class="pin-badge" style="--pin-c:${color}"><div class="pin-ring"></div><div class="pin-head">${letter}</div><div class="pin-tail"></div></div>`,
    className: '',
    iconSize: [30, 42],
    iconAnchor: [15, 40],
  });
}

const nodeKey = (lat: number, lng: number) => `${lat.toFixed(6)},${lng.toFixed(6)}`;

function clamp(v: number, lo: number, hi: number) {
  return Math.min(hi, Math.max(lo, v));
}

// Resample a polyline into N evenly-spaced (by distance) points for smooth vehicle motion.
function resampleTrace(pts: Array<[number, number]>, n = 360): Array<[number, number]> {
  if (pts.length < 2) return pts;
  const cum = [0];
  for (let i = 1; i < pts.length; i++) {
    cum.push(cum[i - 1] + distanceKm(pts[i - 1][0], pts[i - 1][1], pts[i][0], pts[i][1]) * 1000);
  }
  const total = cum[cum.length - 1];
  if (total <= 0) return pts;
  const out: Array<[number, number]> = [];
  let s = 0;
  for (let j = 0; j < n; j++) {
    const target = (j / n) * total;
    while (s < pts.length - 2 && cum[s + 1] < target) s++;
    const lo = cum[s];
    const hi = cum[s + 1];
    const f = hi > lo ? (target - lo) / (hi - lo) : 0;
    out.push([pts[s][0] + (pts[s + 1][0] - pts[s][0]) * f, pts[s][1] + (pts[s + 1][1] - pts[s][1]) * f]);
  }
  return out;
}

function pointAt(trace: Array<[number, number]>, f: number): [number, number] {
  if (trace.length === 0) return DEFAULTS_LOCATION;
  if (trace.length < 2) return trace[0];
  const x = f * (trace.length - 1);
  const i = Math.floor(x);
  const t = x - i;
  if (i >= trace.length - 1) return trace[trace.length - 1];
  return [
    trace[i][0] + (trace[i + 1][0] - trace[i][0]) * t,
    trace[i][1] + (trace[i + 1][1] - trace[i][1]) * t,
  ];
}

export function RealMap({
  segments,
  colorBy,
  brief,
  routeTrace,
  origin,
  destination,
  city,
  routeSpeedKmh,
  height = 480,
}: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const layersRef = useRef<L.LayerGroup | null>(null);

  const visibleLandmarks = useMemo(() => {
    const list = LANDMARKS.filter((l) => l.city === 'Dehradun' || l.city === 'Saharanpur');
    if (city && city !== 'ALL') return list.filter((l) => l.city === city);
    return list;
  }, [city]);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;
    const container = containerRef.current;

    const map = L.map(container, {
      center: DEFAULTS_LOCATION,
      zoom: 12,
      zoomControl: false,
      scrollWheelZoom: false,
      attributionControl: true,
      maxBounds: [[30.05, 77.6], [30.65, 78.3]],
      maxBoundsViscosity: 0.85,
    });

    L.control.zoom({ position: 'bottomright' }).addTo(map);
    L.control.scale({ position: 'bottomright', imperial: false, metric: true }).addTo(map);

    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution:
        '&copy; <a href="https://openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
      subdomains: 'abcd',
      maxZoom: 20,
      minZoom: 10,
      detectRetina: true,
    }).addTo(map);

    const syncLabels = () => container.classList.toggle('labels-on', map.getZoom() >= 13);
    map.on('zoomend', syncLabels);
    syncLabels();

    layersRef.current = L.layerGroup().addTo(map);
    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
      layersRef.current = null;
    };
  }, []);

  useEffect(() => {
    const layers = layersRef.current;
    const map = mapRef.current;
    if (!layers || !map) return;

    layers.clearLayers();
    const hasRoute = !!routeTrace && routeTrace.length > 1;

    const isEdge = (lat: number, lng: number) =>
      (!!origin && Math.abs(origin[0] - lat) < 0.00001 && Math.abs(origin[1] - lng) < 0.00001) ||
      (!!destination && Math.abs(destination[0] - lat) < 0.00001 && Math.abs(destination[1] - lng) < 0.00001);

    // ── Road segments: casing + colored + animated flow ──────
    const junctions = new Map<string, [number, number]>();
    segments.forEach((s) => {
      junctions.set(nodeKey(s.startLat, s.startLng), [s.startLat, s.startLng]);
      junctions.set(nodeKey(s.endLat, s.endLng), [s.endLat, s.endLng]);

      const level = colorBy?.(s.id) ?? null;
      const color = level ? CONGESTION_COLOR[level] : '#4b5a7a';
      const opacity = level ? CONGESTION_OPACITY[level] : 0.45;
      const b = brief?.(s.id);
      const coords: Array<[number, number]> = [[s.startLat, s.startLng], [s.endLat, s.endLng]];

      L.polyline(coords, { color: CASING, weight: 7, opacity: 0.95, lineCap: 'round' as const }).addTo(layers);

      const line = L.polyline(coords, { color, weight: 3.5, opacity, lineCap: 'round' as const });
      line.addTo(layers);
      const el = line.getElement() as HTMLElement | null;
      if (el && level) el.style.filter = `drop-shadow(0 0 5px ${LEVEL_GLOW[level]})`;

      if (level) {
        const flow = L.polyline(coords, {
          color: 'rgba(255,255,255,.55)',
          weight: 2,
          lineCap: 'round' as const,
          className: 'seg-flow',
          dashArray: '4 12',
        });
        flow.addTo(layers);
        const fel = flow.getElement() as HTMLElement | null;
        if (fel) {
          fel.style.animationDuration = `${LEVEL_FLOW_SECONDS[level]}s`;
          fel.style.animationDelay = `-${(s.id % 6) * 0.25}s`;
        }
      }

      const tooltip = level
        ? `<b>${s.name}</b><br/>${level} · ${b?.speed != null ? `${b.speed} km/h` : 'no data'}`
        : `<b>${s.name}</b>`;
      line.bindTooltip(tooltip, {
        sticky: true,
        className: 'map-tooltip-dark',
        direction: 'top',
        offset: [0, -8],
      });
    });

    // Junction nodes (deduped per shared endpoint)
    junctions.forEach(([lat, lng]) => {
      L.circleMarker([lat, lng], {
        radius: 2.8,
        weight: 1,
        color: CASING,
        fillColor: '#c7d2ef',
        fillOpacity: 0.9,
      }).addTo(layers);
    });

    // ── Landmark markers with zoom-gated labels ──────────────
    visibleLandmarks.forEach((l) => {
      const edge = isEdge(l.lat, l.lng);
      L.marker([l.lat, l.lng], {
        icon: landmarkDivIcon(LANDMARK_ICON[l.type], edge, l.name),
        interactive: true,
        riseOnHover: true,
      })
        .bindTooltip(`<b>${l.name}</b><br/>${l.city}${l.note ? ` · ${l.note}` : ''}`, {
          sticky: true,
          className: 'map-tooltip-dark',
          direction: 'top',
          offset: [0, -14],
        })
        .addTo(layers);
    });

    // ── Route: casing + glowing line + animated flow ─────────
    if (hasRoute) {
      const pts = routeTrace as Array<[number, number]>;
      L.polyline(pts, { color: CASING, weight: 11, opacity: 0.9, lineCap: 'round' as const, lineJoin: 'round' as const }).addTo(layers);

      const routeAvg = routeSpeedKmh ?? null;
      const dashDur = routeAvg == null ? 1.5 : clamp(3 - (routeAvg / 60) * 2, 1, 3);

      const routeLine = L.polyline(pts, {
        color: '#7c5cff',
        weight: 6,
        opacity: 0.92,
        lineCap: 'round' as const,
        lineJoin: 'round' as const,
        className: 'route-glow',
      });
      routeLine.addTo(layers);

      if (routeAvg != null) {
        routeLine.bindTooltip(`<b>Fastest route</b><br/>avg ${routeAvg.toFixed(1)} km/h`, {
          sticky: true,
          className: 'map-tooltip-dark',
          direction: 'top',
          offset: [0, -10],
        });
      }

      const routeFlow = L.polyline(pts, {
        color: 'rgba(255,255,255,.75)',
        weight: 2.5,
        lineCap: 'round' as const,
        lineJoin: 'round' as const,
        className: 'route-flow',
        dashArray: '12 10',
      });
      routeFlow.addTo(layers);
      const rel = routeFlow.getElement() as HTMLElement | null;
      if (rel) rel.style.animationDuration = `${dashDur}s`;

      // ── Animated vehicle dots travelling the route ──────────
      const reduceMotion = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      const loopSec = routeAvg == null ? 26 : clamp(60 - (routeAvg / 60) * 46, 14, 60);
      const trace = resampleTrace(pts, 400);
      const vehicles = [0, 1, 2, 3].map((i) => {
        const f = i / 4;
        const ll = pointAt(trace, f);
        const core = L.circleMarker(ll, {
          radius: 4.5,
          weight: 1.5,
          color: '#ffffff',
          fillColor: '#8b6dff',
          fillOpacity: 0.95,
          className: 'vehicle-dot',
        });
        const trail = L.circleMarker(ll, {
          radius: 9,
          weight: 0,
          fillColor: '#7c5cff',
          fillOpacity: 0.22,
          className: 'vehicle-trail',
        });
        return { core, trail, f };
      });
      vehicles.forEach((v) => {
        v.trail.addTo(layers);
        v.core.addTo(layers);
      });

      let raf = 0;
      if (!reduceMotion) {
        let t = 0;
        let last = performance.now();
        const step = (now: number) => {
          raf = requestAnimationFrame(step);
          const dt = Math.min(0.1, (now - last) / 1000);
          last = now;
          t = (t + dt / loopSec) % 1;
          vehicles.forEach((v) => {
            const ll = pointAt(trace, (v.f + t) % 1);
            v.core.setLatLng(ll);
            v.trail.setLatLng(ll);
          });
        };
        raf = requestAnimationFrame(step);
      }

      return () => cancelAnimationFrame(raf);
    }

    // ── Origin / destination pins ────────────────────────────
    if (origin) {
      L.marker(origin, { icon: pinIcon('A', '#22c55e'), zIndexOffset: 1000 }).addTo(layers);
    }
    if (destination) {
      L.marker(destination, { icon: pinIcon('B', '#4facfe'), zIndexOffset: 1000 }).addTo(layers);
    }

    return undefined;
  }, [segments, colorBy, brief, routeTrace, origin, destination, visibleLandmarks, routeSpeedKmh]);

  // Fit + initial view (kept separate so fit changes don't re-trigger layer rebuilds).
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    const hasRoute = !!routeTrace && routeTrace.length > 1;
    const fitPoints: L.LatLngExpression[] = [];
    if (hasRoute) {
      (routeTrace as Array<[number, number]>).forEach((p) => fitPoints.push(p));
    } else if (city === 'ALL') {
      segments.forEach((s) => fitPoints.push([s.startLat, s.startLng], [s.endLat, s.endLng]));
      visibleLandmarks.forEach((l) => fitPoints.push([l.lat, l.lng]));
    } else if (city) {
      segments.filter((s) => s.city === city).forEach((s) => fitPoints.push([s.startLat, s.startLng], [s.endLat, s.endLng]));
      visibleLandmarks.forEach((l) => fitPoints.push([l.lat, l.lng]));
    } else {
      segments.filter((s) => s.city === 'Dehradun').forEach((s) => fitPoints.push([s.startLat, s.startLng], [s.endLat, s.endLng]));
      visibleLandmarks.filter((l) => l.city === 'Dehradun').forEach((l) => fitPoints.push([l.lat, l.lng]));
    }
    if (fitPoints.length >= 2) {
      map.fitBounds(L.latLngBounds(fitPoints), { padding: [30, 30], maxZoom: 14 });
    } else {
      map.setView(DEFAULTS_LOCATION, 12);
    }
  }, [segments, city, visibleLandmarks, routeTrace]);

  return (
    <div ref={containerRef} className="real-map-container" style={{ height, width: '100%', borderRadius: 14, overflow: 'hidden', position: 'relative' }}>
      <div className="map-vignette" />
    </div>
  );
}