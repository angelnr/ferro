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
              {{ 'catalog.e1rmSeries' | translate }}
            </p>
            <ul class="mt-3 space-y-1 text-sm">
              @for (point of data.stats.e1rmSeries; track point.date) {
                <li class="flex justify-between gap-4">
                  <span class="text-zinc-500">{{ point.date | date: 'mediumDate' }}</span>
                  <span class="font-medium">{{ point.value }} kg</span>
                </li>
              }
            </ul>
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
