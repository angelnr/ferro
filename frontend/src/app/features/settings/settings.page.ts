import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../core/auth/auth.store';
import { SessionStore } from '../../core/auth/session.store';
import { UiStore } from '../../core/store/ui.store';

@Component({
  selector: 'app-settings-page',
  imports: [ReactiveFormsModule, TranslatePipe],
  template: `
    @if (session.user(); as user) {
      <section class="mx-auto mt-8 w-full max-w-2xl">
        <h1 class="text-3xl font-black">{{ 'settings.title' | translate }}</h1>
        <p class="mt-1 text-sm text-zinc-400">{{ 'settings.subtitle' | translate }}</p>

        <form
          class="mt-6 grid gap-5 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6 sm:grid-cols-2"
          [formGroup]="form"
          (ngSubmit)="submit()"
        >
          <label class="block text-sm font-medium text-zinc-300 sm:col-span-2" for="displayName">
            {{ 'auth.displayName' | translate }}
            <input
              id="displayName"
              type="text"
              formControlName="displayName"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-emerald-500"
            />
          </label>

          <label class="block text-sm font-medium text-zinc-300" for="locale">
            {{ 'settings.locale' | translate }}
            <select
              id="locale"
              formControlName="locale"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100"
            >
              <option value="es">Español</option>
              <option value="en">English</option>
            </select>
          </label>

          <label class="block text-sm font-medium text-zinc-300" for="unitSystem">
            {{ 'settings.units' | translate }}
            <select
              id="unitSystem"
              formControlName="unitSystem"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100"
            >
              <option value="KG">kg</option>
              <option value="LB">lb</option>
            </select>
          </label>

          <label class="block text-sm font-medium text-zinc-300" for="theme">
            {{ 'settings.theme' | translate }}
            <select
              id="theme"
              formControlName="theme"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100"
            >
              <option value="DARK">{{ 'settings.themeDark' | translate }}</option>
              <option value="LIGHT">{{ 'settings.themeLight' | translate }}</option>
            </select>
          </label>

          <label class="block text-sm font-medium text-zinc-300" for="defaultRestSeconds">
            {{ 'settings.rest' | translate }}
            <input
              id="defaultRestSeconds"
              type="number"
              min="15"
              max="600"
              formControlName="defaultRestSeconds"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-emerald-500"
            />
          </label>

          <p class="text-xs text-zinc-500 sm:col-span-2">
            {{ 'settings.timeZone' | translate }}: {{ user.timeZone }}
          </p>

          @if (saved()) {
            <p
              class="rounded-lg border border-emerald-900 bg-emerald-950/40 px-3 py-2 text-sm text-emerald-300 sm:col-span-2"
            >
              {{ 'settings.saved' | translate }}
            </p>
          }
          @if (auth.error(); as error) {
            <p
              class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300 sm:col-span-2"
            >
              {{ error }}
            </p>
          }

          <button
            type="submit"
            [disabled]="form.invalid || auth.loading()"
            class="rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white disabled:opacity-50 sm:col-span-2"
          >
            {{ (auth.loading() ? 'auth.loading' : 'settings.save') | translate }}
          </button>
        </form>
      </section>
    }
  `,
})
export class SettingsPage {
  private readonly formBuilder = inject(FormBuilder);
  protected readonly session = inject(SessionStore);
  protected readonly auth = inject(AuthStore);
  private readonly ui = inject(UiStore);
  protected readonly saved = signal(false);

  protected readonly form = this.formBuilder.nonNullable.group({
    displayName: ['', [Validators.required, Validators.maxLength(80)]],
    locale: ['es' as 'es' | 'en'],
    unitSystem: ['KG' as 'KG' | 'LB'],
    theme: ['DARK' as 'DARK' | 'LIGHT'],
    defaultRestSeconds: [90, [Validators.min(15), Validators.max(600)]],
  });

  constructor() {
    effect(() => {
      const user = this.session.user();
      if (user) {
        this.form.patchValue(
          {
            displayName: user.displayName,
            locale: user.locale,
            unitSystem: user.unitSystem,
            theme: user.theme,
            defaultRestSeconds: user.defaultRestSeconds,
          },
          { emitEvent: false },
        );
      }
    });
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.auth.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const success = await this.auth.updateProfile(value);
    if (success) {
      this.saved.set(true);
      this.ui.setLocale(value.locale);
      this.ui.setTheme(value.theme === 'DARK' ? 'dark' : 'light');
    }
  }
}
