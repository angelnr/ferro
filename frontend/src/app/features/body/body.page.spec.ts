import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { BodyPage } from './body.page';

const MEASUREMENT = {
  id: 'body-1',
  measuredAt: '2026-11-20T10:00:00Z',
  weight: 80,
  chest: null,
  waist: 82,
  hip: null,
  arm: null,
  thigh: null,
  notes: 'Mañana',
};

const STATS = {
  weight: [
    { date: '2026-11-01T00:00:00Z', value: 81 },
    { date: '2026-11-20T00:00:00Z', value: 80 },
  ],
  chest: [],
  waist: [{ date: '2026-11-20T00:00:00Z', value: 82 }],
  hip: [],
  arm: [],
  thigh: [],
};

describe('BodyPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BodyPage],
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

  async function load(
    fixture: ReturnType<typeof TestBed.createComponent<BodyPage>>,
  ): Promise<void> {
    http.expectOne(`${environment.apiBaseUrl}/body-measurements?page=0&size=20`).flush({
      content: [MEASUREMENT],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    http.expectOne(`${environment.apiBaseUrl}/body-measurements/stats?months=3`).flush(STATS);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('loads measurements and computes the chart', async () => {
    const fixture = TestBed.createComponent(BodyPage);
    fixture.detectChanges();
    await load(fixture);
    expect(fixture.nativeElement.textContent).toContain('80');
    const page = fixture.componentInstance as unknown as {
      points(): string;
      dots(): { x: number; y: number }[];
      firstValue(): number;
      lastValue(): number;
      loadStats(months: number): Promise<void>;
    };
    expect(page.points()).toContain(',');
    expect(page.dots().length).toBe(2);
    expect(page.firstValue()).toBe(81);
    expect(page.lastValue()).toBe(80);

    const loading = page.loadStats(1);
    http.expectOne(`${environment.apiBaseUrl}/body-measurements/stats?months=1`).flush(STATS);
    await loading;
  });

  it('creates and edits measurements', async () => {
    const fixture = TestBed.createComponent(BodyPage);
    fixture.detectChanges();
    await load(fixture);
    const page = fixture.componentInstance as unknown as {
      form: {
        setValue(value: object): void;
        reset(value: object): void;
        getRawValue(): Record<string, unknown>;
      };
      save(): Promise<void>;
      edit(measurement: typeof MEASUREMENT): void;
      cancelEdit(): void;
      remove(measurement: typeof MEASUREMENT): Promise<void>;
      error: () => boolean;
    };
    page.form.setValue({
      date: '2026-11-21',
      weight: 79.5,
      chest: null,
      waist: null,
      hip: null,
      arm: null,
      thigh: null,
      notes: '',
    });
    const creating = page.save();
    http.expectOne(`${environment.apiBaseUrl}/body-measurements`).flush(MEASUREMENT);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/body-measurements?page=0&size=20`).flush({
      content: [MEASUREMENT],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    http.expectOne(`${environment.apiBaseUrl}/body-measurements/stats?months=3`).flush(STATS);
    await creating;

    page.edit(MEASUREMENT);
    const updating = page.save();
    http.expectOne(`${environment.apiBaseUrl}/body-measurements/body-1`).flush(MEASUREMENT);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/body-measurements?page=0&size=20`).flush({
      content: [MEASUREMENT],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    http.expectOne(`${environment.apiBaseUrl}/body-measurements/stats?months=3`).flush(STATS);
    await updating;

    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const removing = page.remove(MEASUREMENT);
    http.expectOne(`${environment.apiBaseUrl}/body-measurements/body-1`).flush(null);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/body-measurements?page=0&size=20`).flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
    http.expectOne(`${environment.apiBaseUrl}/body-measurements/stats?months=3`).flush(STATS);
    await removing;
    page.cancelEdit();
  });
});
