import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { SessionDetailPage } from './session-detail.page';

const DETAIL = {
  id: 'session-1',
  name: 'PPL',
  notes: 'Buena',
  startedAt: '2026-11-20T10:00:00Z',
  finishedAt: '2026-11-20T11:00:00Z',
  durationSeconds: 3660,
  totalVolume: 640,
  status: 'FINISHED',
  exercises: [
    {
      id: 'entry-1',
      exerciseId: 'exercise-1',
      name: 'Press',
      position: 0,
      volume: 640,
      notes: null,
      sets: [
        {
          id: 'set-1',
          position: 0,
          setType: 'NORMAL',
          reps: 8,
          weight: 80,
          rir: 2,
          volume: 640,
          e1rm: 101.33,
          notes: null,
          loggedAt: '2026-11-20T10:05:00Z',
        },
      ],
      personalRecords: [{ type: 'MAX_WEIGHT', value: 80 }],
    },
  ],
};

describe('SessionDetailPage', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [SessionDetailPage],
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

  it('loads and edits a finished session and its sets', async () => {
    const fixture = TestBed.createComponent(SessionDetailPage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/progress/sessions/session-1`).flush(DETAIL);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('PPL');
    const page = fixture.componentInstance as unknown as {
      value(event: Event): string;
      sessionDuration(): string;
      startEdit(): void;
      saveSession(): Promise<void>;
      startSetEdit(exercise: object, set: object): void;
      saveSet(exercise: object, set: object): Promise<void>;
      removeSet(exercise: object, set: object): Promise<void>;
      removeSession(): Promise<void>;
    };
    expect(page.value({ target: { value: 'x' } } as unknown as Event)).toBe('x');
    expect(page.sessionDuration()).toContain('1 h');

    page.startEdit();
    const saving = page.saveSession();
    http
      .expectOne(`${environment.apiBaseUrl}/workout-sessions/session-1`)
      .flush({ id: 'session-1' });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/progress/sessions/session-1`).flush(DETAIL);
    await saving;

    page.startSetEdit(DETAIL.exercises[0], DETAIL.exercises[0].sets[0]);
    const savingSet = page.saveSet(DETAIL.exercises[0], DETAIL.exercises[0].sets[0]);
    http
      .expectOne(
        `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-1/sets/set-1`,
      )
      .flush({ id: 'session-1' });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/progress/sessions/session-1`).flush(DETAIL);
    await savingSet;

    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const removingSet = page.removeSet(DETAIL.exercises[0], DETAIL.exercises[0].sets[0]);
    http
      .expectOne(
        `${environment.apiBaseUrl}/workout-sessions/session-1/exercises/entry-1/sets/set-1`,
      )
      .flush({ id: 'session-1' });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/progress/sessions/session-1`).flush(DETAIL);
    await removingSet;

    const removing = page.removeSession();
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/session-1`).flush(null);
    await removing;
    expect(router.navigateByUrl).toHaveBeenCalledWith('/history');
  });

  it('shows an error when the session cannot be loaded', async () => {
    const fixture = TestBed.createComponent(SessionDetailPage);
    fixture.detectChanges();
    http
      .expectOne(`${environment.apiBaseUrl}/progress/sessions/session-1`)
      .flush({}, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as { error: () => boolean };
    expect(page.error()).toBe(true);
  });
});
