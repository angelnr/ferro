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
  notes: string | null;
}

export interface TargetSet {
  id: string;
  position: number;
  setType: string;
  targetReps: number;
  targetWeight: number | null;
  targetRir: number | null;
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
  targetSets: TargetSet[];
}

export interface WorkoutSession {
  id: string;
  name: string;
  notes: string | null;
  status: string;
  startedAt: string;
  finishedAt: string | null;
  durationSeconds: number | null;
  totalVolume: number;
  exercises: SessionExercise[];
}

export interface PreviousSet {
  reps: number;
  weight: number;
  rir: number | null;
  setType: string;
  loggedAt: string;
}

export interface PreviousSession {
  sessionId: string;
  startedAt: string;
  sets: PreviousSet[];
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
  notes?: string | null;
  targetSetId?: string | null;
}
