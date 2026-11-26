import { Type, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { AuthStore } from '../../core/auth/auth.store';
import { AuthResponse } from '../../core/auth/auth.models';
import { SessionStore } from '../../core/auth/session.store';
import { ForgotPasswordPage } from './forgot-password/forgot-password.page';
import { HomePage } from '../home/home.page';
import { SettingsPage } from '../settings/settings.page';
import { LoginPage } from './login/login.page';
import { RegisterPage } from './register/register.page';
import { ResetPasswordPage } from './reset-password/reset-password.page';
import { VerifyEmailPage } from './verify-email/verify-email.page';
import { OAuthCallbackPage } from './oauth-callback/oauth-callback.page';

interface FormPage {
  form: FormGroup;
  submit(): Promise<void>;
}

interface VerifyPageAccess {
  state(): string;
}

interface ForgotPageAccess extends FormPage {
  sent(): boolean;
}

interface ResetPageAccess extends FormPage {
  done(): boolean;
}

interface HomePageAccess {
  resend(): Promise<void>;
  resent(): boolean;
}

function fakeAuth() {
  return {
    loading: signal(false),
    error: signal<string | null>(null),
    login: vi.fn().mockResolvedValue(true),
    register: vi.fn().mockResolvedValue(true),
    confirmEmail: vi.fn().mockResolvedValue(true),
    resendVerification: vi.fn().mockResolvedValue(true),
    requestPasswordReset: vi.fn().mockResolvedValue(true),
    resetPassword: vi.fn().mockResolvedValue(true),
    updateProfile: vi.fn().mockResolvedValue(true),
    oauthCallback: vi.fn().mockResolvedValue({ needsEmail: false, onboardingToken: null }),
    oauthCompleteEmail: vi.fn().mockResolvedValue(true),
    logout: vi.fn().mockResolvedValue(undefined),
    logoutAll: vi.fn().mockResolvedValue(undefined),
    clearError: vi.fn(),
  };
}

function configure(component: Type<unknown>, auth: ReturnType<typeof fakeAuth>, route?: unknown) {
  TestBed.configureTestingModule({
    imports: [component],
    providers: [
      provideRouter([]),
      ...provideTranslateService({ lang: 'es', fallbackLang: 'en' }),
      { provide: AuthStore, useValue: auth },
      ...(route ? [{ provide: ActivatedRoute, useValue: route }] : []),
    ],
  });
}

describe('auth pages', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('logs in from the login form', async () => {
    const auth = fakeAuth();
    configure(LoginPage, auth);
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(LoginPage);
    const page = fixture.componentInstance as unknown as FormPage;

    page.form.setValue({ email: 'p1@ferro.dev', password: 'Ferro12345' });
    await page.submit();

    expect(auth.login).toHaveBeenCalledWith({ email: 'p1@ferro.dev', password: 'Ferro12345' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('does not submit an invalid login form', async () => {
    const auth = fakeAuth();
    configure(LoginPage, auth);
    const fixture = TestBed.createComponent(LoginPage);
    const page = fixture.componentInstance as unknown as FormPage;

    await page.submit();

    expect(auth.login).not.toHaveBeenCalled();
    expect(page.form.controls['email'].touched).toBe(true);
  });

  it('renders the server error on the login form', () => {
    const auth = fakeAuth();
    auth.error.set('Credenciales incorrectas');
    configure(LoginPage, auth);
    const fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Credenciales incorrectas');
  });

  it('registers a new account', async () => {
    const auth = fakeAuth();
    configure(RegisterPage, auth);
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(RegisterPage);
    const page = fixture.componentInstance as unknown as FormPage;

    page.form.setValue({
      displayName: 'Ángel',
      email: 'p1@ferro.dev',
      password: 'Ferro12345',
      healthConsent: true,
    });
    await page.submit();

    expect(auth.register).toHaveBeenCalledWith({
      displayName: 'Ángel',
      email: 'p1@ferro.dev',
      password: 'Ferro12345',
      healthConsent: true,
      locale: 'es',
      unitSystem: 'KG',
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('does not register with an invalid form and shows a server error', async () => {
    const auth = fakeAuth();
    auth.error.set('La contraseña es demasiado común');
    configure(RegisterPage, auth);
    const fixture = TestBed.createComponent(RegisterPage);
    fixture.detectChanges();
    const page = fixture.componentInstance as unknown as FormPage;

    await page.submit();
    fixture.detectChanges();

    expect(auth.register).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('La contraseña es demasiado común');
  });

  it('completes the OAuth callback and navigates home', async () => {
    const auth = fakeAuth();
    configure(OAuthCallbackPage, auth, {
      snapshot: {
        paramMap: convertToParamMap({ provider: 'google' }),
        queryParamMap: convertToParamMap({ code: 'code', state: 'state' }),
      },
    });
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(OAuthCallbackPage);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(auth.oauthCallback).toHaveBeenCalledWith('google', 'code', 'state');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('shows a success state after verifying the email', async () => {
    const auth = fakeAuth();
    configure(VerifyEmailPage, auth, {
      snapshot: { queryParamMap: convertToParamMap({ token: 'token' }) },
    });
    const fixture = TestBed.createComponent(VerifyEmailPage);
    const page = fixture.componentInstance as unknown as VerifyPageAccess;

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(auth.confirmEmail).toHaveBeenCalledWith('token');
    expect(page.state()).toBe('success');
    expect(fixture.nativeElement.textContent).toContain('auth.verifySuccessTitle');
  });

  it('shows an error state when the verification token is missing', async () => {
    const auth = fakeAuth();
    configure(VerifyEmailPage, auth, { snapshot: { queryParamMap: convertToParamMap({}) } });
    const fixture = TestBed.createComponent(VerifyEmailPage);
    const page = fixture.componentInstance as unknown as VerifyPageAccess;

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(page.state()).toBe('error');
    expect(fixture.nativeElement.textContent).toContain('auth.verifyErrorTitle');
  });

  it('requests a password reset and shows the generic confirmation', async () => {
    const auth = fakeAuth();
    configure(ForgotPasswordPage, auth);
    const fixture = TestBed.createComponent(ForgotPasswordPage);
    const page = fixture.componentInstance as unknown as ForgotPageAccess;

    page.form.setValue({ email: 'p1@ferro.dev' });
    await page.submit();
    fixture.detectChanges();

    expect(auth.requestPasswordReset).toHaveBeenCalledWith('p1@ferro.dev');
    expect(page.sent()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('auth.forgotSuccess');
  });

  it('shows an error on the forgot password form', () => {
    const auth = fakeAuth();
    auth.error.set('Error de red');
    configure(ForgotPasswordPage, auth);
    const fixture = TestBed.createComponent(ForgotPasswordPage);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Error de red');
  });

  it('resets the password when a token is present', async () => {
    const auth = fakeAuth();
    configure(ResetPasswordPage, auth, {
      snapshot: { queryParamMap: convertToParamMap({ token: 'token' }) },
    });
    const fixture = TestBed.createComponent(ResetPasswordPage);
    const page = fixture.componentInstance as unknown as ResetPageAccess;

    page.form.setValue({ password: 'NuevaClave123', confirmPassword: 'NuevaClave123' });
    await page.submit();
    fixture.detectChanges();

    expect(auth.resetPassword).toHaveBeenCalledWith('token', 'NuevaClave123');
    expect(page.done()).toBe(true);
  });

  it('does not reset when the passwords do not match', async () => {
    const auth = fakeAuth();
    configure(ResetPasswordPage, auth, {
      snapshot: { queryParamMap: convertToParamMap({ token: 'token' }) },
    });
    const fixture = TestBed.createComponent(ResetPasswordPage);
    const page = fixture.componentInstance as unknown as ResetPageAccess;

    page.form.setValue({ password: 'NuevaClave123', confirmPassword: 'OtraClave123' });
    await page.submit();

    expect(auth.resetPassword).not.toHaveBeenCalled();
  });

  it('shows an error when the reset token is missing', () => {
    const auth = fakeAuth();
    configure(ResetPasswordPage, auth, { snapshot: { queryParamMap: convertToParamMap({}) } });
    const fixture = TestBed.createComponent(ResetPasswordPage);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('auth.resetInvalid');
  });

  it('updates the profile from settings', async () => {
    const auth = fakeAuth();
    configure(SettingsPage, auth);
    const session = TestBed.inject(SessionStore);
    session.setSession(AUTH_RESPONSE);
    const fixture = TestBed.createComponent(SettingsPage);
    fixture.detectChanges();
    const page = fixture.componentInstance as unknown as FormPage;

    page.form.setValue({
      displayName: 'Nuevo Nombre',
      locale: 'en',
      unitSystem: 'LB',
      theme: 'LIGHT',
      defaultRestSeconds: 120,
    });
    await page.submit();

    expect(auth.updateProfile).toHaveBeenCalledWith({
      displayName: 'Nuevo Nombre',
      locale: 'en',
      unitSystem: 'LB',
      theme: 'LIGHT',
      defaultRestSeconds: 120,
    });
  });

  it('renders the authenticated home and resends verification', async () => {
    const auth = fakeAuth();
    configure(HomePage, auth);
    const session = TestBed.inject(SessionStore);
    session.setSession(AUTH_RESPONSE);
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    const page = fixture.componentInstance as unknown as HomePageAccess;

    expect(fixture.nativeElement.textContent).toContain('p1@ferro.dev');
    await page.resend();

    expect(auth.resendVerification).toHaveBeenCalled();
    expect(page.resent()).toBe(true);
  });

  it('hides the verification banner for active users and logs out', async () => {
    const auth = fakeAuth();
    configure(HomePage, auth);
    const session = TestBed.inject(SessionStore);
    session.setSession({ ...AUTH_RESPONSE, user: { ...AUTH_RESPONSE.user, status: 'ACTIVE' } });
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    const page = fixture.componentInstance as unknown as {
      logout(): Promise<void>;
      logoutAll(): Promise<void>;
    };

    expect(fixture.nativeElement.textContent).not.toContain('auth.unverified');
    await page.logout();
    await page.logoutAll();

    expect(auth.logout).toHaveBeenCalled();
    expect(auth.logoutAll).toHaveBeenCalled();
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
