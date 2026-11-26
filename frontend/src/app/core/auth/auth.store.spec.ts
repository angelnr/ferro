import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AuthResponse } from './auth.models';
import { AuthStore } from './auth.store';
import { SessionStore } from './session.store';

const AUTH_RESPONSE: AuthResponse = {
  accessToken: 'access-token',
  user: {
    id: '01936a1e-0000-7000-8000-000000000000',
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

describe('AuthStore', () => {
  let store: InstanceType<typeof AuthStore>;
  let session: SessionStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(AuthStore);
    session = TestBed.inject(SessionStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('logs in and keeps the token in memory', async () => {
    const promise = store.login({ email: 'p1@ferro.dev', password: 'Ferro12345' });
    const request = http.expectOne(`${environment.apiBaseUrl}/auth/login`);

    expect(request.request.withCredentials).toBe(true);
    expect(request.request.body).toEqual({ email: 'p1@ferro.dev', password: 'Ferro12345' });
    request.flush(AUTH_RESPONSE);

    await expect(promise).resolves.toBe(true);
    expect(session.isAuthenticated()).toBe(true);
    expect(session.accessToken()).toBe('access-token');
    expect(session.user()?.displayName).toBe('Ángel');
  });

  it('exposes backend validation errors', async () => {
    const promise = store.login({ email: 'p1@ferro.dev', password: 'bad' });
    http.expectOne(`${environment.apiBaseUrl}/auth/login`).flush(
      {
        code: 'INVALID_CREDENTIALS',
        detail: 'Email o contraseña incorrectos',
      },
      { status: 401, statusText: 'Unauthorized' },
    );

    await expect(promise).resolves.toBe(false);
    expect(store.error()).toBe('Email o contraseña incorrectos');
  });

  it('restores a session with the refresh cookie', async () => {
    const promise = store.restore();
    http.expectOne(`${environment.apiBaseUrl}/auth/refresh`).flush(AUTH_RESPONSE);
    await promise;

    expect(session.isAuthenticated()).toBe(true);
    expect(store.initialized()).toBe(true);
  });

  it('marks the app initialized when there is no refresh cookie', async () => {
    const promise = store.restore();
    http
      .expectOne(`${environment.apiBaseUrl}/auth/refresh`)
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    await promise;

    expect(session.isAuthenticated()).toBe(false);
    expect(store.initialized()).toBe(true);
  });

  it('logs out and clears the session', async () => {
    session.setSession(AUTH_RESPONSE);
    const promise = store.logout();
    http.expectOne(`${environment.apiBaseUrl}/auth/logout`).flush(null);
    await promise;

    expect(session.isAuthenticated()).toBe(false);
  });

  it('registers and stores the session', async () => {
    const promise = store.register({
      displayName: 'Ángel',
      email: 'p1@ferro.dev',
      password: 'Ferro12345',
      healthConsent: true,
    });
    const request = http.expectOne(`${environment.apiBaseUrl}/auth/register`);

    expect(request.request.withCredentials).toBe(true);
    request.flush(AUTH_RESPONSE);

    await expect(promise).resolves.toBe(true);
    expect(session.user()?.email).toBe('p1@ferro.dev');
  });

  it('clears all sessions', async () => {
    session.setSession(AUTH_RESPONSE);
    const promise = store.logoutAll();
    http.expectOne(`${environment.apiBaseUrl}/auth/logout-all`).flush(null);
    await promise;

    expect(session.isAuthenticated()).toBe(false);
  });

  it('confirms an email and exposes failure', async () => {
    let promise = store.confirmEmail('good-token');
    http.expectOne(`${environment.apiBaseUrl}/auth/email-verification/confirm`).flush(null);
    await expect(promise).resolves.toBe(true);

    promise = store.confirmEmail('bad-token');
    http
      .expectOne(`${environment.apiBaseUrl}/auth/email-verification/confirm`)
      .flush(
        { code: 'VERIFICATION_INVALID', detail: 'Token inválido' },
        { status: 400, statusText: 'Bad Request' },
      );
    await expect(promise).resolves.toBe(false);
    expect(store.error()).toBe('Token inválido');
  });

  it('resends verification and requests a password reset', async () => {
    let promise = store.resendVerification();
    http.expectOne(`${environment.apiBaseUrl}/auth/email-verification/resend`).flush(null);
    await expect(promise).resolves.toBe(true);

    promise = store.requestPasswordReset('p1@ferro.dev');
    const resetRequest = http.expectOne(`${environment.apiBaseUrl}/auth/password-reset/request`);
    expect(resetRequest.request.body).toEqual({ email: 'p1@ferro.dev' });
    resetRequest.flush(null);
    await expect(promise).resolves.toBe(true);
  });

  it('resets the password', async () => {
    const promise = store.resetPassword('token', 'NuevaClave123');
    const request = http.expectOne(`${environment.apiBaseUrl}/auth/password-reset/confirm`);

    expect(request.request.body).toEqual({ token: 'token', newPassword: 'NuevaClave123' });
    request.flush(null);
    await expect(promise).resolves.toBe(true);
  });

  it('completes an OAuth callback', async () => {
    const promise = store.oauthCallback('google', 'code', 'state');
    const request = http.expectOne(`${environment.apiBaseUrl}/auth/oauth/google/callback`);

    expect(request.request.body).toEqual({ code: 'code', state: 'state' });
    request.flush({ ...AUTH_RESPONSE, needsEmail: false, onboardingToken: null });

    await expect(promise).resolves.toEqual({ needsEmail: false, onboardingToken: null });
    expect(session.isAuthenticated()).toBe(true);
  });

  it('completes the email onboarding for an OAuth account', async () => {
    const promise = store.oauthCompleteEmail('github', 'onboarding', 'user@ferro.dev');
    const request = http.expectOne(`${environment.apiBaseUrl}/auth/oauth/github/complete-email`);

    expect(request.request.body).toEqual({
      onboardingToken: 'onboarding',
      email: 'user@ferro.dev',
    });
    request.flush(AUTH_RESPONSE);

    await expect(promise).resolves.toBe(true);
    expect(session.isAuthenticated()).toBe(true);
  });

  it('updates the profile and refreshes the user', async () => {
    session.setSession(AUTH_RESPONSE);
    const promise = store.updateProfile({ displayName: 'Nuevo', locale: 'en' });

    http.expectOne(`${environment.apiBaseUrl}/users/me`).flush(null);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne(`${environment.apiBaseUrl}/auth/me`).flush({
      user: { ...AUTH_RESPONSE.user, displayName: 'Nuevo', locale: 'en' },
      preferences: {
        locale: 'en',
        unitSystem: 'KG',
        theme: 'DARK',
        timeZone: 'Europe/Madrid',
        defaultRestSeconds: 90,
        timerSound: true,
      },
    });

    await expect(promise).resolves.toBe(true);
    expect(session.user()?.displayName).toBe('Nuevo');
  });

  it('clears errors', () => {
    store.clearError();
    expect(store.error()).toBeNull();
  });
});
