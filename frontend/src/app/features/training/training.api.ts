import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FinishSessionResponse,
  PreviousSession,
  LogSetPayload,
  SessionSet,
  WorkoutSession,
} from './training.models';

@Injectable({ providedIn: 'root' })
export class TrainingApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/workout-sessions`;

  start(): Observable<WorkoutSession> {
    return this.http.post<WorkoutSession>(this.base, {});
  }

  active(): Observable<WorkoutSession | null> {
    return this.http.get<WorkoutSession | null>(`${this.base}/active`);
  }

  get(id: string): Observable<WorkoutSession> {
    return this.http.get<WorkoutSession>(`${this.base}/${id}`);
  }

  addExercise(id: string, exerciseId: string): Observable<WorkoutSession> {
    return this.http.post<WorkoutSession>(`${this.base}/${id}/exercises`, { exerciseId });
  }

  logSet(id: string, entryId: string, payload: LogSetPayload): Observable<SessionSet> {
    return this.http.post<SessionSet>(`${this.base}/${id}/exercises/${entryId}/sets`, payload);
  }

  updateExercise(
    id: string,
    entryId: string,
    payload: {
      position?: number;
      restSeconds?: number | null;
      supersetGroup?: number | null;
      notes?: string | null;
    },
  ): Observable<WorkoutSession> {
    return this.http.patch<WorkoutSession>(`${this.base}/${id}/exercises/${entryId}`, payload);
  }

  updateSession(
    id: string,
    payload: { name?: string; notes?: string | null },
  ): Observable<WorkoutSession> {
    return this.http.patch<WorkoutSession>(`${this.base}/${id}`, payload);
  }

  updateSet(
    id: string,
    entryId: string,
    setId: string,
    payload: {
      reps: number;
      weight: number;
      rir?: number | null;
      setType: string;
      notes?: string | null;
    },
  ): Observable<WorkoutSession> {
    return this.http.patch<WorkoutSession>(
      `${this.base}/${id}/exercises/${entryId}/sets/${setId}`,
      payload,
    );
  }

  deleteSet(id: string, entryId: string, setId: string): Observable<WorkoutSession> {
    return this.http.delete<WorkoutSession>(
      `${this.base}/${id}/exercises/${entryId}/sets/${setId}`,
    );
  }

  deleteExercise(id: string, entryId: string): Observable<WorkoutSession> {
    return this.http.delete<WorkoutSession>(`${this.base}/${id}/exercises/${entryId}`);
  }

  groupSuperset(id: string, entryIds: string[], group: number | null): Observable<WorkoutSession> {
    return this.http.patch<WorkoutSession>(`${this.base}/${id}/supersets`, { entryIds, group });
  }

  previous(exerciseId: string, limit = 3): Observable<PreviousSession[]> {
    return this.http.get<PreviousSession[]>(
      `${environment.apiBaseUrl}/exercises/${exerciseId}/history`,
      {
        params: { limit },
      },
    );
  }

  finish(id: string): Observable<FinishSessionResponse> {
    return this.http.post<FinishSessionResponse>(`${this.base}/${id}/finish`, {});
  }

  discard(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
