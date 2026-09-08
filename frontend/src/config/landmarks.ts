// Curated places the route planner can snap to. Every coordinate is either an
// exact road-network endpoint (universities, transit hubs) or sits within the
// 2 km snapping radius of one, so the backend reliably routes between them.

export type LandmarkType = 'university' | 'transit' | 'hub' | 'road';

export interface Landmark {
  id: string;
  name: string;
  type: LandmarkType;
  city: string;
  lat: number;
  lng: number;
  note?: string;
}

export const LANDMARKS: Landmark[] = [
  // Dehradun — universities (arcadia/mussoorie corridors)
  { id: 'dit', name: 'DIT University', type: 'university', city: 'Dehradun', lat: 30.39934, lng: 78.07535, note: 'Mussoorie Diversion Rd' },
  { id: 'ims', name: 'IMS Unison University', type: 'university', city: 'Dehradun', lat: 30.39984, lng: 78.0778, note: 'Mussoorie Diversion Rd' },
  { id: 'geu', name: 'Graphic Era University', type: 'university', city: 'Dehradun', lat: 30.268, lng: 77.99601, note: 'Bell Rd, Clement Town' },
  { id: 'uttaranchal', name: 'Uttaranchal University', type: 'university', city: 'Dehradun', lat: 30.33994, lng: 77.95094, note: 'Arcadia Grant, Premnagar' },
  { id: 'upes', name: 'UPES — Bidholi', type: 'university', city: 'Dehradun', lat: 30.41717, lng: 77.96819, note: 'Energy Acres, NH-72' },
  // Dehradun — transit & civic hubs
  { id: 'isbt', name: 'ISBT Dehradun', type: 'transit', city: 'Dehradun', lat: 30.28782, lng: 77.99803 },
  { id: 'railway', name: 'Dehradun Railway Stn', type: 'transit', city: 'Dehradun', lat: 30.3134, lng: 78.0352 },
  { id: 'clocktower', name: 'Clock Tower (Rajpur Rd)', type: 'hub', city: 'Dehradun', lat: 30.3248, lng: 78.0413 },
  { id: 'mussoorie-city', name: 'Mussoorie Road City Jn', type: 'hub', city: 'Dehradun', lat: 30.3157, lng: 78.0334 },
  // Saharanpur — city roads
  { id: 'mg-road', name: 'MG Road', type: 'road', city: 'Saharanpur', lat: 29.968, lng: 77.546 },
  { id: 'civil-lines', name: 'Civil Lines Rd', type: 'road', city: 'Saharanpur', lat: 29.972, lng: 77.551 },
  { id: 'station-road', name: 'Station Road', type: 'road', city: 'Saharanpur', lat: 29.974, lng: 77.558 },
  { id: 'ring-road', name: 'Ring Road', type: 'road', city: 'Saharanpur', lat: 29.974, lng: 77.558 },
];

export const LANDMARK_ICON: Record<LandmarkType, string> = {
  university: '🎓',
  transit: '🚏',
  hub: '🏛️',
  road: '🛣️',
};