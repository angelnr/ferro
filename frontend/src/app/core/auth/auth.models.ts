export interface User {
  id: string;
  displayName: string;
  email: string;
  status: 'UNVERIFIED' | 'ACTIVE' | 'LOCKED';
  locale: 'es' | 'en';
  unitSystem: 'KG' | 'LB';
  theme: 'DARK' | 'LIGHT';
  timeZone: string;
  defaultRestSeconds: number;
}

export interface Preferences {
  locale: 'es' | 'en';
  unitSystem: 'KG' | 'LB';
  theme: 'DARK' | 'LIGHT';
  timeZone: string;
  defaultRestSeconds: number;
  timerSound: boolean;
}

export interface AuthResponse {
  user: User;
  accessToken: string;
}

export interface MeResponse {
  user: User;
  preferences: Preferences;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  errors?: Record<string, string[]>;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterPayload {
  displayName: string;
  email: string;
  password: string;
  healthConsent: boolean;
  locale?: 'es' | 'en';
  unitSystem?: 'KG' | 'LB';
}

export interface UpdateProfilePayload {
  displayName?: string;
  locale?: 'es' | 'en';
  unitSystem?: 'KG' | 'LB';
  theme?: 'DARK' | 'LIGHT';
  defaultRestSeconds?: number;
}
