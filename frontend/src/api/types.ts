// Mirrors the backend DTOs (see each service's dto/ package).

export type CongestionLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'SEVERE';
export type TrafficSource = 'SENSOR' | 'CAMERA' | 'GPS' | 'MANUAL' | 'API';

export interface AuthResponse {
  token: string;
  tokenType: string;
  email: string;
  role: string;
  expiresInMs: number;
}

export interface RoadSegment {
  id: number;
  name: string;
  city: string;
  startLat: number;
  startLng: number;
  endLat: number;
  endLng: number;
  createdAt: string;
}

export interface TrafficReading {
  id: number;
  roadSegmentId: number;
  roadSegmentName: string;
  vehicleCount: number;
  avgSpeedKmh: number;
  congestionLevel: CongestionLevel;
  source: TrafficSource;
  recordedAt: string;
}

export interface Prediction {
  roadSegmentId: number;
  roadSegmentName: string;
  forTimestamp: string;
  predictedCongestionLevel: CongestionLevel;
  confidence: number;
}

export interface RouteSegmentInfo {
  roadSegmentId: number;
  roadSegmentName: string;
  distanceKm: number;
  effectiveSpeedKmh: number;
  travelTimeMinutes: number;
}

export interface RouteResult {
  totalDistanceKm: number;
  totalTravelTimeMinutes: number;
  segments: RouteSegmentInfo[];
}

export interface RouteRequest {
  originLat: number;
  originLng: number;
  destinationLat: number;
  destinationLng: number;
}

export interface TrafficDataRequest {
  roadSegmentId: number;
  vehicleCount: number;
  avgSpeedKmh: number;
  congestionLevel: CongestionLevel;
  source: TrafficSource;
}

// UI-facing summary combining a segment with its latest live reading + forecast.
export interface SegmentStatus extends RoadSegment {
  latest: TrafficReading | null;
  prediction: Prediction | null;
}

export const CONGESTION_COLOR: Record<CongestionLevel, string> = {
  LOW: '#16a34a',
  MEDIUM: '#d97706',
  HIGH: '#ea580c',
  SEVERE: '#dc2626',
};