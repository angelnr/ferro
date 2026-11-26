import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

export type Theme = 'dark' | 'light';
export type Locale = 'es' | 'en';

interface UiState {
  theme: Theme;
  locale: Locale;
}

const initialState: UiState = {
  theme: 'dark',
  locale: 'es',
};

export const UiStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ theme }) => ({
    isDark: computed(() => theme() === 'dark'),
  })),
  withMethods((store) => ({
    setTheme(theme: Theme): void {
      patchState(store, { theme });
    },
    toggleTheme(): void {
      patchState(store, { theme: store.theme() === 'dark' ? 'light' : 'dark' });
    },
    setLocale(locale: Locale): void {
      patchState(store, { locale });
    },
  })),
);
