import { useMemo } from 'react';
import type { CongestionLevel, RoadSegment } from '../api/types';
import { CONGESTION_COLOR } from '../api/types';

interface Props {
  segments: RoadSegment[];
  highlights?: Set<number>;
  colorBy?: (segmentId: number) => CongestionLevel | null;
  width?: number;
  height?: number;
}

// Plot the segment network on an SVG canvas using an equirectangular
// projection of the lat/lng bounding box (fine for a small city network).
export function NetworkMap({ segments, highlights, colorBy, width = 720, height = 420 }: Props) {
  const { x, y } = useMemo(() => {
    const lats = segments.flatMap((s) => [s.startLat, s.endLat]);
    const lngs = segments.flatMap((s) => [s.startLng, s.endLng]);
    if (lats.length === 0) return { x: () => 0, y: () => 0 };

    const minLat = Math.min(...lats);
    const maxLat = Math.max(...lats);
    const minLng = Math.min(...lngs);
    const maxLng = Math.max(...lngs);
    const padLng = (maxLng - minLng || 0.01) * 0.15;
    const padLat = (maxLat - minLat || 0.01) * 0.2;

    const lngRange = maxLng - minLng + padLng * 2;
    const latRange = maxLat - minLat + padLat * 2;

    const x = (lng: number) => ((lng - (minLng - padLng)) / lngRange) * width;
    const y = (lat: number) => height - ((lat - (minLat - padLat)) / latRange) * height;

    return { x, y };
  }, [segments, width, height]);

  const nodes = useMemo(() => {
    const seen = new Map<string, RoadSegment>();
    for (const s of segments) {
      seen.set(`${s.startLat},${s.startLng}`, s);
      seen.set(`${s.endLat},${s.endLng}`, s);
    }
    return [...seen.entries()].map(([key, s]) => {
      const [lat, lng] = key.split(',').map(Number);
      return { key, cx: x(lng), cy: y(lat), s };
    });
  }, [segments, x, y]);

  return (
    <svg className="network-map" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Road network map">
      {segments.map((s) => {
        const level = colorBy?.(s.id) ?? null;
        const inRoute = highlights?.has(s.id) ?? false;
        return (
          <line
            key={s.id}
            x1={x(s.startLng)}
            y1={y(s.startLat)}
            x2={x(s.endLng)}
            y2={y(s.endLat)}
            stroke={level ? CONGESTION_COLOR[level] : '#94a3b8'}
            strokeWidth={inRoute ? 7 : 3}
            strokeLinecap="round"
            opacity={inRoute ? 0.95 : 0.75}
          />
        );
      })}
      {nodes.map(({ key, cx, cy }) => (
        <circle key={key} cx={cx} cy={cy} r={4} fill="#1e293b" stroke="#fff" strokeWidth={1.5} />
      ))}
    </svg>
  );
}