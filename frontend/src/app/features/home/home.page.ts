import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../core/auth/auth.store';
import { SessionStore } from '../../core/auth/session.store';
import { TrainingApi } from '../training/training.api';

@Component({
  selector: 'app-home-page',
  imports: [TranslatePipe],
  template: `
    @if (session.user(); as user) {
      <section class="mx-auto mt-10 w-full max-w-3xl space-y-6">
        <div>
          <p class="text-sm uppercase tracking-wider text-emerald-400">
            {{ 'home.dashboard' | translate }}
          </p>
          <h1 class="mt-1 text-3xl font-black">
            {{ 'home.hello' | translate }}, {{ user.displayName }}
          </h1>
          <p class="mt-1 text-zinc-400">{{ user.email }}</p>
        </div>

        <div class="rounded-xl border border-emerald-900 bg-emerald-950/30 p-4">
          <p class="text-sm text-zinc-300">{{ 'training.startHint' | translate }}</p>
          <button
            type="button"
            (click)="startTraining()"
            class="mt-3 rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white hover:bg-emerald-500"
          >
            {{ 'training.start' | translate }}
          </button>
          @if (startError(); as message) {
            <p class="mt-2 text-sm text-red-300">{{ message }}</p>
          }
        </div>

        @if (user.status === 'UNVERIFIED') {
          <div class="rounded-xl border border-amber-900 bg-amber-950/40 p-4">
            <p class="text-sm text-amber-200">{{ 'auth.unverified' | translate }}</p>
            <button
              type="button"
              (click)="resend()"
              [disabled]="auth.loading()"
              class="mt-3 rounded-lg bg-amber-500 px-3 py-1.5 text-sm font-semibold text-zinc-950 disabled:opacity-50"
            >
              {{ (resent() ? 'auth.resendSent' : 'auth.resend') | translate }}
            </button>
          </div>
        }

        <div class="grid gap-4 sm:grid-cols-2">
          <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <p class="text-xs uppercase tracking-wider text-zinc-500">
              {{ 'home.account' | translate }}
            </p>
            <dl class="mt-3 space-y-2 text-sm">
              <div class="flex justify-between gap-4">
                <dt class="text-zinc-500">{{ 'home.status' | translate }}</dt>
                <dd class="font-medium">{{ user.status }}</dd>
              </div>
              <div class="flex justify-between gap-4">
                <dt class="text-zinc-500">{{ 'home.locale' | translate }}</dt>
                <dd class="font-medium">{{ user.locale }}</dd>
              </div>
              <div class="flex justify-between gap-4">
                <dt class="text-zinc-500">{{ 'home.units' | translate }}</dt>
                <dd class="font-medium">{{ user.unitSystem }}</dd>
              </div>
              <div class="flex justify-between gap-4">
                <dt class="text-zinc-500">{{ 'home.rest' | translate }}</dt>
                <dd class="font-medium">{{ user.defaultRestSeconds }} s</dd>
              </div>
            </dl>
          </div>

          <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <p class="text-xs uppercase tracking-wider text-zinc-500">
              {{ 'home.session' | translate }}
            </p>
            <p class="mt-3 text-sm text-zinc-400">{{ 'home.sessionHint' | translate }}</p>
            <div class="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                (click)="logout()"
                class="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm hover:border-zinc-500"
              >
                {{ 'nav.logout' | translate }}
              </button>
              <button
                type="button"
                (click)="logoutAll()"
                class="rounded-lg border border-red-900 px-3 py-1.5 text-sm text-red-300 hover:border-red-700"
              >
                {{ 'home.logoutAll' | translate }}
              </button>
            </div>
          </div>
        </div>

        <p class="text-sm text-zinc-500">{{ 'home.comingSoon' | translate }}</p>
      </section>
    }
  `,
})
export class HomePage {
  protected readonly session = inject(SessionStore);
  protected readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly trainingApi = inject(TrainingApi);
  protected readonly resent = signal(false);
  protected readonly startError = signal<string | null>(null);

  protected async startTraining(): Promise<void> {
    this.startError.set(null);
    try {
      const session = await firstValueFrom(this.trainingApi.start());
      await this.router.navigate(['/training', session.id]);
    } catch (error) {
      const activeId =
        error instanceof HttpErrorResponse
          ? (error.error as { activeSessionId?: string } | null)?.activeSessionId
          : undefined;
      if (activeId) {
        await this.router.navigate(['/training', activeId]);
      } else {
        this.startError.set('No se pudo iniciar la sesión');
      }
    }
  }

  protected async resend(): Promise<void> {
    this.resent.set(await this.auth.resendVerification());
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }

  protected async logoutAll(): Promise<void> {
    await this.auth.logoutAll();
    await this.router.navigateByUrl('/login');
  }
}
