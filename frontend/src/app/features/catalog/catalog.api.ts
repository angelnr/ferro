import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateExercisePayload, Exercise, ExerciseDetail, ExercisePage } from './catalog.models';

@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/exercises`;

  search(q: string, muscle: string, equipment: string): Observable<ExercisePage> {
    let params = new HttpParams().set('size', '30');
    if (q.trim()) {
      params = params.set('q', q.trim());
    }
    if (muscle) {
      params = params.set('muscle', muscle);
    }
    if (equipment) {
      params = params.set('equipment', equipment);
    }
    return this.http.get<ExercisePage>(this.base, { params });
  }

  create(payload: CreateExercisePayload): Observable<Exercise> {
    return this.http.post<Exercise>(this.base, payload);
  }

  detail(id: string): Observable<ExerciseDetail> {
    return this.http.get<ExerciseDetail>(`${this.base}/${id}`);
  }
}
