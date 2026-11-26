import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BodyMeasurement, BodyPage, BodyPayload, BodyStats } from './body.models';

@Injectable({ providedIn: 'root' })
export class BodyApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/body-measurements`;

  list(page = 0, size = 20, from?: string, to?: string): Observable<BodyPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }
    return this.http.get<BodyPage>(this.base, { params });
  }

  create(payload: BodyPayload): Observable<BodyMeasurement> {
    return this.http.post<BodyMeasurement>(this.base, payload);
  }

  update(id: string, payload: BodyPayload): Observable<BodyMeasurement> {
    return this.http.patch<BodyMeasurement>(`${this.base}/${id}`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  stats(months = 3): Observable<BodyStats> {
    return this.http.get<BodyStats>(`${this.base}/stats`, { params: { months } });
  }
}
