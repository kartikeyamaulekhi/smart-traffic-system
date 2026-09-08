import { useMemo, useState } from 'react';
import type { CongestionLevel, RoadSegment } from '../api/types';
import { CONGESTION_COLOR } from '../api/types';
import { LANDMARK_ICON, LANDMARKS } from '../config/landmarks';

export interface SegmentStatusBrief {
  level: CongestionLevel | null;
  speed: number | null;
}

interface Props {
  segments: RoadSegment[];
  city: string | 'ALL';
  colorBy?: (segmentId: number) => CongestionLevel | null;
  brief?: (segmentId: number) => SegmentStatusBrief | null;
  routeTrace?: Array<[number, number]>;
  origin?: [number, number];
  destination?: [number, number];
  width?: number;
  height?: number;
}

interface Hover {
  name: string;
  level: CongestionLevel | null;
  x: number;
  y: number;
}

function Projection({ segments, width, height }: { segments: RoadSegment[]; width: number; height: number }) {
  return useMemo(() => {
    const lats = segments.flatMap((s) => [s.startLat, s.endLat]);
    const lngs = segments.flatMap((s) => [s.startLng, s.endLng]);
    if (lats.length === 0) return { x: () => 0, y: () => 0 };
    const minLat = Math.min(...lats);
    const maxLat = Math.max(...lats);
    const minLng = Math.min(...lngs);
    const maxLng = Math.max(...lngs);
    const pad = Math.max((maxLng - minLng) || 0.01, (maxLat - minLat) || 0.01) * 0.22;
    const x = (lng: number) => ((lng - (minLng - pad)) / (maxLng - minLng + pad * 2)) * width;
    const y = (lat: number) => height - ((lat - (minLat - pad)) / (maxLat - minLat + pad * 2)) * height;
    return { x, y };
  }, [segments, width, height]);
}

export function NetworkMap({ segments, city, colorBy, brief, routeTrace, origin, destination, width = 880, height = 460 }: Props) {
  const [hover, setHover] = useState<Hover | null>(null);
  const proj = Projection({ segments, width, height });
  const { x, y } = proj;

  const visibleLandmarks = useMemo(
    () => (city === 'ALL' ? LANDMARKS.filter((l) => l.city === 'Dehradun') : LANDMARKS.filter((l) => l.city === city || l.city === 'Dehradun')),
    [city],
  );

  const routePoints = routeTrace ?? [];

  return (
    <div className="map-wrap" style={{ position: 'relative' }}>
      <svg
        className="network-map"
        viewBox={`0 0 ${width} ${height}`}
        role="img"
        aria-label="Road network map"
        onMouseLeave={() => setHover(null)}
      >
        <defs>
          <linearGradient id="routeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#7c5cff" />
            <stop offset="100%" stopColor="#4facfe" />
          </linearGradient>
        </defs>

        {/* faint grid */}
        {Array.from({ length: 8 }, (_, i) => (
          <line key={`v${i}`} x1={(width / 8) * i} y1={0} x2={(width / 8) * i} y2={height} stroke="rgba(255,255,255,0.03)" />
        ))}
        {Array.from({ length: 6 }, (_, i) => (
          <line key={`h${i}`} x1={0} y1={(height / 6) * i} x2={width} y2={(height / 6) * i} stroke="rgba(255,255,255,0.03)" />
        ))}

        {/* road segments with animated traffic flow */}
        {segments.map((s) => {
          const level = colorBy?.(s.id) ?? null;
          const color = level ? CONGESTION_COLOR[level] : '#4b5a7a';
          const b = brief?.(s.id);
          const mx = (x(s.startLng) + x(s.endLng)) / 2;
          const my = (y(s.startLat) + y(s.endLat)) / 2;
          return (
            <g key={s.id} className="road-group" onMouseEnter={() => setHover({ name: s.name, level, x: mx, y: my })}>
              <line x1={x(s.startLng)} y1={y(s.startLat)} x2={x(s.endLng)} y2={y(s.endLat)} stroke={color} strokeWidth={3.2} strokeLinecap="round" opacity={0.85} style={{ filter: `drop-shadow(0 0 4px ${color})` }} />
              <line className="flow-dash" x1={x(s.startLng)} y1={y(s.startLat)} x2={x(s.endLng)} y2={y(s.endLat)} stroke="rgba(255,255,255,0.85)" strokeWidth={1.4} strokeLinecap="round" strokeDasharray={4 + (s.id % 3) * 3} />
              {b?.speed != null && (
                <title>{`${s.name}\n${level ?? 'no data'} · ${b.speed} km/h`}</title>
              )}
            </g>
          );
        })}

        {/* route polyline */}
        {routePoints.length > 1 && (
          <polyline
            points={routePoints.map(([la, ln]) => `${x(ln)},${y(la)}`).join(' ')}
            fill="none"
            stroke="url(#routeGrad)"
            strokeWidth={6}
            strokeLinejoin="round"
            strokeLinecap="round"
            style={{ filter: 'drop-shadow(0 0 8px rgba(124,92,255,0.9))' }}
          />
        )}

        {/* landmark pins */}
        {visibleLandmarks.map((l) => {
          const [cx, cy] = [x(l.lng), y(l.lat)];
          const isEdge = [origin, destination].some((p) => p && Math.abs(p[0] - l.lat) < 0.00001 && Math.abs(p[1] - l.lng) < 0.00001);
          return (
            <g key={l.id} className="landmark" transform={`translate(${cx},${cy})`} onMouseEnter={() => setHover({ name: `${l.name} (${l.city})`, level: null, x: cx, y: cy + (cy > height * 0.5 ? 26 : -26) })}>
              <circle r={isEdge ? 9 : 7} fill="rgba(255,255,255,0.9)" stroke={isEdge ? '#7c5cff' : '#2a3552'} strokeWidth={2} />
              <text y={2} textAnchor="middle" fontSize={isEdge ? 11 : 8.5}>
                {LANDMARK_ICON[l.type]}
              </text>
            </g>
          );
        })}

        {/* node endpoints */}
        {segments.map((s) => (
          <g key={`n${s.id}`}>
            <circle cx={x(s.startLng)} cy={y(s.startLat)} r={2.6} fill="#dbe3ff" className="node-pulse" />
            <circle cx={x(s.endLng)} cy={y(s.endLat)} r={2.6} fill="#dbe3ff" className="node-pulse" />
          </g>
        ))}
      </svg>

      {hover && (
        <div
          className="map-tooltip"
          style={{
            left: hover.x,
            top: hover.y,
            transform: 'translate(-50%, -110%)',
          }}
        >
          <strong>{hover.name}</strong>
          {hover.level && <span style={{ color: CONGESTION_COLOR[hover.level] }}>{hover.level}</span>}
        </div>
      )}
    </div>
  );
}