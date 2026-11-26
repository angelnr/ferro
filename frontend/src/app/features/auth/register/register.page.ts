import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../core/auth/auth.store';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  template: `
    <section
      class="mx-auto mt-10 w-full max-w-md rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6 shadow-xl"
    >
      <h1 class="text-2xl font-bold">{{ 'auth.registerTitle' | translate }}</h1>
      <p class="mt-1 text-sm text-zinc-400">{{ 'auth.registerSubtitle' | translate }}</p>

      <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
        <label class="block text-sm font-medium text-zinc-300" for="displayName">
          {{ 'auth.displayName' | translate }}
          <input
            id="displayName"
            type="text"
            formControlName="displayName"
            autocomplete="nickname"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-emerald-500"
          />
        </label>
        @if (form.controls.displayName.touched && form.controls.displayName.invalid) {
          <p class="text-sm text-red-400">{{ 'auth.displayNameInvalid' | translate }}</p>
        }

        <label class="block text-sm font-medium text-zinc-300" for="email">
          {{ 'auth.email' | translate }}
          <input
            id="email"
            type="email"
            formControlName="email"
            autocomplete="email"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-emerald-500"
          />
        </label>
        @if (form.controls.email.touched && form.controls.email.invalid) {
          <p class="text-sm text-red-400">{{ 'auth.emailInvalid' | translate }}</p>
        }

        <label class="block text-sm font-medium text-zinc-300" for="password">
          {{ 'auth.password' | translate }}
          <input
            id="password"
            type="password"
            formControlName="password"
            autocomplete="new-password"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-emerald-500"
          />
        </label>
        <p class="text-xs text-zinc-500">{{ 'auth.passwordHint' | translate }}</p>
        @if (form.controls.password.touched && form.controls.password.invalid) {
          <p class="text-sm text-red-400">{{ 'auth.passwordInvalid' | translate }}</p>
        }

        <label class="flex items-start gap-3 text-sm text-zinc-300">
          <input type="checkbox" formControlName="healthConsent" class="mt-1 accent-emerald-500" />
          <span>{{ 'auth.consent' | translate }}</span>
        </label>
        @if (form.controls.healthConsent.touched && form.controls.healthConsent.invalid) {
          <p class="text-sm text-red-400">{{ 'auth.consentRequired' | translate }}</p>
        }

        @if (auth.error(); as error) {
          <p class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
            {{ error }}
          </p>
        }

        <button
          type="submit"
          [disabled]="form.invalid || auth.loading()"
          class="w-full rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white transition hover:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {{ (auth.loading() ? 'auth.loading' : 'auth.submitRegister') | translate }}
        </button>
      </form>

      <p class="mt-5 text-sm text-zinc-400">
        {{ 'auth.haveAccount' | translate }}
        <a routerLink="/login" class="text-emerald-400 hover:text-emerald-300">{{
          'nav.login' | translate
        }}</a>
      </p>
    </section>
  `,
})
export class RegisterPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthStore);

  protected readonly form = this.formBuilder.nonNullable.group({
    displayName: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(10)]],
    healthConsent: [false, [Validators.requiredTrue]],
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.auth.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const success = await this.auth.register({
      displayName: value.displayName,
      email: value.email,
      password: value.password,
      healthConsent: value.healthConsent,
      locale: 'es',
      unitSystem: 'KG',
    });
    if (success) {
      await this.router.navigateByUrl('/');
    }
  }
}
