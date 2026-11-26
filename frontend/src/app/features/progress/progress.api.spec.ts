import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { ProgressApi } from './progress.api';

describe('ProgressApi', () => {
  let api: ProgressApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ProgressApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('loads history with filters and detail', () => {
    api.history({ from: 'a', to: 'b', exerciseId: 'e', page: 1, size: 20 }).subscribe();
    let request = http.expectOne(
      `${environment.apiBaseUrl}/progress/sessions?page=1&size=20&from=a&to=b&exerciseId=e`,
    );
    request.flush({ content: [], page: 1, size: 20, totalElements: 0, totalPages: 0 });

    api.session('session-1').subscribe();
    request = http.expectOne(`${environment.apiBaseUrl}/progress/sessions/session-1`);
    request.flush({ id: 'session-1' });
  });

  it('loads records, summary and volume', () => {
    api.personalRecords('exercise-1').subscribe();
    let request = http.expectOne(`${environment.apiBaseUrl}/progress/prs?exerciseId=exercise-1`);
    request.flush([]);

    api.personalRecords().subscribe();
    request = http.expectOne(`${environment.apiBaseUrl}/progress/prs`);
    request.flush([]);

    api.summary(12).subscribe();
    request = http.expectOne(`${environment.apiBaseUrl}/progress/summary?weeks=12`);
    request.flush({ weeks: 12 });

    api.volume('a', 'b').subscribe();
    request = http.expectOne(
      `${environment.apiBaseUrl}/progress/volume?granularity=week&from=a&to=b`,
    );
    request.flush([]);

    api.volume().subscribe();
    request = http.expectOne(`${environment.apiBaseUrl}/progress/volume?granularity=week`);
    request.flush([]);
  });

  it('deletes a finished session', () => {
    api.deleteSession('session-1').subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/workout-sessions/session-1`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
