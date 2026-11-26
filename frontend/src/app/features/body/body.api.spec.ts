import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { BodyApi } from './body.api';

describe('BodyApi', () => {
  let api: BodyApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(BodyApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('lists and loads stats', () => {
    api.list(1, 20, 'a', 'b').subscribe();
    let request = http.expectOne(
      `${environment.apiBaseUrl}/body-measurements?page=1&size=20&from=a&to=b`,
    );
    request.flush({ content: [] });

    api.list().subscribe();
    request = http.expectOne(`${environment.apiBaseUrl}/body-measurements?page=0&size=20`);
    request.flush({ content: [] });

    api.stats(6).subscribe();
    request = http.expectOne(`${environment.apiBaseUrl}/body-measurements/stats?months=6`);
    request.flush({ weight: [] });
  });

  it('creates, updates and deletes measurements', () => {
    const payload = {
      measuredAt: '2026-11-20T10:00:00Z',
      weight: 80,
      chest: null,
      waist: null,
      hip: null,
      arm: null,
      thigh: null,
      notes: null,
    };
    api.create(payload).subscribe();
    let request = http.expectOne(`${environment.apiBaseUrl}/body-measurements`);
    expect(request.request.method).toBe('POST');
    request.flush({ id: 'body-1' });

    api.update('body-1', payload).subscribe();
    request = http.expectOne(`${environment.apiBaseUrl}/body-measurements/body-1`);
    expect(request.request.method).toBe('PATCH');
    request.flush({ id: 'body-1' });

    api.delete('body-1').subscribe();
    request = http.expectOne(`${environment.apiBaseUrl}/body-measurements/body-1`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
