import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { Exercise } from '../catalog/catalog.models';
import { RoutineBuilderPage } from './routine-builder.page';

const EXERCISE: Exercise = {
  id: 'exercise-1',
  name: 'Press de banca con barra',
  nameEn: 'Barbell Bench Press',
  muscleGroup: 'CHEST',
  secondaryMuscles: [],
  equipment: 'BARBELL',
  mechanics: 'COMPOUND',
  bodyweight: false,
  mine: false,
};

const ROUTINE = {
  id: 'routine-1',
  name: 'PPL',
  notes: 'Empuje',
  exercises: [
    {
      id: 'routine-exercise-1',
      exerciseId: 'exercise-1',
      name: 'Press de banca con barra',
      position: 0,
      restSeconds: 120,
      supersetGroup: null,
      notes: 'Codo alto',
      sets: [
        {
          id: 'target-1',
          position: 0,
          setType: 'NORMAL',
          targetReps: 8,
          targetWeight: 60,
          targetRir: 2,
        },
      ],
    },
  ],
};

describe('RoutineBuilderPage', () => {
  let http: HttpTestingController;
  let router: Router;
  let routeId: string;

  beforeEach(() => {
    routeId = 'new';
    TestBed.configureTestingModule({
      imports: [RoutineBuilderPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => routeId } } },
        },
        ...provideTranslateService({ lang: 'es' }),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('builds and saves a new routine', async () => {
    const fixture = TestBed.createComponent(RoutineBuilderPage);
    fixture.detectChanges();
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as {
      form: { setValue(value: object): void };
      exercises: () => Record<string, unknown>[];
      addExercise(exercise: Exercise): void;
      addSet(exercise: object): void;
      setSeriesCount(exercise: object, event: Event): void;
      setTargetReps(exercise: object, event: Event): void;
      removeSet(exercise: object, set: object): void;
      move(index: number, delta: number): void;
      startDrag(key: string): void;
      dropExercise(exercise: object): void;
      toggle(key: string): void;
      groupSelected(): void;
      ungroupSelected(): void;
      removeExercise(exercise: object): void;
      supersetLabel(exercise: object): string;
      save(): Promise<void>;
      cancel(): Promise<void>;
      error: () => boolean;
    };

    page.form.setValue({ name: 'PPL', notes: 'Empuje' });
    page.addExercise(EXERCISE);
    page.addExercise({ ...EXERCISE, id: 'exercise-2', name: 'Sentadilla' });
    const exercises = page.exercises();
    expect(exercises.length).toBe(2);

    page.setSeriesCount(exercises[0], { target: { value: '5' } } as unknown as Event);
    expect((exercises[0]['sets'] as unknown[]).length).toBe(5);
    page.setTargetReps(exercises[0], { target: { value: '12' } } as unknown as Event);
    expect((exercises[0]['sets'] as { targetReps: number }[])[0].targetReps).toBe(12);
    page.setSeriesCount(exercises[0], { target: { value: '1' } } as unknown as Event);
    page.addSet(exercises[0]);
    page.addSet(exercises[0]);
    page.removeSet(exercises[0], (exercises[0]['sets'] as object[])[0]);
    page.move(0, 1);
    page.move(0, -1);
    page.startDrag(page.exercises()[0]['key'] as string);
    page.dropExercise(page.exercises()[1]);

    const firstKey = page.exercises()[0]['key'] as string;
    const secondKey = page.exercises()[1]['key'] as string;
    page.toggle(firstKey);
    page.toggle(firstKey);
    page.groupSelected();
    page.toggle(firstKey);
    page.toggle(secondKey);
    page.groupSelected();
    expect(page.supersetLabel(page.exercises()[0])).toContain('A1');
    page.ungroupSelected();

    vi.spyOn(window, 'confirm').mockReturnValue(false);
    page.removeExercise(page.exercises()[1]);
    expect(page.exercises().length).toBe(2);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    page.removeExercise(page.exercises()[1]);
    expect(page.exercises().length).toBe(1);
    expect(page.error()).toBe(true);

    const saving = page.save();
    const request = http.expectOne(`${environment.apiBaseUrl}/routines`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toMatchObject({
      name: 'PPL',
      notes: 'Empuje',
      exercises: [{ exerciseId: 'exercise-1', restSeconds: 90 }],
    });
    request.flush(ROUTINE);
    await saving;
    expect(router.navigateByUrl).toHaveBeenCalledWith('/routines');

    await page.cancel();
  });

  it('loads and updates an existing routine', async () => {
    routeId = 'routine-1';
    const fixture = TestBed.createComponent(RoutineBuilderPage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/routines/routine-1`).flush(ROUTINE);
    await fixture.whenStable();
    fixture.detectChanges();
    const page = fixture.componentInstance as unknown as {
      form: { getRawValue(): { name: string; notes: string } };
      exercises: () => Record<string, unknown>[];
      save(): Promise<void>;
    };
    expect(page.form.getRawValue()).toEqual({ name: 'PPL', notes: 'Empuje' });
    expect(page.exercises().length).toBe(1);

    const saving = page.save();
    const request = http.expectOne(`${environment.apiBaseUrl}/routines/routine-1`);
    expect(request.request.method).toBe('PUT');
    request.flush(ROUTINE);
    await saving;
  });

  it('handles load and save errors', async () => {
    routeId = 'routine-1';
    const fixture = TestBed.createComponent(RoutineBuilderPage);
    fixture.detectChanges();
    http
      .expectOne(`${environment.apiBaseUrl}/routines/routine-1`)
      .flush({}, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as {
      error: () => boolean;
      save(): Promise<void>;
    };
    expect(page.error()).toBe(true);

    await page.save();
    expect(page.error()).toBe(true);
  });
});
