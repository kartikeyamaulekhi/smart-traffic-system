import type { RoadSegment, RouteResult } from '../api/types';

// Mirrors routing-service GeoUtils: endpoints rounding to 5 decimals must
// produce identical node keys for the graph to connect.
export function nodeKey(lat: number, lng: number): string {
  return `${lat.toFixed(5)},${lng.toFixed(5)}`;
}

export function distanceKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// Reconstruct the ordered lat/lng polyline of a route from the road segment
// list + the ordered edge list returned by the router.
export function traceRoute(
  segments: RoadSegment[],
  route: RouteResult,
  originLat: number,
  originLng: number,
): Array<[number, number]> {
  const byId = new Map(segments.map((s) => [s.id, s]));
  const points: Array<[number, number]> = [];
  if (route.segments.length === 0 || byId.size === 0) return points;

  const first = byId.get(route.segments[0].roadSegmentId);
  if (!first) return points;

  const firstNearEnd =
    distanceKm(originLat, originLng, first.endLat, first.endLng) <
    distanceKm(originLat, originLng, first.startLat, first.startLng);

  let current: [number, number] = firstNearEnd
    ? [first.endLat, first.endLng]
    : [first.startLat, first.startLng];
  points.push(current);

  for (const leg of route.segments) {
    const seg = byId.get(leg.roadSegmentId);
    if (!seg) continue;
    const next: [number, number] =
      distanceKm(current[0], current[1], seg.startLat, seg.startLng) <
      distanceKm(current[0], current[1], seg.endLat, seg.endLng)
        ? [seg.endLat, seg.endLng]
        : [seg.startLat, seg.startLng];
    points.push(next);
    current = next;
  }
  return points;
}

export function formatKm(km: number): string {
  return `${km.toFixed(km >= 10 ? 1 : 2)} km`;
}

export function formatEta(minutes: number): string {
  const m = Math.round(minutes);
  if (m < 60) return `${m} min`;
  return `${Math.floor(m / 60)}h ${m % 60}m`;
}

export function formatInstant(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: 'short',
  });
}