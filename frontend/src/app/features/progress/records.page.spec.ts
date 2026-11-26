import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { Exercise } from '../catalog/catalog.models';
import { RecordsPage } from './records.page';

const GROUP = {
  exerciseId: 'exercise-1',
  exerciseName: 'Press',
  current: [{ type: 'MAX_WEIGHT', value: 100, achievedAt: '2026-11-20T10:00:00Z' }],
  history: [{ type: 'MAX_WEIGHT', value: 95, achievedAt: '2026-10-20T10:00:00Z' }],
};

const EXERCISE: Exercise = {
  id: 'exercise-1',
  name: 'Press',
  nameEn: 'Press',
  muscleGroup: 'CHEST',
  secondaryMuscles: [],
  equipment: 'BARBELL',
  mechanics: 'COMPOUND',
  bodyweight: false,
  mine: false,
};

describe('RecordsPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RecordsPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        ...provideTranslateService({ lang: 'es' }),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('loads, filters and clears records', async () => {
    const fixture = TestBed.createComponent(RecordsPage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/progress/prs`).flush([GROUP]);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Press');
    const page = fixture.componentInstance as unknown as {
      filter(exercise: Exercise): void;
      clear(): void;
    };
    page.filter(EXERCISE);
    http.expectOne(`${environment.apiBaseUrl}/progress/prs?exerciseId=exercise-1`).flush([GROUP]);
    await fixture.whenStable();
    page.clear();
    http.expectOne(`${environment.apiBaseUrl}/progress/prs`).flush([GROUP]);
    await fixture.whenStable();
  });

  it('shows an error when records fail', async () => {
    const fixture = TestBed.createComponent(RecordsPage);
    fixture.detectChanges();
    http
      .expectOne(`${environment.apiBaseUrl}/progress/prs`)
      .flush({}, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as { error: () => boolean };
    expect(page.error()).toBe(true);
  });
});
