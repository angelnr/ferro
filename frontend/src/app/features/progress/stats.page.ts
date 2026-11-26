import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { ProgressApi } from './progress.api';
import { ProgressSummary } from './progress.models';

@Component({
  selector: 'app-stats-page',
  imports: [DatePipe, DecimalPipe, RouterLink, TranslatePipe],
  template: `
    <section class="mx-auto mt-8 max-w-5xl space-y-6">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p class="text-sm uppercase tracking-wider text-emerald-400">
            {{ 'progress.title' | translate }}
          </p>
          <h1 class="mt-1 text-3xl font-black">{{ 'progress.stats' | translate }}</h1>
        </div>
        <a routerLink="/history" class="text-sm text-emerald-400">{{
          'progress.backHistory' | translate
        }}</a>
      </div>

      <div class="flex gap-2">
        @for (option of ranges; track option) {
          <button
            type="button"
            (click)="load(option)"
            [class.bg-emerald-600]="weeks() === option"
            [class.text-white]="weeks() === option"
            class="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm"
          >
            {{ option }} {{ 'progress.weeks' | translate }}
          </button>
        }
      </div>

      @if (error()) {
        <p class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
          {{ 'progress.error' | translate }}
        </p>
      }

      @if (summary(); as data) {
        <div class="grid gap-4 sm:grid-cols-3">
          <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <p class="text-xs uppercase tracking-wider text-zinc-500">
              {{ 'progress.volume' | translate }}
            </p>
            <p class="mt-2 text-2xl font-black">{{ data.current.volume | number }} kg</p>
            <p
              class="text-sm"
              [class.text-emerald-400]="delta(data.current.volume, data.previous.volume) >= 0"
              [class.text-red-400]="delta(data.current.volume, data.previous.volume) < 0"
            >
              {{ delta(data.current.volume, data.previous.volume) | number: '1.0-1' }} %
            </p>
          </div>
          <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <p class="text-xs uppercase tracking-wider text-zinc-500">
              {{ 'progress.sessions' | translate }}
            </p>
            <p class="mt-2 text-2xl font-black">{{ data.current.sessions }}</p>
            <p class="text-sm text-zinc-500">
              {{ data.current.sets }} {{ 'progress.sets' | translate }}
            </p>
          </div>
          <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <p class="text-xs uppercase tracking-wider text-zinc-500">
              {{ 'progress.streak' | translate }}
            </p>
            <p class="mt-2 text-4xl font-black">{{ data.currentStreak }}</p>
            <p class="text-sm text-zinc-500">
              {{ 'progress.bestStreak' | translate }}: {{ data.bestStreak }}
            </p>
          </div>
        </div>

        <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
          <p class="text-xs uppercase tracking-wider text-zinc-500">
            {{ 'progress.weeklyVolume' | translate }}
          </p>
          <div class="mt-3 flex h-44 items-end gap-1">
            @for (point of data.weeklyVolume; track point.weekStart) {
              <div class="flex min-w-0 flex-1 flex-col items-center gap-1">
                <div
                  class="w-full rounded-t bg-emerald-600"
                  [style.height.%]="barHeight(point.value, maxVolume())"
                  [attr.title]="point.value + ' kg · ' + (point.weekStart | date: 'shortDate')"
                ></div>
                <span class="text-[10px] text-zinc-600">{{ point.weekStart | date: 'dd/MM' }}</span>
              </div>
            }
          </div>
        </div>

        <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
          <p class="text-xs uppercase tracking-wider text-zinc-500">
            {{ 'progress.weeklySessions' | translate }}
          </p>
          <div class="mt-3 flex h-32 items-end gap-1">
            @for (point of data.weeklySessions; track point.weekStart) {
              <div class="flex min-w-0 flex-1 flex-col items-center gap-1">
                <div
                  class="w-full rounded-t bg-sky-500"
                  [style.height.%]="barHeight(point.value, maxSessions())"
                  [attr.title]="point.value + ' ' + ('progress.sessions' | translate)"
                ></div>
                <span class="text-[10px] text-zinc-600">{{ point.weekStart | date: 'dd/MM' }}</span>
              </div>
            }
          </div>
        </div>

        <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
          <p class="text-xs uppercase tracking-wider text-zinc-500">
            {{ 'progress.muscleDistribution' | translate }}
          </p>
          <div class="mt-3 space-y-2">
            @for (slice of data.muscleDistribution; track slice.muscleGroup) {
              <div>
                <div class="flex justify-between text-xs text-zinc-400">
                  <span>{{ slice.muscleGroup }}</span>
                  <span>{{ slice.volume | number }} kg</span>
                </div>
                <div class="mt-1 h-2 rounded bg-zinc-800">
                  <div
                    class="h-2 rounded bg-amber-500"
                    [style.width.%]="barHeight(slice.volume, maxMuscle())"
                  ></div>
                </div>
              </div>
            }
          </div>
        </div>
      } @else if (!error()) {
        <p class="text-zinc-400">{{ 'auth.loading' | translate }}</p>
      }
    </section>
  `,
})
export class StatsPage implements OnInit {
  private readonly api = inject(ProgressApi);
  protected readonly summary = signal<ProgressSummary | null>(null);
  protected readonly weeks = signal(12);
  protected readonly error = signal(false);
  protected readonly ranges = [4, 12, 24];

  async ngOnInit(): Promise<void> {
    await this.load(12);
  }

  protected async load(weeks: number): Promise<void> {
    this.weeks.set(weeks);
    this.error.set(false);
    try {
      this.summary.set(await firstValueFrom(this.api.summary(weeks)));
    } catch {
      this.error.set(true);
    }
  }

  protected delta(current: number, previous: number): number {
    if (!previous) {
      return current ? 100 : 0;
    }
    return ((current - previous) / previous) * 100;
  }

  protected maxVolume(): number {
    return Math.max(1, ...(this.summary()?.weeklyVolume.map((point) => point.value) ?? [0]));
  }

  protected maxSessions(): number {
    return Math.max(1, ...(this.summary()?.weeklySessions.map((point) => point.value) ?? [0]));
  }

  protected maxMuscle(): number {
    return Math.max(1, ...(this.summary()?.muscleDistribution.map((slice) => slice.volume) ?? [0]));
  }

  protected barHeight(value: number, max: number): number {
    return Math.max(2, (value / max) * 100);
  }
}
