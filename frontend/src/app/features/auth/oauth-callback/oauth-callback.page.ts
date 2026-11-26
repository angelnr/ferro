import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../core/auth/auth.store';

@Component({
  selector: 'app-oauth-callback-page',
  imports: [RouterLink, TranslatePipe],
  template: `
    <section
      class="mx-auto mt-10 w-full max-w-md rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6 text-center"
    >
      @if (error()) {
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
  private readonly auth = inject(AuthStore);
  protected readonly error = signal(false);

  async ngOnInit(): Promise<void> {
    const provider = this.route.snapshot.paramMap.get('provider');
    const code = this.route.snapshot.queryParamMap.get('code');
    const state = this.route.snapshot.queryParamMap.get('state');
    if (!provider || !code || !state) {
      this.error.set(true);
      return;
    }
    const success = await this.auth.oauthCallback(provider, code, state);
    if (success) {
      await this.router.navigateByUrl('/');
    } else {
      this.error.set(true);
    }
  }
}
