import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../../environments/environment';
import { ExerciseDetailPage } from './exercise-detail.page';

const DETAIL = {
  exercise: {
    id: 'exercise-1',
    name: 'Press de banca con barra',
    nameEn: 'Barbell Bench Press',
    muscleGroup: 'CHEST',
    secondaryMuscles: ['TRICEPS'],
    equipment: 'BARBELL',
    mechanics: 'COMPOUND',
    bodyweight: false,
    mine: false,
  },
  stats: {
    personalRecords: [{ type: 'MAX_WEIGHT', value: 100, achievedAt: '2026-11-20T10:00:00Z' }],
    bestE1rm: 133.33,
    totalVolume: 1000,
    sessionCount: 1,
    e1rmSeries: [{ date: '2026-11-20T10:00:00Z', value: 133.33 }],
  },
};

describe('ExerciseDetailPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ExerciseDetailPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'exercise-1' } } },
        },
        ...provideTranslateService({ lang: 'es' }),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('renders the exercise and its stats', async () => {
    const fixture = TestBed.createComponent(ExerciseDetailPage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/exercises/exercise-1`).flush(DETAIL);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Press de banca con barra');
    expect(fixture.nativeElement.textContent).toContain('133.33');
    expect(fixture.nativeElement.textContent).toContain('MAX_WEIGHT');
  });
});
