import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { RoutineApi } from '../training/routine.api';
import { RoutineSummary } from '../training/routine.models';

@Component({
  selector: 'app-routines-page',
  imports: [RouterLink, TranslatePipe],
  template: `
    <section class="mx-auto mt-8 max-w-4xl space-y-6">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p class="text-sm uppercase tracking-wider text-emerald-400">
            {{ 'routines.title' | translate }}
          </p>
          <h1 class="mt-1 text-3xl font-black">{{ 'routines.list' | translate }}</h1>
        </div>
        <a
          routerLink="/routines/new"
          class="rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white hover:bg-emerald-500"
        >
          {{ 'routines.new' | translate }}
        </a>
      </div>

      @if (error()) {
        <p class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
          {{ 'routines.error' | translate }}
        </p>
      }
      @if (startError(); as message) {
        <p
          class="rounded-lg border border-amber-900 bg-amber-950/40 px-3 py-2 text-sm text-amber-200"
        >
          {{ message }}
        </p>
      }

      @if (!loading() && !routines().length) {
        <p class="rounded-xl border border-dashed border-zinc-800 p-8 text-sm text-zinc-500">
          {{ 'routines.empty' | translate }}
        </p>
      }

      <div class="space-y-3">
        @for (routine of routines(); track routine.id) {
          <article class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 class="text-lg font-semibold">{{ routine.name }}</h2>
                <p class="text-sm text-zinc-500">
                  {{ routine.exerciseCount }} {{ 'routines.exercises' | translate }}
                  @if (routine.lastUsedAt) {
                    · {{ 'routines.lastUsed' | translate }} {{ routine.lastUsedAt.slice(0, 10) }}
                  }
                </p>
              </div>
              <div class="flex flex-wrap gap-2">
                <button
                  type="button"
                  (click)="start(routine)"
                  class="rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-emerald-500"
                >
                  {{ 'routines.start' | translate }}
                </button>
                <a
                  [routerLink]="['/routines', routine.id]"
                  class="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm"
                >
                  {{ 'routines.edit' | translate }}
                </a>
                <button
                  type="button"
                  (click)="duplicate(routine)"
                  class="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm"
                >
                  {{ 'routines.duplicate' | translate }}
                </button>
                <button
                  type="button"
                  (click)="remove(routine)"
                  class="rounded-lg border border-red-900 px-3 py-1.5 text-sm text-red-300"
                >
                  {{ 'routines.delete' | translate }}
                </button>
              </div>
            </div>
          </article>
        }
      </div>
    </section>
  `,
})
export class RoutinesPage implements OnInit {
  private readonly api = inject(RoutineApi);
  private readonly router = inject(Router);
  protected readonly routines = signal<RoutineSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly startError = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    await this.load();
  }

  protected async start(routine: RoutineSummary): Promise<void> {
    this.startError.set(null);
    try {
      const session = await firstValueFrom(this.api.start(routine.id));
      await this.router.navigate(['/training', session.id]);
    } catch (error) {
      const activeId =
        error instanceof HttpErrorResponse
          ? (error.error as { activeSessionId?: string } | null)?.activeSessionId
          : undefined;
      if (activeId) {
        await this.router.navigate(['/training', activeId]);
      } else {
        this.startError.set('No se pudo iniciar la rutina');
      }
    }
  }

  protected async duplicate(routine: RoutineSummary): Promise<void> {
    try {
      await firstValueFrom(this.api.duplicate(routine.id));
      await this.load();
    } catch {
      this.error.set(true);
    }
  }

  protected async remove(routine: RoutineSummary): Promise<void> {
    if (!window.confirm('¿Eliminar esta rutina?')) {
      return;
    }
    try {
      await firstValueFrom(this.api.delete(routine.id));
      await this.load();
    } catch {
      this.error.set(true);
    }
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      const page = await firstValueFrom(this.api.list());
      this.routines.set(page.content);
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }
}
