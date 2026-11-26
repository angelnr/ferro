import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FinishSessionResponse,
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

  get(id: string): Observable<WorkoutSession> {
    return this.http.get<WorkoutSession>(`${this.base}/${id}`);
  }

  addExercise(id: string, exerciseId: string): Observable<WorkoutSession> {
    return this.http.post<WorkoutSession>(`${this.base}/${id}/exercises`, { exerciseId });
  }

  logSet(id: string, entryId: string, payload: LogSetPayload): Observable<SessionSet> {
    return this.http.post<SessionSet>(`${this.base}/${id}/exercises/${entryId}/sets`, payload);
  }

  finish(id: string): Observable<FinishSessionResponse> {
    return this.http.post<FinishSessionResponse>(`${this.base}/${id}/finish`, {});
  }

  discard(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
