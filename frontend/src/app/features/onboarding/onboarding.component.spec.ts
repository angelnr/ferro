import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { SessionStore } from '../../core/auth/session.store';
import { User } from '../../core/auth/auth.models';
import { OnboardingComponent } from './onboarding.component';

const USER = {
  id: 'id',
  displayName: 'Angel',
  email: 'a@ferro.dev',
  status: 'ACTIVE',
  locale: 'es',
  unitSystem: 'KG',
  theme: 'DARK',
  timeZone: 'Europe/Madrid',
  defaultRestSeconds: 90,
} as User;

describe('OnboardingComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [OnboardingComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ...provideTranslateService({ lang: 'es' }),
      ],
    });
    localStorage.removeItem('ferro.onboarding.seen');
  });

  afterEach(() => {
    localStorage.removeItem('ferro.onboarding.seen');
    TestBed.resetTestingModule();
  });

  it('walks through the cards and persists the skip', () => {
    const session = TestBed.inject(SessionStore);
    session.setUser(USER);
    const fixture = TestBed.createComponent(OnboardingComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      visible: () => boolean;
      step: () => number;
      card(): { title: string };
      next(): void;
      finish(): void;
    };
    expect(component.visible()).toBe(true);
    expect(component.card().title).toContain('onboarding.step1');
    component.next();
    component.next();
    expect(component.step()).toBe(2);
    component.next();
    component.finish();
    expect(component.visible()).toBe(false);
    expect(localStorage.getItem('ferro.onboarding.seen')).toBe('1');
  });

  it('stays hidden when there is no authenticated user', () => {
    const fixture = TestBed.createComponent(OnboardingComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as { visible: () => boolean };
    expect(component.visible()).toBe(false);
  });
});
