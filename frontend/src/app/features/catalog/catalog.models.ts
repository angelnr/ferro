export interface Exercise {
  id: string;
  name: string;
  nameEn: string;
  muscleGroup: string;
  secondaryMuscles: string[];
  equipment: string;
  mechanics: string;
  bodyweight: boolean;
  mine: boolean;
}

export interface ExercisePage {
  content: Exercise[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ExerciseStats {
  personalRecords: unknown[];
  bestE1rm: number | null;
  totalVolume: number;
  sessionCount: number;
  e1rmSeries: unknown[];
}

export interface ExerciseDetail {
  exercise: Exercise;
  stats: ExerciseStats;
}

export interface CreateExercisePayload {
  nameEs?: string;
  nameEn?: string;
  muscleGroup: string;
  secondaryMuscles?: string[];
  equipment: string;
  mechanics: string;
  bodyweight?: boolean;
}
