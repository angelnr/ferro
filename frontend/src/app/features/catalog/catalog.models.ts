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
  imageUrl?: string | null;
}

export interface ExercisePage {
  content: Exercise[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PersonalRecord {
  type: string;
  value: number;
  achievedAt: string;
}

export interface E1rmPoint {
  date: string;
  value: number;
}

export interface ExerciseStats {
  personalRecords: PersonalRecord[];
  bestE1rm: number | null;
  totalVolume: number;
  sessionCount: number;
  e1rmSeries: E1rmPoint[];
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
