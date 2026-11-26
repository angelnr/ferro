import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Routine, RoutinePage, RoutinePayload } from './routine.models';
import { WorkoutSession } from './training.models';

@Injectable({ providedIn: 'root' })
export class RoutineApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/routines`;

  list(page = 0, size = 20): Observable<RoutinePage> {
    return this.http.get<RoutinePage>(this.base, { params: { page, size } });
  }

  get(id: string): Observable<Routine> {
    return this.http.get<Routine>(`${this.base}/${id}`);
  }

  create(payload: RoutinePayload): Observable<Routine> {
    return this.http.post<Routine>(this.base, payload);
  }

  update(id: string, payload: RoutinePayload): Observable<Routine> {
    return this.http.put<Routine>(`${this.base}/${id}`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  duplicate(id: string): Observable<Routine> {
    return this.http.post<Routine>(`${this.base}/${id}/duplicate`, {});
  }

  start(id: string): Observable<WorkoutSession> {
    return this.http.post<WorkoutSession>(`${this.base}/${id}/start`, {});
  }
}
