export interface BodyMeasurement {
  id: string;
  measuredAt: string;
  weight: number | null;
  chest: number | null;
  waist: number | null;
  hip: number | null;
  arm: number | null;
  thigh: number | null;
  notes: string | null;
}

export interface BodyPage {
  content: BodyMeasurement[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface BodyPoint {
  date: string;
  value: number;
}

export interface BodyStats {
  weight: BodyPoint[];
  chest: BodyPoint[];
  waist: BodyPoint[];
  hip: BodyPoint[];
  arm: BodyPoint[];
  thigh: BodyPoint[];
}

export interface BodyPayload {
  measuredAt: string | null;
  weight: number | null;
  chest: number | null;
  waist: number | null;
  hip: number | null;
  arm: number | null;
  thigh: number | null;
  notes: string | null;
}
