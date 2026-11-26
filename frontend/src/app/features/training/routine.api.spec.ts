import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { RoutineApi } from './routine.api';

const ROUTINE = {
  id: 'routine-1',
  name: 'PPL',
  notes: null,
  exercises: [],
};

describe('RoutineApi', () => {
  let api: RoutineApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(RoutineApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('lists and loads routines', () => {
    api.list(1, 10).subscribe();
    let request = http.expectOne(`${environment.apiBaseUrl}/routines?page=1&size=10`);
    expect(request.request.method).toBe('GET');
    request.flush({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 });

    api.get('routine-1').subscribe();
    request = http.expectOne(`${environment.apiBaseUrl}/routines/routine-1`);
    request.flush(ROUTINE);
  });

  it('creates, updates, duplicates and deletes routines', () => {
    const payload = { name: 'PPL', notes: null, exercises: [] };
    api.create(payload).subscribe();
    let request = http.expectOne(`${environment.apiBaseUrl}/routines`);
    expect(request.request.method).toBe('POST');
    request.flush(ROUTINE);

    api.update('routine-1', payload).subscribe();
    request = http.expectOne(`${environment.apiBaseUrl}/routines/routine-1`);
    expect(request.request.method).toBe('PUT');
    request.flush(ROUTINE);

    api.duplicate('routine-1').subscribe();
    request = http.expectOne(`${environment.apiBaseUrl}/routines/routine-1/duplicate`);
    expect(request.request.method).toBe('POST');
    request.flush(ROUTINE);

    api.delete('routine-1').subscribe();
    request = http.expectOne(`${environment.apiBaseUrl}/routines/routine-1`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });

  it('starts a session from a routine', () => {
    api.start('routine-1').subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/routines/routine-1/start`);
    expect(request.request.method).toBe('POST');
    request.flush({ id: 'session-1' });
  });
});
