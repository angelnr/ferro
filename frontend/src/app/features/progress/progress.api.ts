import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  HistoryPage,
  PersonalRecordGroup,
  ProgressSummary,
  SessionDetail,
  VolumePoint,
} from './progress.models';

@Injectable({ providedIn: 'root' })
export class ProgressApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/progress`;

  history(params: {
    from?: string;
    to?: string;
    exerciseId?: string;
    page?: number;
    size?: number;
  }): Observable<HistoryPage> {
    let httpParams = new HttpParams().set('page', params.page ?? 0).set('size', params.size ?? 20);
    if (params.from) {
      httpParams = httpParams.set('from', params.from);
    }
    if (params.to) {
      httpParams = httpParams.set('to', params.to);
    }
    if (params.exerciseId) {
      httpParams = httpParams.set('exerciseId', params.exerciseId);
    }
    return this.http.get<HistoryPage>(`${this.base}/sessions`, { params: httpParams });
  }

  session(id: string): Observable<SessionDetail> {
    return this.http.get<SessionDetail>(`${this.base}/sessions/${id}`);
  }

  personalRecords(exerciseId?: string): Observable<PersonalRecordGroup[]> {
    let params = new HttpParams();
    if (exerciseId) {
      params = params.set('exerciseId', exerciseId);
    }
    return this.http.get<PersonalRecordGroup[]>(`${this.base}/prs`, { params });
  }

  summary(weeks: number): Observable<ProgressSummary> {
    return this.http.get<ProgressSummary>(`${this.base}/summary`, { params: { weeks } });
  }

  volume(from?: string, to?: string, granularity = 'week'): Observable<VolumePoint[]> {
    let params = new HttpParams().set('granularity', granularity);
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }
    return this.http.get<VolumePoint[]>(`${this.base}/volume`, { params });
  }

  deleteSession(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/workout-sessions/${id}`);
  }
}
