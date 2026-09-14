import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { CongestionLevel, RoadSegment } from '../api/types';
import { CONGESTION_COLOR } from '../api/types';
import { LANDMARKS, LANDMARK_ICON } from '../config/landmarks';
import type { SegmentStatusBrief } from './NetworkMap';

const CONGESTION_OPACITY: Record<CongestionLevel, number> = {
  LOW: 0.55,
  MEDIUM: 0.7,
  HIGH: 0.85,
  SEVERE: 1,
};

interface Props {
  segments: RoadSegment[];
  colorBy?: (segmentId: number) => CongestionLevel | null;
  brief?: (segmentId: number) => SegmentStatusBrief | null;
  routeTrace?: Array<[number, number]>;
  origin?: [number, number];
  destination?: [number, number];
  height?: number;
}

function landmarkDivIcon(emoji: string, highlight: boolean) {
  return L.divIcon({
    html: `<div style="font-size:${highlight ? 22 : 17}px;line-height:1;text-shadow:0 1px 4px rgba(0,0,0,.7)">${emoji}</div>`,
    className: '',
    iconSize: [28, 28],
    iconAnchor: [14, 14],
  });
}

function pinIcon(fill: string) {
  return L.divIcon({
    html: `<div style="width:18px;height:18px;border-radius:50%;background:${fill};border:3px solid #fff;box-shadow:0 0 8px ${fill}"></div>`,
    className: '',
    iconSize: [18, 18],
    iconAnchor: [9, 9],
  });
}

export function RealMap({ segments, colorBy, brief, routeTrace, origin, destination, height = 480 }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const layersRef = useRef<L.LayerGroup | null>(null);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    const map = L.map(containerRef.current, {
      center: [30.345, 78.02],
      zoom: 12,
      zoomControl: false,
      scrollWheelZoom: false,
      attributionControl: true,
      maxBounds: [[30.05, 77.6], [30.65, 78.3]],
      maxBoundsViscosity: 0.85,
    });

    L.control.zoom({ position: 'bottomright' }).addTo(map);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 18,
      subdomains: 'abc',
    }).addTo(map);

    layersRef.current = L.layerGroup().addTo(map);
    mapRef.current = map;

    return () => { map.remove(); mapRef.current = null; };
  }, []);

  // Redraw layers whenever data changes
  useEffect(() => {
    const layers = layersRef.current;
    const map = mapRef.current;
    if (!layers || !map) return;

    layers.clearLayers();
    const hasRoute = routeTrace && routeTrace.length > 1;
    const fitPoints: L.LatLngExpression[] = [];

    // ── Road segments ───────────────────────────────────────
    segments.forEach((s) => {
      const level = colorBy?.(s.id) ?? null;
      const color = level ? CONGESTION_COLOR[level] : '#4b5a7a';
      const opacity = level ? CONGESTION_OPACITY[level] : 0.4;
      const b = brief?.(s.id);

      const line = L.polyline(
        [[s.startLat, s.startLng], [s.endLat, s.endLng]],
        { color, weight: 3.5, opacity, lineCap: 'round' as const },
      );

      const tooltip = level
        ? `<b>${s.name}</b><br/>${level} · ${b?.speed != null ? `${b.speed} km/h` : 'no data'}`
        : `<b>${s.name}</b>`;

      line.bindTooltip(tooltip, {
        sticky: true,
        className: 'map-tooltip-dark',
        direction: 'top',
        offset: [0, -8],
      });

      line.addTo(layers);
      // Default view centres on Dehradun; a computed route takes precedence.
      if (!hasRoute && s.city === 'Dehradun') {
        fitPoints.push([s.startLat, s.startLng], [s.endLat, s.endLng]);
      }
    });

    // ── Landmark markers ────────────────────────────────────
    const isEdge = (lat: number, lng: number) =>
      (!!origin && Math.abs(origin[0] - lat) < 0.00001 && Math.abs(origin[1] - lng) < 0.00001) ||
      (!!destination && Math.abs(destination[0] - lat) < 0.00001 && Math.abs(destination[1] - lng) < 0.00001);

    LANDMARKS.filter((l) => l.city === 'Dehradun' || l.city === 'Saharanpur').forEach((l) => {
      const edge = isEdge(l.lat, l.lng);
      L.marker([l.lat, l.lng], {
        icon: landmarkDivIcon(LANDMARK_ICON[l.type], edge),
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

      if ((hasRoute && edge) || (l.city === 'Dehradun' && !hasRoute)) {
        fitPoints.push([l.lat, l.lng]);
      }
    });

    // ── Route polyline (drawn last so it sits on top) ───────
    if (hasRoute) {
      L.polyline(routeTrace as Array<[number, number]>, {
        color: '#7c5cff',
        weight: 7,
        opacity: 0.92,
        lineCap: 'round' as const,
        lineJoin: 'round' as const,
        className: 'route-glow',
      })
        .addTo(layers);

      routeTrace.forEach((p) => fitPoints.push(p));
    }

    // ── Origin / destination pins ───────────────────────────
    if (origin) {
      L.marker(origin, { icon: pinIcon('#7c5cff'), zIndexOffset: 1000 }).addTo(layers);
      fitPoints.push(origin);
    }
    if (destination) {
      L.marker(destination, { icon: pinIcon('#4facfe'), zIndexOffset: 1000 }).addTo(layers);
      fitPoints.push(destination);
    }

    // ── Fit view ────────────────────────────────────────────
    if (fitPoints.length >= 2) {
      map.fitBounds(L.latLngBounds(fitPoints), { padding: [30, 30], maxZoom: 15 });
    } else {
      map.setView([30.345, 78.02], 12);
    }
  }, [segments, colorBy, brief, routeTrace, origin, destination]);

  return (
    <div
      ref={containerRef}
      className="real-map-container"
      style={{ height, width: '100%', borderRadius: 14, overflow: 'hidden' }}
    />
  );
}
