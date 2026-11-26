import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../core/auth/auth.store';

@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  template: `
    <section
      class="mx-auto mt-10 w-full max-w-md rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6 shadow-xl"
    >
      <h1 class="text-2xl font-bold">{{ 'auth.forgotTitle' | translate }}</h1>
      <p class="mt-1 text-sm text-zinc-400">{{ 'auth.forgotSubtitle' | translate }}</p>

      @if (sent()) {
        <p
          class="mt-6 rounded-lg border border-emerald-900 bg-emerald-950/40 px-3 py-2 text-sm text-emerald-300"
        >
          {{ 'auth.forgotSuccess' | translate }}
        </p>
      } @else {
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
            {{ (auth.loading() ? 'auth.loading' : 'auth.sendReset') | translate }}
          </button>
        </form>
      }

      <a routerLink="/login" class="mt-5 inline-block text-sm text-emerald-400">{{
        'nav.login' | translate
      }}</a>
    </section>
  `,
})
export class ForgotPasswordPage {
  private readonly formBuilder = inject(FormBuilder);
  protected readonly auth = inject(AuthStore);
  protected readonly sent = signal(false);
  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.auth.loading()) {
      return;
    }
    await this.auth.requestPasswordReset(this.form.getRawValue().email);
    this.sent.set(true);
  }
}
