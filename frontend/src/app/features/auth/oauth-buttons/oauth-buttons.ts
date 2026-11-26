import { Component, OnInit, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { AuthApi } from '../../../core/auth/auth.api';

type Providers = Record<string, boolean>;

@Component({
  selector: 'app-oauth-buttons',
  imports: [TranslatePipe],
  template: `
    @if (providers(); as enabled) {
      @if (enabled['google'] || enabled['github']) {
        <div class="mt-6 space-y-3">
          <div class="flex items-center gap-3 text-xs uppercase tracking-wider text-zinc-500">
            <span class="h-px flex-1 bg-zinc-800"></span>
            <span>{{ 'auth.or' | translate }}</span>
            <span class="h-px flex-1 bg-zinc-800"></span>
          </div>

          @if (enabled['google']) {
            <button
              type="button"
              (click)="continueWith('google')"
              class="w-full rounded-lg border border-zinc-700 px-4 py-2 text-sm font-medium hover:border-zinc-500"
            >
              {{ 'auth.continueGoogle' | translate }}
            </button>
          }
          @if (enabled['github']) {
            <button
              type="button"
              (click)="continueWith('github')"
              class="w-full rounded-lg border border-zinc-700 px-4 py-2 text-sm font-medium hover:border-zinc-500"
            >
              {{ 'auth.continueGithub' | translate }}
            </button>
          }
        </div>
      }
    }
  `,
})
export class OAuthButtons implements OnInit {
  private readonly api = inject(AuthApi);
  protected readonly providers = signal<Providers | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      this.providers.set(await firstValueFrom(this.api.oauthProviders()));
    } catch {
      this.providers.set({ google: false, github: false });
    }
  }

  protected async continueWith(provider: string): Promise<void> {
    const response = await firstValueFrom(this.api.oauthAuthorizeUrl(provider));
    window.location.assign(response.url);
  }
}
