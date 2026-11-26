import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { CatalogApi } from '../catalog.api';
import { ExerciseDetail } from '../catalog.models';

@Component({
  selector: 'app-exercise-detail-page',
  imports: [DatePipe, RouterLink, TranslatePipe],
  template: `
    <section class="mx-auto mt-8 max-w-3xl space-y-6">
      <a routerLink="/catalog" class="text-sm text-emerald-400">{{ 'catalog.back' | translate }}</a>

      @if (detail(); as data) {
        <div>
          <p class="text-sm uppercase tracking-wider text-emerald-400">
            {{ 'catalog.detail' | translate }}
          </p>
          <h1 class="mt-1 text-3xl font-black">{{ data.exercise.name }}</h1>
          <p class="text-zinc-400">{{ data.exercise.nameEn }}</p>
          <p class="mt-2 text-xs uppercase tracking-wider text-zinc-500">
            {{ data.exercise.muscleGroup }} · {{ data.exercise.equipment }} ·
            {{ data.exercise.mechanics }}
          </p>
        </div>

        <div class="grid gap-4 sm:grid-cols-3">
          <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <p class="text-xs uppercase tracking-wider text-zinc-500">
              {{ 'catalog.sessions' | translate }}
            </p>
            <p class="mt-2 text-2xl font-bold">{{ data.stats.sessionCount }}</p>
          </div>
          <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <p class="text-xs uppercase tracking-wider text-zinc-500">
              {{ 'catalog.volume' | translate }}
            </p>
            <p class="mt-2 text-2xl font-bold">{{ data.stats.totalVolume }} kg</p>
          </div>
          <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <p class="text-xs uppercase tracking-wider text-zinc-500">
              {{ 'catalog.e1rm' | translate }}
            </p>
            <p class="mt-2 text-2xl font-bold">{{ data.stats.bestE1rm ?? '—' }}</p>
          </div>
        </div>

        @if (data.stats.personalRecords.length) {
          <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <p class="text-xs uppercase tracking-wider text-zinc-500">
              {{ 'catalog.prs' | translate }}
            </p>
            <ul class="mt-3 space-y-2 text-sm">
              @for (record of data.stats.personalRecords; track record.type) {
                <li class="flex justify-between gap-4">
                  <span>{{ record.type }}</span>
                  <span class="font-semibold">{{ record.value }} kg</span>
                </li>
              }
            </ul>
          </div>
        } @else {
          <p class="rounded-xl border border-dashed border-zinc-800 p-4 text-sm text-zinc-500">
            {{ 'catalog.noStats' | translate }}
          </p>
        }

        @if (data.stats.e1rmSeries.length) {
          <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <p class="text-xs uppercase tracking-wider text-zinc-500">
              {{ 'catalog.e1rmSeries' | translate }} · 52 {{ 'progress.weeks' | translate }}
            </p>
            <svg
              viewBox="0 0 100 40"
              class="mt-3 h-40 w-full"
              role="img"
              [attr.aria-label]="'catalog.e1rmSeries' | translate"
            >
              <polyline
                [attr.points]="linePoints()"
                fill="none"
                stroke="rgb(16 185 129)"
                stroke-width="1"
                vector-effect="non-scaling-stroke"
              />
              @for (dot of dots(); track dot.date) {
                <circle [attr.cx]="dot.x" [attr.cy]="dot.y" r="0.8" fill="rgb(52 211 153)" />
              }
            </svg>
            <div class="flex justify-between text-xs text-zinc-500">
              <span>{{ data.stats.e1rmSeries[0].date | date: 'shortDate' }}</span>
              <span> {{ data.stats.e1rmSeries[data.stats.e1rmSeries.length - 1].value }} kg </span>
            </div>
          </div>
        }
      } @else if (error()) {
        <p class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
          {{ 'catalog.detailError' | translate }}
        </p>
      } @else {
        <p class="text-zinc-400">{{ 'auth.loading' | translate }}</p>
      }
    </section>
  `,
})
export class ExerciseDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(CatalogApi);
  protected readonly detail = signal<ExerciseDetail | null>(null);
  protected readonly error = signal(false);

  constructor() {
    void this.load();
  }

  protected linePoints(): string {
    const series = this.detail()?.stats.e1rmSeries ?? [];
    if (series.length < 2) {
      return series.length ? '0,20 100,20' : '';
    }
    const values = series.map((point) => point.value);
    const min = Math.min(...values);
    const max = Math.max(...values);
    return series
      .map((point, index) => {
        const x = (index / (series.length - 1)) * 100;
        const y = max === min ? 20 : 38 - ((point.value - min) / (max - min)) * 36;
        return `${x},${y}`;
      })
      .join(' ');
  }

  protected dots(): { x: number; y: number; date: string }[] {
    const series = this.detail()?.stats.e1rmSeries ?? [];
    const values = series.map((point) => point.value);
    const min = values.length ? Math.min(...values) : 0;
    const max = values.length ? Math.max(...values) : 1;
    return series.map((point, index) => ({
      x: series.length > 1 ? (index / (series.length - 1)) * 100 : 50,
      y: max === min ? 20 : 38 - ((point.value - min) / (max - min)) * 36,
      date: point.date,
    }));
  }

  private async load(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set(true);
      return;
    }
    try {
      this.detail.set(await firstValueFrom(this.api.detail(id)));
    } catch {
      this.error.set(true);
    }
  }
}
