export interface SessionSet {
  id: string;
  position: number;
  setType: string;
  reps: number;
  weight: number;
  rir: number | null;
  volume: number;
  e1rm: number | null;
  loggedAt: string;
}

export interface SessionExercise {
  id: string;
  exerciseId: string;
  name: string;
  position: number;
  restSeconds: number | null;
  supersetGroup: number | null;
  notes: string | null;
  sets: SessionSet[];
}

export interface WorkoutSession {
  id: string;
  name: string;
  status: string;
  startedAt: string;
  finishedAt: string | null;
  durationSeconds: number | null;
  totalVolume: number;
  exercises: SessionExercise[];
}

export interface NewPersonalRecord {
  exerciseId: string;
  exerciseName: string;
  prType: string;
  value: number;
  previousValue: number | null;
  achievedAt: string;
}

export interface FinishSessionResponse {
  session: WorkoutSession;
  newPersonalRecords: NewPersonalRecord[];
}

export interface LogSetPayload {
  reps: number;
  weight: number;
  rir?: number | null;
  setType: string;
  notes?: string;
}
