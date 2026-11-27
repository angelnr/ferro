import { DatePipe } from '@angular/common';
import { WeightPipe } from '../../core/units/units.pipes';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { Exercise } from '../catalog/catalog.models';
import { ExercisePicker } from '../catalog/exercise-picker/exercise-picker';
import { ProgressApi } from './progress.api';
import { PersonalRecordGroup } from './progress.models';

@Component({
  selector: 'app-records-page',
  imports: [DatePipe, RouterLink, TranslatePipe, ExercisePicker, WeightPipe],
  template: `
    <section class="mx-auto mt-8 max-w-4xl space-y-6">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p class="text-sm uppercase tracking-wider text-emerald-400">
            {{ 'progress.title' | translate }}
          </p>
          <h1 class="mt-1 text-3xl font-black">{{ 'progress.records' | translate }}</h1>
        </div>
        <a routerLink="/history" class="text-sm text-emerald-400">{{
          'progress.backHistory' | translate
        }}</a>
      </div>

      <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
        <p class="text-sm text-zinc-300">{{ 'progress.exerciseFilter' | translate }}</p>
        <app-exercise-picker (selectedExercise)="filter($event)" />
        @if (filtered(); as exercise) {
          <button type="button" (click)="clear()" class="mt-1 text-xs text-zinc-400">
            {{ 'progress.clearFilter' | translate }} · {{ exercise.name }}
          </button>
        }
      </div>

      @if (error()) {
        <p class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
          {{ 'progress.error' | translate }}
        </p>
      }

      @if (!groups().length) {
        <p class="rounded-xl border border-dashed border-zinc-800 p-8 text-sm text-zinc-500">
          {{ 'progress.noRecords' | translate }}
        </p>
      }

      <div class="space-y-3">
        @for (group of groups(); track group.exerciseId) {
          <article class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <h2 class="font-semibold">{{ group.exerciseName }}</h2>
            <div class="mt-2 grid gap-2 sm:grid-cols-2">
              @for (record of group.current; track record.type) {
                <div class="rounded-lg border border-emerald-900 bg-emerald-950/20 p-3">
                  <p class="text-xs uppercase tracking-wider text-emerald-400">
                    {{ record.type }}
                  </p>
                  <p class="text-2xl font-black">{{ record.value | weight }}</p>
                  <p class="text-xs text-zinc-500">
                    {{ record.achievedAt | date: 'mediumDate' }}
                  </p>
                </div>
              }
            </div>
            @if (group.history.length) {
              <details class="mt-3 text-sm text-zinc-400">
                <summary class="cursor-pointer">
                  {{ 'progress.previousRecords' | translate }}
                </summary>
                <ul class="mt-2 space-y-1">
                  @for (record of group.history; track record.achievedAt + record.type) {
                    <li class="flex justify-between gap-4">
                      <span>{{ record.type }}</span>
                      <span
                        >{{ record.value | weight }} ·
                        {{ record.achievedAt | date: 'mediumDate' }}</span
                      >
                    </li>
                  }
                </ul>
              </details>
            }
          </article>
        }
      </div>
    </section>
  `,
})
export class RecordsPage implements OnInit {
  private readonly api = inject(ProgressApi);
  protected readonly groups = signal<PersonalRecordGroup[]>([]);
  protected readonly filtered = signal<Exercise | null>(null);
  protected readonly error = signal(false);

  async ngOnInit(): Promise<void> {
    await this.load();
  }

  protected filter(exercise: Exercise): void {
    this.filtered.set(exercise);
    void this.load();
  }

  protected clear(): void {
    this.filtered.set(null);
    void this.load();
  }

  private async load(): Promise<void> {
    this.error.set(false);
    try {
      this.groups.set(await firstValueFrom(this.api.personalRecords(this.filtered()?.id)));
    } catch {
      this.error.set(true);
    }
  }
}
