import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../core/auth/auth.store';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  template: `
    <section
      class="mx-auto mt-10 w-full max-w-md rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6 shadow-xl"
    >
      <h1 class="text-2xl font-bold">{{ 'auth.loginTitle' | translate }}</h1>
      <p class="mt-1 text-sm text-zinc-400">{{ 'auth.loginSubtitle' | translate }}</p>

      <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
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
            autocomplete="current-password"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-emerald-500"
          />
        </label>
        @if (form.controls.password.touched && form.controls.password.invalid) {
          <p class="text-sm text-red-400">{{ 'auth.passwordRequired' | translate }}</p>
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
          {{ (auth.loading() ? 'auth.loading' : 'auth.submitLogin') | translate }}
        </button>
      </form>

      <div class="mt-5 flex items-center justify-between text-sm text-zinc-400">
        <a routerLink="/register" class="hover:text-emerald-400">{{
          'auth.noAccount' | translate
        }}</a>
        <a routerLink="/forgot-password" class="hover:text-emerald-400">{{
          'auth.forgot' | translate
        }}</a>
      </div>
    </section>
  `,
})
export class LoginPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthStore);

  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.auth.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const success = await this.auth.login(this.form.getRawValue());
    if (success) {
      await this.router.navigateByUrl('/');
    }
  }
}
