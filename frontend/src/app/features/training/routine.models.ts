export interface RoutineSet {
  id: string;
  position: number;
  setType: string;
  targetReps: number;
  targetWeight: number | null;
  targetRir: number | null;
}

export interface RoutineExercise {
  id: string;
  exerciseId: string;
  name: string;
  position: number;
  restSeconds: number | null;
  supersetGroup: number | null;
  notes: string | null;
  sets: RoutineSet[];
}

export interface Routine {
  id: string;
  name: string;
  notes: string | null;
  exercises: RoutineExercise[];
}

export interface RoutineSummary {
  id: string;
  name: string;
  exerciseCount: number;
  lastUsedAt: string | null;
}

export interface RoutinePage {
  content: RoutineSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface RoutineSetPayload {
  setType: string;
  targetReps: number;
  targetWeight: number | null;
  targetRir: number | null;
}

export interface RoutineExercisePayload {
  exerciseId: string;
  restSeconds: number | null;
  supersetGroup: number | null;
  notes: string | null;
  sets: RoutineSetPayload[];
}

export interface RoutinePayload {
  name: string;
  notes: string | null;
  exercises: RoutineExercisePayload[];
}
