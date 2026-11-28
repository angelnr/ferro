import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { App } from './app';
import { AuthStore } from './core/auth/auth.store';
import { AuthResponse } from './core/auth/auth.models';
import { SessionStore } from './core/auth/session.store';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        ...provideTranslateService({ lang: 'es', fallbackLang: 'en' }),
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the brand and the login link when anonymous', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('header a')?.textContent).toContain('Ferro');
    expect(compiled.textContent).toContain('nav.login');
  });

  it('shows and hides the offline banner', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      offline: () => boolean;
      ngOnDestroy(): void;
    };
    window.dispatchEvent(new Event('offline'));
    fixture.detectChanges();
    expect(component.offline()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('app.offline');
    window.dispatchEvent(new Event('online'));
    fixture.detectChanges();
    expect(component.offline()).toBe(false);
    component.ngOnDestroy();
  });

  it('should show the user and allow logout when authenticated', async () => {
    const session = TestBed.inject(SessionStore);
    session.setSession(AUTH_RESPONSE);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('p1@ferro.dev');

    const auth = TestBed.inject(AuthStore);
    const logout = vi.spyOn(auth, 'logout').mockResolvedValue(undefined);
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const button = fixture.nativeElement.querySelector('.logout-button') as HTMLButtonElement;
    button.click();
    await fixture.whenStable();

    expect(logout).toHaveBeenCalled();
  });
});

const AUTH_RESPONSE: AuthResponse = {
  accessToken: 'token',
  user: {
    id: 'id',
    displayName: 'Ángel',
    email: 'p1@ferro.dev',
    status: 'UNVERIFIED',
    locale: 'es',
    unitSystem: 'KG',
    theme: 'DARK',
    timeZone: 'Europe/Madrid',
    defaultRestSeconds: 90,
  },
};
