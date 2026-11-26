import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { StatsPage } from './stats.page';

const SUMMARY = {
  weeks: 12,
  weeklyVolume: [{ weekStart: '2026-11-16T00:00:00Z', value: 1000 }],
  weeklySessions: [{ weekStart: '2026-11-16T00:00:00Z', value: 2 }],
  currentStreak: 3,
  bestStreak: 5,
  current: { volume: 1000, sessions: 2, sets: 8 },
  previous: { volume: 800, sessions: 1, sets: 6 },
  muscleDistribution: [{ muscleGroup: 'CHEST', volume: 600 }],
  periodStart: '2026-09-01T00:00:00Z',
  periodEnd: '2026-11-24T00:00:00Z',
};

describe('StatsPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [StatsPage],
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

  it('loads ranges and computes chart values', async () => {
    const fixture = TestBed.createComponent(StatsPage);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/progress/summary?weeks=12`).flush(SUMMARY);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('1,000');
    const page = fixture.componentInstance as unknown as {
      load(weeks: number): Promise<void>;
      delta(current: number, previous: number): number;
      maxVolume(): number;
      maxSessions(): number;
      maxMuscle(): number;
      barHeight(value: number, max: number): number;
    };
    expect(page.delta(100, 0)).toBe(100);
    expect(page.delta(80, 100)).toBe(-20);
    expect(page.maxVolume()).toBe(1000);
    expect(page.maxSessions()).toBe(2);
    expect(page.maxMuscle()).toBe(600);
    expect(page.barHeight(50, 100)).toBe(50);

    const loading = page.load(4);
    http
      .expectOne(`${environment.apiBaseUrl}/progress/summary?weeks=4`)
      .flush({ ...SUMMARY, weeks: 4 });
    await loading;
  });

  it('shows an error when stats fail', async () => {
    const fixture = TestBed.createComponent(StatsPage);
    fixture.detectChanges();
    http
      .expectOne(`${environment.apiBaseUrl}/progress/summary?weeks=12`)
      .flush({}, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as { error: () => boolean };
    expect(page.error()).toBe(true);
  });
});
