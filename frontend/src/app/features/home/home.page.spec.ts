import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { User } from '../../core/auth/auth.models';
import { SessionStore } from '../../core/auth/session.store';
import { HomePage } from './home.page';

const USER = {
  id: 'user-1',
  displayName: 'Angel',
  email: 'angel@ferro.dev',
  status: 'ACTIVE',
  locale: 'es',
  unitSystem: 'KG',
  theme: 'DARK',
  timeZone: 'Europe/Madrid',
  defaultRestSeconds: 90,
} as User;

const ROUTINE = { id: 'routine-1', name: 'PPL', exerciseCount: 3, lastUsedAt: null };

describe('HomePage', () => {
  let http: HttpTestingController;
  let router: Router;
  let session: SessionStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        ...provideTranslateService({ lang: 'es' }),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    session = TestBed.inject(SessionStore);
    session.setUser(USER);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('loads the active session and routines', async () => {
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/active`).flush(null);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/routines?page=0&size=5`).flush({
      content: [ROUTINE],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('PPL');
  });

  it('starts an empty session and handles an active session conflict', async () => {
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/active`).flush(null);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/routines?page=0&size=5`).flush({
      content: [],
      page: 0,
      size: 5,
      totalElements: 0,
      totalPages: 0,
    });
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as {
      startTraining(): Promise<void>;
      startError: () => string | null;
    };

    const starting = page.startTraining();
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions`).flush({ id: 'session-1' });
    await starting;
    expect(router.navigate).toHaveBeenCalledWith(['/training', 'session-1']);

    const active = page.startTraining();
    http
      .expectOne(`${environment.apiBaseUrl}/workout-sessions`)
      .flush({ activeSessionId: 'session-active' }, { status: 409, statusText: 'Conflict' });
    await active;
    expect(router.navigate).toHaveBeenCalledWith(['/training', 'session-active']);

    const failing = page.startTraining();
    http
      .expectOne(`${environment.apiBaseUrl}/workout-sessions`)
      .flush({}, { status: 500, statusText: 'Error' });
    await failing;
    expect(page.startError()).toContain('No se pudo');
  });

  it('starts a routine and handles an active session conflict', async () => {
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/active`).flush(null);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/routines?page=0&size=5`).flush({
      content: [ROUTINE],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    });
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as {
      startRoutine(routine: typeof ROUTINE): Promise<void>;
      startError: () => string | null;
    };

    const starting = page.startRoutine(ROUTINE);
    http.expectOne(`${environment.apiBaseUrl}/routines/routine-1/start`).flush({ id: 'session-2' });
    await starting;
    expect(router.navigate).toHaveBeenCalledWith(['/training', 'session-2']);

    const active = page.startRoutine(ROUTINE);
    http
      .expectOne(`${environment.apiBaseUrl}/routines/routine-1/start`)
      .flush({ activeSessionId: 'session-active' }, { status: 409, statusText: 'Conflict' });
    await active;
    expect(router.navigate).toHaveBeenCalledWith(['/training', 'session-active']);

    const failing = page.startRoutine(ROUTINE);
    http
      .expectOne(`${environment.apiBaseUrl}/routines/routine-1/start`)
      .flush({}, { status: 500, statusText: 'Error' });
    await failing;
    expect(page.startError()).toContain('No se pudo');
  });

  it('shows the active session banner and navigates back to it', async () => {
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    http
      .expectOne(`${environment.apiBaseUrl}/workout-sessions/active`)
      .flush({ id: 'session-active', name: 'Sesión en curso' });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/routines?page=0&size=5`).flush({
      content: [],
      page: 0,
      size: 5,
      totalElements: 0,
      totalPages: 0,
    });
    await fixture.whenStable();
    fixture.detectChanges();
    const page = fixture.componentInstance as unknown as {
      continueTraining(): Promise<void>;
    };

    await page.continueTraining();
    expect(router.navigate).toHaveBeenCalledWith(['/training', 'session-active']);
  });

  it('tolerates failures loading routines', async () => {
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/workout-sessions/active`).flush(null);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne(`${environment.apiBaseUrl}/routines?page=0&size=5`)
      .flush({}, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as { routines: () => unknown[] };
    expect(page.routines()).toEqual([]);
  });
});
