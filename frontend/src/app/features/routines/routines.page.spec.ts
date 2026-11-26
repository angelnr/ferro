import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { RoutinesPage } from './routines.page';

const ROUTINE = { id: 'routine-1', name: 'PPL', exerciseCount: 3, lastUsedAt: null };

describe('RoutinesPage', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RoutinesPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        ...provideTranslateService({ lang: 'es' }),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
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

  it('loads routines and starts one', async () => {
    const fixture = TestBed.createComponent(RoutinesPage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/routines?page=0&size=20`).flush({
      content: [ROUTINE],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('PPL');

    const page = fixture.componentInstance as unknown as {
      start(routine: typeof ROUTINE): Promise<void>;
      startError: () => string | null;
    };
    const starting = page.start(ROUTINE);
    http.expectOne(`${environment.apiBaseUrl}/routines/routine-1/start`).flush({ id: 'session-1' });
    await starting;
    expect(router.navigate).toHaveBeenCalledWith(['/training', 'session-1']);

    const active = page.start(ROUTINE);
    http
      .expectOne(`${environment.apiBaseUrl}/routines/routine-1/start`)
      .flush({ activeSessionId: 'session-active' }, { status: 409, statusText: 'Conflict' });
    await active;
    expect(router.navigate).toHaveBeenCalledWith(['/training', 'session-active']);

    const failing = page.start(ROUTINE);
    http
      .expectOne(`${environment.apiBaseUrl}/routines/routine-1/start`)
      .flush({}, { status: 500, statusText: 'Error' });
    await failing;
    expect(page.startError()).toContain('No se pudo');
  });

  it('duplicates and deletes routines', async () => {
    const fixture = TestBed.createComponent(RoutinesPage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/routines?page=0&size=20`).flush({
      content: [ROUTINE],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as {
      duplicate(routine: typeof ROUTINE): Promise<void>;
      remove(routine: typeof ROUTINE): Promise<void>;
      error: () => boolean;
    };

    const duplicating = page.duplicate(ROUTINE);
    http.expectOne(`${environment.apiBaseUrl}/routines/routine-1/duplicate`).flush({});
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/routines?page=0&size=20`).flush({
      content: [ROUTINE],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    await duplicating;

    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    await page.remove(ROUTINE);
    expect(confirm).toHaveBeenCalled();

    confirm.mockReturnValue(true);
    const removing = page.remove(ROUTINE);
    http.expectOne(`${environment.apiBaseUrl}/routines/routine-1`).flush(null);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/routines?page=0&size=20`).flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
    await removing;
    expect(page.error()).toBe(false);
  });

  it('shows an error when the list cannot be loaded', async () => {
    const fixture = TestBed.createComponent(RoutinesPage);
    fixture.detectChanges();
    http
      .expectOne(`${environment.apiBaseUrl}/routines?page=0&size=20`)
      .flush({}, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    const page = fixture.componentInstance as unknown as { error: () => boolean };
    expect(page.error()).toBe(true);
  });
});
