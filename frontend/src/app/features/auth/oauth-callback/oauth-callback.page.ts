import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../core/auth/auth.store';

@Component({
  selector: 'app-oauth-callback-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  template: `
    <section
      class="mx-auto mt-10 w-full max-w-md rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6 text-center"
    >
      @if (needsEmail()) {
        <h1 class="text-2xl font-bold">{{ 'auth.oauthEmailTitle' | translate }}</h1>
        <p class="mt-2 text-sm text-zinc-400">{{ 'auth.oauthEmailHint' | translate }}</p>
        <form class="mt-5 space-y-4 text-left" [formGroup]="form" (ngSubmit)="submitEmail()">
          <label class="block text-sm font-medium text-zinc-300" for="oauthEmail">
            {{ 'auth.email' | translate }}
            <input
              id="oauthEmail"
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
            {{ (auth.loading() ? 'auth.loading' : 'auth.oauthEmailSubmit') | translate }}
          </button>
        </form>
      } @else if (error()) {
        <h1 class="text-2xl font-bold text-red-400">{{ 'auth.oauthErrorTitle' | translate }}</h1>
        <p class="mt-2 text-sm text-zinc-400">{{ 'auth.oauthError' | translate }}</p>
        <a routerLink="/login" class="mt-6 inline-block text-emerald-400">{{
          'nav.login' | translate
        }}</a>
      } @else {
        <p class="text-zinc-300">{{ 'auth.oauthLoading' | translate }}</p>
      }
    </section>
  `,
})
export class OAuthCallbackPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  protected readonly auth = inject(AuthStore);
  protected readonly error = signal(false);
  protected readonly needsEmail = signal(false);
  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });
  private provider = '';
  private onboardingToken = '';

  async ngOnInit(): Promise<void> {
    this.provider = this.route.snapshot.paramMap.get('provider') ?? '';
    const code = this.route.snapshot.queryParamMap.get('code');
    const state = this.route.snapshot.queryParamMap.get('state');
    if (!this.provider || !code || !state) {
      this.error.set(true);
      return;
    }
    const result = await this.auth.oauthCallback(this.provider, code, state);
    if (result.needsEmail && result.onboardingToken) {
      this.onboardingToken = result.onboardingToken;
      this.needsEmail.set(true);
      return;
    }
    if (this.auth.error()) {
      this.error.set(true);
      return;
    }
    await this.router.navigateByUrl('/');
  }

  protected async submitEmail(): Promise<void> {
    if (this.form.invalid || this.auth.loading()) {
      return;
    }
    const success = await this.auth.oauthCompleteEmail(
      this.provider,
      this.onboardingToken,
      this.form.getRawValue().email,
    );
    if (success) {
      await this.router.navigateByUrl('/');
    }
  }
}
