export interface HistorySession {
  id: string;
  name: string;
  startedAt: string;
  finishedAt: string;
  durationSeconds: number;
  totalVolume: number;
  exerciseCount: number;
  setCount: number;
  hasPr: boolean;
  status: string;
}

export interface HistoryPage {
  content: HistorySession[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SessionSetDetail {
  id: string;
  position: number;
  setType: string;
  reps: number;
  weight: number;
  rir: number | null;
  volume: number;
  e1rm: number | null;
  notes: string | null;
  loggedAt: string;
}

export interface PrAchieved {
  type: string;
  value: number;
}

export interface SessionExerciseDetail {
  id: string;
  exerciseId: string;
  name: string;
  position: number;
  volume: number;
  notes: string | null;
  sets: SessionSetDetail[];
  personalRecords: PrAchieved[];
}

export interface SessionDetail {
  id: string;
  name: string;
  notes: string | null;
  startedAt: string;
  finishedAt: string;
  durationSeconds: number;
  totalVolume: number;
  status: string;
  exercises: SessionExerciseDetail[];
}

export interface RecordEntry {
  type: string;
  value: number;
  achievedAt: string;
}

export interface PersonalRecordGroup {
  exerciseId: string;
  exerciseName: string;
  current: RecordEntry[];
  history: RecordEntry[];
}

export interface WeekPoint {
  weekStart: string;
  value: number;
}

export interface PeriodTotals {
  volume: number;
  sessions: number;
  sets: number;
}

export interface MuscleSlice {
  muscleGroup: string;
  volume: number;
}

export interface ProgressSummary {
  weeks: number;
  weeklyVolume: WeekPoint[];
  weeklySessions: WeekPoint[];
  currentStreak: number;
  bestStreak: number;
  current: PeriodTotals;
  previous: PeriodTotals;
  muscleDistribution: MuscleSlice[];
  periodStart: string;
  periodEnd: string;
}

export interface VolumePoint {
  date: string;
  volume: number;
}
