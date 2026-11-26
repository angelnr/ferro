import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { environment } from '../../../../environments/environment';
import { OAuthButtons } from './oauth-buttons';

describe('OAuthButtons', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [OAuthButtons],
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

  it('hides the buttons when no provider is configured', async () => {
    const fixture = TestBed.createComponent(OAuthButtons);
    fixture.detectChanges();
    http
      .expectOne(`${environment.apiBaseUrl}/auth/oauth/providers`)
      .flush({ google: false, github: false });
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).not.toContain('Google');
  });

  it('renders the configured providers', async () => {
    const fixture = TestBed.createComponent(OAuthButtons);
    fixture.detectChanges();
    http
      .expectOne(`${environment.apiBaseUrl}/auth/oauth/providers`)
      .flush({ google: true, github: true });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('auth.continueGoogle');
    expect(fixture.nativeElement.textContent).toContain('auth.continueGithub');
  });
});
