import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { Exercise } from '../catalog/catalog.models';
import { TrainingPage } from './training.page';

const SESSION = {
  id: 'session-1',
  name: 'Sesión del 2026-11-26',
  status: 'ACTIVE',
  startedAt: '2026-11-26T10:00:00Z',
  finishedAt: null,
  durationSeconds: null,
  totalVolume: 640,
  exercises: [
    {
      id: 'entry-1',
      exerciseId: 'exercise-1',
      name: 'Press de banca con barra',
      position: 0,
      restSeconds: null,
      supersetGroup: null,
      notes: null,
      sets: [
        {
          id: 'set-1',
          position: 0,
          setType: 'NORMAL',
          reps: 8,
          weight: 80,
          rir: null,
          volume: 640,
          e1rm: 101.33,
          loggedAt: '2026-11-26T10:05:00Z',
        },
      ],
    },
  ],
};

describe('TrainingPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TrainingPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'session-1' } } },
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

  it('renders the active session and its sets', async () => {
    const fixture = TestBed.createComponent(TrainingPage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/session-1`).flush(SESSION);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Press de banca con barra');
    expect(fixture.nativeElement.textContent).toContain('640');
  });

  it('formats and controls the rest timer', () => {
    const fixture = TestBed.createComponent(TrainingPage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/session-1`).flush(SESSION);
    const page = fixture.componentInstance as unknown as {
      restRemaining: { set(value: number | null): void; (): number | null };
      formatRest(seconds: number): string;
      addRest(): void;
      skipRest(): void;
      ngOnDestroy(): void;
    };

    expect(page.formatRest(90)).toBe('1:30');
    page.restRemaining.set(90);
    page.addRest();
    expect(page.restRemaining()).toBe(105);
    page.skipRest();
    expect(page.restRemaining()).toBeNull();
    page.ngOnDestroy();
  });

  it('adds an exercise and discards the session', async () => {
    const fixture = TestBed.createComponent(TrainingPage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/session-1`).flush(SESSION);
    await fixture.whenStable();
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const page = fixture.componentInstance as unknown as {
      addExercise(exercise: Exercise): Promise<void>;
      discard(): Promise<void>;
    };

    const adding = page.addExercise({
      id: 'exercise-2',
      name: 'Sentadilla',
      nameEn: 'Squat',
      muscleGroup: 'QUADS',
      secondaryMuscles: [],
      equipment: 'BARBELL',
      mechanics: 'COMPOUND',
      bodyweight: false,
      mine: false,
    });
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/session-1/exercises`).flush(SESSION);
    await adding;

    const discarding = page.discard();
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/session-1`).flush(null);
    await discarding;

    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('logs a set and finishes the session', async () => {
    const fixture = TestBed.createComponent(TrainingPage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/session-1`).flush(SESSION);
    await fixture.whenStable();
    fixture.detectChanges();

    const page = fixture.componentInstance as unknown as {
      setForm: { setValue(value: object): void };
      logSet(): Promise<void>;
      finish(): Promise<void>;
    };
    page.setForm.setValue({ entryId: 'entry-1', reps: 10, weight: 85, rir: 2 });
    const logging = page.logSet();

    const post = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-1/sets`,
    );
    expect(post.request.body).toMatchObject({ reps: 10, weight: 85, rir: 2, setType: 'NORMAL' });
    post.flush(SESSION.exercises[0].sets[0]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/session-1`).flush(SESSION);
    await logging;

    const finishing = page.finish();
    const finishRequest = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/finish`,
    );
    finishRequest.flush({
      session: { ...SESSION, status: 'FINISHED', totalVolume: 1490 },
      newPersonalRecords: [
        {
          exerciseId: 'exercise-1',
          exerciseName: 'Press de banca con barra',
          prType: 'MAX_WEIGHT',
          value: 85,
          previousValue: 80,
          achievedAt: '2026-11-26T10:10:00Z',
        },
      ],
    });
    await finishing;
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('MAX_WEIGHT');
  });
});
