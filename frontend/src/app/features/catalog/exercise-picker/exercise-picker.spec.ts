import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../../environments/environment';
import { ExercisePicker } from './exercise-picker';

const PAGE = {
  content: [
    {
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
  ],
  page: 0,
  size: 30,
  totalElements: 1,
  totalPages: 1,
};

describe('ExercisePicker', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ExercisePicker],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ...provideTranslateService({ lang: 'es' }),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('searches with debounce and emits the selected exercise', async () => {
    const fixture = TestBed.createComponent(ExercisePicker);
    const picker = fixture.componentInstance as unknown as {
      query: { setValue(value: string): void };
      selectedExercise: { subscribe(fn: (value: unknown) => void): void };
    };
    let selected: unknown = null;
    picker.selectedExercise.subscribe((value) => (selected = value));

    picker.query.setValue('banca');
    await new Promise((resolve) => setTimeout(resolve, 300));
    http.expectOne((request) => request.url === `${environment.apiBaseUrl}/exercises`).flush(PAGE);
    await fixture.whenStable();
    fixture.detectChanges();
    const option = fixture.nativeElement.querySelector('[role="option"]') as HTMLElement;
    expect(option.textContent).toContain('Press de banca con barra');
    option.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));

    expect(selected).toMatchObject({ id: 'exercise-1' });
  });

  it('emits a create request when there are no results', async () => {
    const fixture = TestBed.createComponent(ExercisePicker);
    const picker = fixture.componentInstance as unknown as {
      query: { setValue(value: string): void };
      createRequested: { subscribe(fn: (value: string) => void): void };
    };
    let requested = '';
    picker.createRequested.subscribe((value) => (requested = value));

    picker.query.setValue('Ejercicio nuevo');
    await new Promise((resolve) => setTimeout(resolve, 300));
    http
      .expectOne((request) => request.url === `${environment.apiBaseUrl}/exercises`)
      .flush({ ...PAGE, content: [], totalElements: 0 });
    await fixture.whenStable();
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button).toBeTruthy();
    button.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));

    expect(requested).toBe('Ejercicio nuevo');
  });
});
