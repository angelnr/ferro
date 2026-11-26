import { TestBed } from '@angular/core/testing';
import { AuthResponse } from './auth.models';
import { SessionStore } from './session.store';

const RESPONSE: AuthResponse = {
  accessToken: 'token',
  user: {
    id: 'id',
    displayName: 'Ángel',
    email: 'a@ferro.dev',
    status: 'ACTIVE',
    locale: 'es',
    unitSystem: 'KG',
    theme: 'DARK',
    timeZone: 'Europe/Madrid',
    defaultRestSeconds: 90,
  },
};

describe('SessionStore', () => {
  let store: SessionStore;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    store = TestBed.inject(SessionStore);
  });

  afterEach(() => TestBed.resetTestingModule());

  it('starts anonymous and uninitialized', () => {
    expect(store.isAuthenticated()).toBe(false);
    expect(store.user()).toBeNull();
    expect(store.initialized()).toBe(false);
  });

  it('sets and clears the session', () => {
    store.setSession(RESPONSE);

    expect(store.isAuthenticated()).toBe(true);
    expect(store.user()?.email).toBe('a@ferro.dev');

    store.clear();

    expect(store.isAuthenticated()).toBe(false);
    expect(store.user()).toBeNull();
  });
});
