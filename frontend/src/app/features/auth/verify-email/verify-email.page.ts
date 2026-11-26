import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../core/auth/auth.store';

type VerificationState = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-verify-email-page',
  imports: [RouterLink, TranslatePipe],
  template: `
    <section
      class="mx-auto mt-10 w-full max-w-md rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6 text-center shadow-xl"
    >
      @switch (state()) {
        @case ('loading') {
          <p class="text-zinc-300">{{ 'auth.verifying' | translate }}</p>
        }
        @case ('success') {
          <h1 class="text-2xl font-bold text-emerald-400">
            {{ 'auth.verifySuccessTitle' | translate }}
          </h1>
          <p class="mt-2 text-sm text-zinc-400">{{ 'auth.verifySuccess' | translate }}</p>
          <a
            routerLink="/login"
            class="mt-6 inline-block rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white"
          >
            {{ 'nav.login' | translate }}
          </a>
        }
        @default {
          <h1 class="text-2xl font-bold text-red-400">{{ 'auth.verifyErrorTitle' | translate }}</h1>
          <p class="mt-2 text-sm text-zinc-400">{{ 'auth.verifyError' | translate }}</p>
          <a routerLink="/login" class="mt-6 inline-block text-emerald-400">{{
            'nav.login' | translate
          }}</a>
        }
      }
    </section>
  `,
})
export class VerifyEmailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthStore);
  protected readonly state = signal<VerificationState>('loading');

  async ngOnInit(): Promise<void> {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state.set('error');
      return;
    }
    const success = await this.auth.confirmEmail(token);
    this.state.set(success ? 'success' : 'error');
  }
}
