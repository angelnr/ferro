import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { Exercise } from '../catalog/catalog.models';
import { HistoryPage } from './history.page';

const SESSION = {
  id: 'session-1',
  name: 'PPL',
  startedAt: '2026-11-20T10:00:00Z',
  finishedAt: '2026-11-20T11:00:00Z',
  durationSeconds: 3660,
  totalVolume: 1000,
  exerciseCount: 2,
  setCount: 8,
  hasPr: true,
  status: 'FINISHED',
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

describe('HistoryPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HistoryPage],
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

  it('loads, filters and paginates history', async () => {
    const fixture = TestBed.createComponent(HistoryPage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/progress/sessions?page=0&size=20`).flush({
      content: [SESSION],
      page: 0,
      size: 20,
      totalElements: 21,
      totalPages: 2,
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('PPL');
    const page = fixture.componentInstance as unknown as {
      value(event: Event): string;
      filterExercise(exercise: Exercise): void;
      clearExercise(): void;
      go(delta: number): Promise<void>;
      duration(seconds: number): string;
    };
    expect(page.value({ target: { value: '2026-11-01' } } as unknown as Event)).toBe('2026-11-01');
    expect(page.duration(3660)).toContain('1 h');
    expect(page.duration(120)).toBe('2 min');

    page.filterExercise(EXERCISE);
    http
      .expectOne(`${environment.apiBaseUrl}/progress/sessions?page=0&size=20&exerciseId=exercise-1`)
      .flush({ content: [SESSION], page: 0, size: 20, totalElements: 21, totalPages: 2 });
    await fixture.whenStable();

    page.clearExercise();
    http
      .expectOne(`${environment.apiBaseUrl}/progress/sessions?page=0&size=20`)
      .flush({ content: [SESSION], page: 0, size: 20, totalElements: 21, totalPages: 2 });
    await fixture.whenStable();

    const paging = page.go(1);
    http
      .expectOne(`${environment.apiBaseUrl}/progress/sessions?page=1&size=20`)
      .flush({ content: [], page: 1, size: 20, totalElements: 21, totalPages: 2 });
    await paging;
  });

  it('shows an error when history fails', async () => {
    const fixture = TestBed.createComponent(HistoryPage);
    fixture.detectChanges();
    http
      .expectOne(`${environment.apiBaseUrl}/progress/sessions?page=0&size=20`)
      .flush({}, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as { error: () => boolean };
    expect(page.error()).toBe(true);
  });
});
