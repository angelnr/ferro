import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { CatalogPage } from './catalog.page';

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

describe('CatalogPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CatalogPage],
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

  it('loads and renders exercises', async () => {
    const fixture = TestBed.createComponent(CatalogPage);
    fixture.detectChanges();
    http.expectOne((request) => request.url === `${environment.apiBaseUrl}/exercises`).flush(PAGE);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Press de banca con barra');
    expect(fixture.nativeElement.textContent).toContain('CHEST');
  });

  it('creates a custom exercise and refreshes the list', async () => {
    const fixture = TestBed.createComponent(CatalogPage);
    fixture.detectChanges();
    http.expectOne((request) => request.url === `${environment.apiBaseUrl}/exercises`).flush(PAGE);
    await fixture.whenStable();

    const page = fixture.componentInstance as unknown as {
      createForm: { setValue(value: object): void };
      create(): Promise<void>;
    };
    page.createForm.setValue({
      nameEs: 'Curl en banco',
      nameEn: 'Bench Curl',
      muscleGroup: 'BICEPS',
      equipment: 'DUMBBELL',
      mechanics: 'ISOLATION',
      bodyweight: false,
    });
    const creating = page.create();

    const post = http.expectOne(`${environment.apiBaseUrl}/exercises`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toMatchObject({ nameEs: 'Curl en banco', muscleGroup: 'BICEPS' });
    post.flush({ ...PAGE.content[0], id: 'custom-1', name: 'Curl en banco', mine: true });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne((request) => request.url === `${environment.apiBaseUrl}/exercises`).flush(PAGE);
    await creating;
  });
});
