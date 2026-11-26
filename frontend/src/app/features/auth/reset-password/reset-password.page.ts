import { Component, inject, signal } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../core/auth/auth.store';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmation = control.get('confirmPassword')?.value;
  return password === confirmation ? null : { passwordsMismatch: true };
}

@Component({
  selector: 'app-reset-password-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  template: `
    <section
      class="mx-auto mt-10 w-full max-w-md rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6 shadow-xl"
    >
      <h1 class="text-2xl font-bold">{{ 'auth.resetTitle' | translate }}</h1>

      @if (done()) {
        <p
          class="mt-6 rounded-lg border border-emerald-900 bg-emerald-950/40 px-3 py-2 text-sm text-emerald-300"
        >
          {{ 'auth.resetSuccess' | translate }}
        </p>
        <a routerLink="/login" class="mt-5 inline-block text-sm text-emerald-400">{{
          'nav.login' | translate
        }}</a>
      } @else if (!token()) {
        <p
          class="mt-6 rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300"
        >
          {{ 'auth.resetInvalid' | translate }}
        </p>
        <a routerLink="/forgot-password" class="mt-5 inline-block text-sm text-emerald-400">
          {{ 'auth.forgot' | translate }}
        </a>
      } @else {
        <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
          <label class="block text-sm font-medium text-zinc-300" for="password">
            {{ 'auth.newPassword' | translate }}
            <input
              id="password"
              type="password"
              formControlName="password"
              autocomplete="new-password"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-emerald-500"
            />
          </label>
          <label class="block text-sm font-medium text-zinc-300" for="confirmPassword">
            {{ 'auth.confirmPassword' | translate }}
            <input
              id="confirmPassword"
              type="password"
              formControlName="confirmPassword"
              autocomplete="new-password"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-emerald-500"
            />
          </label>
          @if (form.hasError('passwordsMismatch') && form.touched) {
            <p class="text-sm text-red-400">{{ 'auth.passwordsMismatch' | translate }}</p>
          }
          @if (auth.error(); as error) {
            <p
              class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300"
            >
              {{ error }}
            </p>
          }
          <button
            type="submit"
            [disabled]="form.invalid || auth.loading()"
            class="w-full rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white disabled:opacity-50"
          >
            {{ (auth.loading() ? 'auth.loading' : 'auth.resetSubmit') | translate }}
          </button>
        </form>
      }
    </section>
  `,
})
export class ResetPasswordPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  protected readonly auth = inject(AuthStore);
  protected readonly done = signal(false);
  protected readonly token = signal(this.route.snapshot.queryParamMap.get('token'));

  protected readonly form = this.formBuilder.nonNullable.group(
    {
      password: ['', [Validators.required, Validators.minLength(10)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.auth.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const token = this.token();
    if (!token) {
      return;
    }
    const value = this.form.getRawValue();
    const success = await this.auth.resetPassword(token, value.password);
    this.done.set(success);
  }
}
