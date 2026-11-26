import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { Exercise } from '../catalog/catalog.models';
import { TrainingPage } from './training.page';
import { TrainingApi } from './training.api';

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

const SESSION_TWO = {
  ...SESSION,
  exercises: [
    ...SESSION.exercises,
    {
      id: 'entry-2',
      exerciseId: 'exercise-2',
      name: 'Sentadilla trasera con barra',
      position: 1,
      restSeconds: null,
      supersetGroup: null,
      notes: null,
      sets: [],
    },
  ],
} as typeof SESSION;

const SESSION_URL = `${environment.apiBaseUrl}/workout-sessions/session-1`;
const HISTORY_URL = `${environment.apiBaseUrl}/exercises/exercise-1/history?limit=3`;
const PR_URL = `${environment.apiBaseUrl}/exercises/exercise-1`;
const PR_RESPONSE = {
  exercise: { id: 'exercise-1', name: 'Press de banca con barra' },
  stats: { personalRecords: [] },
};

describe('TrainingPage', () => {
  let http: HttpTestingController;

  async function loadSession(fixture: ComponentFixture<TrainingPage>): Promise<void> {
    http.expectOne(SESSION_URL).flush(SESSION);
    await fixture.whenStable();
    http.expectOne(HISTORY_URL).flush([]);
    http.expectOne(PR_URL).flush(PR_RESPONSE);
    await fixture.whenStable();
  }

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
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('renders the active session and its sets', async () => {
    const fixture = TestBed.createComponent(TrainingPage);
    fixture.detectChanges();
    await loadSession(fixture);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Press de banca con barra');
    expect(fixture.nativeElement.textContent).toContain('640');
  });

  it('starts a session through the API', () => {
    const api = TestBed.inject(TrainingApi);
    let started: unknown = null;

    api.start().subscribe((session) => {
      started = session;
    });

    const request = http.expectOne(`${environment.apiBaseUrl}/workout-sessions`);
    expect(request.request.method).toBe('POST');
    request.flush(SESSION);
    expect(started).toEqual(SESSION);
  });

  it('ignores actions without an active session', async () => {
    const fixture = TestBed.createComponent(TrainingPage);
    fixture.detectChanges();
    const page = fixture.componentInstance as unknown as {
      addExercise(exercise: Exercise): Promise<void>;
      logSet(): Promise<void>;
      finish(): Promise<void>;
      discard(): Promise<void>;
      move(entryId: string, delta: number): Promise<void>;
      startDrag(entryId: string): void;
      dropEntry(entry: object): Promise<void>;
      groupSelected(): Promise<void>;
      ungroupSelected(): Promise<void>;
    };

    await page.addExercise({
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
    await page.logSet();
    await page.finish();
    await page.discard();
    await page.move('entry-1', 1);
    await page.groupSelected();
    await page.ungroupSelected();

    http.expectOne(SESSION_URL).flush(SESSION);
    await fixture.whenStable();
    http.expectOne(HISTORY_URL).flush([]);
    http.expectOne(PR_URL).flush(PR_RESPONSE);
    await fixture.whenStable();
  });

  it('moves exercises and toggles their selection', async () => {
    const fixture = TestBed.createComponent(TrainingPage);
    fixture.detectChanges();
    http.expectOne(SESSION_URL).flush(SESSION_TWO);
    await fixture.whenStable();
    http.expectOne(HISTORY_URL).flush([]);
    http.expectOne(PR_URL).flush(PR_RESPONSE);
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as {
      selectedEntries: () => Set<string>;
      toggleEntry(id: string): void;
      move(entryId: string, delta: number): Promise<void>;
      startDrag(entryId: string): void;
      dropEntry(entry: object): Promise<void>;
      groupSelected(): Promise<void>;
      ungroupSelected(): Promise<void>;
    };

    const moving = page.move('entry-1', 1);
    const request = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-1`,
    );
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ position: 1 });
    request.flush(SESSION_TWO);
    await moving;

    page.startDrag('entry-1');
    const dropping = page.dropEntry(SESSION_TWO.exercises[1]);
    const dropRequest = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-1`,
    );
    expect(dropRequest.request.body).toEqual({ position: 1 });
    dropRequest.flush(SESSION_TWO);
    await dropping;

    await page.move('missing', 1);
    await page.move('entry-1', -1);
    page.toggleEntry('entry-1');
    expect(page.selectedEntries().has('entry-1')).toBe(true);
    page.toggleEntry('entry-1');
    expect(page.selectedEntries().has('entry-1')).toBe(false);
    await page.groupSelected();
    await page.ungroupSelected();
  });

  it('edits through the template actions', async () => {
    const fixture = TestBed.createComponent(TrainingPage);
    fixture.detectChanges();
    http.expectOne(SESSION_URL).flush(SESSION_TWO);
    await fixture.whenStable();
    http.expectOne(HISTORY_URL).flush([]);
    http.expectOne(PR_URL).flush(PR_RESPONSE);
    await fixture.whenStable();
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    const findButton = (label: string): HTMLButtonElement | undefined =>
      Array.from(
        fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
      ).find((button) => (button.textContent ?? '').trim() === label);

    findButton('training.editSession')?.click();
    fixture.detectChanges();
    findButton('training.cancel')?.click();
    fixture.detectChanges();

    fixture.nativeElement.querySelector('input[type="checkbox"]')?.click();
    fixture.detectChanges();

    findButton('training.edit')?.click();
    fixture.detectChanges();
    findButton('training.cancel')?.click();
    fixture.detectChanges();

    findButton('training.delete')?.click();
    const request = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-1/sets/set-1`,
    );
    expect(request.request.method).toBe('DELETE');
    request.flush(SESSION_TWO);
    await fixture.whenStable();
  });

  it('updates and deletes session resources through the API', () => {
    const api = TestBed.inject(TrainingApi);

    api.updateSession('session-1', { name: 'Nueva', notes: 'Nota' }).subscribe();
    let request = http.expectOne(`${environment.apiBaseUrl}/workout-sessions/session-1`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ name: 'Nueva', notes: 'Nota' });
    request.flush(SESSION);

    api
      .updateSet('session-1', 'entry-1', 'set-1', {
        reps: 9,
        weight: 82.5,
        rir: 1,
        setType: 'FAILURE',
        notes: 'Dura',
      })
      .subscribe();
    request = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-1/sets/set-1`,
    );
    expect(request.request.method).toBe('PATCH');
    request.flush(SESSION);

    api.deleteSet('session-1', 'entry-1', 'set-1').subscribe();
    request = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-1/sets/set-1`,
    );
    expect(request.request.method).toBe('DELETE');
    request.flush(SESSION);

    api.deleteExercise('session-1', 'entry-2').subscribe();
    request = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-2`,
    );
    expect(request.request.method).toBe('DELETE');
    request.flush(SESSION);
  });

  it('edits the session, exercises and sets', async () => {
    const fixture = TestBed.createComponent(TrainingPage);
    fixture.detectChanges();
    http.expectOne(SESSION_URL).flush(SESSION_TWO);
    await fixture.whenStable();
    http.expectOne(HISTORY_URL).flush([]);
    http.expectOne(PR_URL).flush(PR_RESPONSE);
    await fixture.whenStable();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const page = fixture.componentInstance as unknown as {
      sessionForm: { setValue(value: object): void; getRawValue(): object };
      editSetForm: { setValue(value: object): void };
      startSessionEdit(): void;
      cancelSessionEdit(): void;
      saveSession(): Promise<void>;
      changeRest(entryId: string, event: Event): void;
      changeNotes(entryId: string, event: Event): void;
      deleteEntry(entry: object): Promise<void>;
      startSetEdit(entryId: string, set: object): void;
      cancelSetEdit(): void;
      saveSetEdit(): Promise<void>;
      deleteSet(entryId: string, set: object): Promise<void>;
      formatDuration(seconds: number): string;
      totalSets(): number;
      bestSet(session: object): { loggedAt: string } | null;
    };

    page.startSessionEdit();
    page.sessionForm.setValue({ name: 'Sesión editada', notes: 'Pesado' });
    const saving = page.saveSession();
    let request = http.expectOne(SESSION_URL);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ name: 'Sesión editada', notes: 'Pesado' });
    request.flush(SESSION_TWO);
    await saving;

    page.changeRest('entry-1', { target: { value: '120' } } as unknown as Event);
    request = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-1`,
    );
    expect(request.request.body).toEqual({ restSeconds: 120 });
    request.flush(SESSION_TWO);

    page.changeNotes('entry-1', { target: { value: '  Codo alto  ' } } as unknown as Event);
    request = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-1`,
    );
    expect(request.request.body).toEqual({ notes: 'Codo alto' });
    request.flush(SESSION_TWO);

    const deletingEntry = page.deleteEntry(SESSION_TWO.exercises[1]);
    request = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-2`,
    );
    expect(request.request.method).toBe('DELETE');
    request.flush(SESSION);
    await deletingEntry;

    page.startSetEdit('entry-1', SESSION.exercises[0].sets[0]);
    page.editSetForm.setValue({
      reps: 9,
      weight: 82.5,
      rir: 1,
      setType: 'FAILURE',
      notes: 'Dura',
    });
    const savingSet = page.saveSetEdit();
    request = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-1/sets/set-1`,
    );
    expect(request.request.method).toBe('PATCH');
    request.flush(SESSION_TWO);
    await savingSet;

    const deletingSet = page.deleteSet('entry-1', SESSION.exercises[0].sets[0]);
    request = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-1/sets/set-1`,
    );
    expect(request.request.method).toBe('DELETE');
    request.flush(SESSION_TWO);
    await deletingSet;

    page.startSetEdit('entry-1', SESSION.exercises[0].sets[0]);
    page.cancelSetEdit();
    page.startSessionEdit();
    page.cancelSessionEdit();
    expect(page.formatDuration(3661)).toBe('1:01:01');
    expect(page.formatDuration(90)).toBe('1:30');
    expect(page.totalSets()).toBe(1);
    expect(page.bestSet({ sets: [{ loggedAt: 'a', setType: 'NORMAL', weight: 80 }] })).toBeTruthy();
  });

  it('shows the previous performance of the exercise', async () => {
    const fixture = TestBed.createComponent(TrainingPage);
    fixture.detectChanges();
    http.expectOne(SESSION_URL).flush(SESSION);
    await fixture.whenStable();
    http.expectOne(HISTORY_URL).flush([
      {
        sessionId: 'session-0',
        startedAt: '2026-11-20T10:00:00Z',
        sets: [
          {
            reps: 8,
            weight: 80,
            rir: 2,
            setType: 'NORMAL',
            loggedAt: '2026-11-20T10:05:00Z',
          },
        ],
      },
    ]);
    http.expectOne(PR_URL).flush(PR_RESPONSE);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('2026-11-20');
    expect(fixture.nativeElement.textContent).toContain('8×80');
  });

  it('groups and ungroups selected exercises', async () => {
    const fixture = TestBed.createComponent(TrainingPage);
    fixture.detectChanges();
    await loadSession(fixture);
    const page = fixture.componentInstance as unknown as {
      selectedEntries: { set(value: Set<string>): void; (): Set<string> };
      toggleEntry(id: string): void;
      groupSelected(): Promise<void>;
      ungroupSelected(): Promise<void>;
      move(entryId: string, delta: number): Promise<void>;
    };

    page.toggleEntry('entry-1');
    expect(page.selectedEntries().has('entry-1')).toBe(true);
    page.selectedEntries.set(new Set(['entry-1', 'entry-2']));
    const grouping = page.groupSelected();
    const groupRequest = http.expectOne(
      `${environment.apiBaseUrl}/workout-sessions/session-1/supersets`,
    );
    expect(groupRequest.request.method).toBe('PATCH');
    expect(groupRequest.request.body).toMatchObject({ group: 1 });
    groupRequest.flush(SESSION);
    await grouping;

    await page.move('entry-1', 1);
    page.selectedEntries.set(new Set(['entry-1']));
    const ungrouping = page.ungroupSelected();
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/session-1/supersets`).flush(SESSION);
    await ungrouping;
  });

  it('formats and controls the rest timer', async () => {
    const fixture = TestBed.createComponent(TrainingPage);
    fixture.detectChanges();
    await loadSession(fixture);
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
    await loadSession(fixture);
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
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
    await loadSession(fixture);
    fixture.detectChanges();

    const page = fixture.componentInstance as unknown as {
      setForm: { setValue(value: object): void };
      logSet(): Promise<void>;
      finish(): Promise<void>;
    };
    page.setForm.setValue({
      entryId: 'entry-1',
      reps: 10,
      weight: 85,
      rir: 2,
      setType: 'NORMAL',
      notes: '',
    });
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
