import { TestBed } from '@angular/core/testing';
import { UiStore } from './ui.store';

describe('UiStore', () => {
  let store: InstanceType<typeof UiStore>;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    store = TestBed.inject(UiStore);
  });

  it('starts in dark mode with Spanish locale', () => {
    expect(store.theme()).toBe('dark');
    expect(store.locale()).toBe('es');
    expect(store.isDark()).toBe(true);
  });

  it('toggles the theme', () => {
    store.toggleTheme();

    expect(store.theme()).toBe('light');
    expect(store.isDark()).toBe(false);
  });

  it('changes the locale', () => {
    store.setLocale('en');

    expect(store.locale()).toBe('en');
  });
});
