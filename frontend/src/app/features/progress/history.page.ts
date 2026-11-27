import { DatePipe } from '@angular/common';
import { WeightPipe } from '../../core/units/units.pipes';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { Exercise } from '../catalog/catalog.models';
import { ExercisePicker } from '../catalog/exercise-picker/exercise-picker';
import { ProgressApi } from './progress.api';
import { HistorySession } from './progress.models';

@Component({
  selector: 'app-history-page',
  imports: [DatePipe, RouterLink, TranslatePipe, ExercisePicker, WeightPipe],
  template: `
    <section class="mx-auto mt-8 max-w-4xl space-y-6">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p class="text-sm uppercase tracking-wider text-emerald-400">
            {{ 'progress.title' | translate }}
          </p>
          <h1 class="mt-1 text-3xl font-black">{{ 'progress.history' | translate }}</h1>
        </div>
        <div class="flex gap-2 text-sm">
          <a routerLink="/stats" class="rounded-lg border border-zinc-700 px-3 py-1.5">{{
            'progress.stats' | translate
          }}</a>
          <a routerLink="/records" class="rounded-lg border border-zinc-700 px-3 py-1.5">{{
            'progress.records' | translate
          }}</a>
        </div>
      </div>

      <div class="grid gap-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-4 sm:grid-cols-3">
        <label class="text-sm text-zinc-300" for="historyFrom">
          {{ 'progress.from' | translate }}
          <input
            id="historyFrom"
            type="date"
            [value]="from()"
            (change)="from.set(value($event))"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
          />
        </label>
        <label class="text-sm text-zinc-300" for="historyTo">
          {{ 'progress.to' | translate }}
          <input
            id="historyTo"
            type="date"
            [value]="to()"
            (change)="to.set(value($event))"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
          />
        </label>
        <div>
          <p class="text-sm text-zinc-300">{{ 'progress.exerciseFilter' | translate }}</p>
          <app-exercise-picker (selectedExercise)="filterExercise($event)" />
          @if (exerciseFilter()) {
            <button type="button" (click)="clearExercise()" class="mt-1 text-xs text-zinc-400">
              {{ 'progress.clearFilter' | translate }} · {{ exerciseFilter()?.name }}
            </button>
          }
        </div>
      </div>

      @if (error()) {
        <p class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
          {{ 'progress.error' | translate }}
        </p>
      }

      @if (!loading() && !sessions().length) {
        <p class="rounded-xl border border-dashed border-zinc-800 p-8 text-sm text-zinc-500">
          {{ 'progress.empty' | translate }}
        </p>
      }

      <div class="space-y-3">
        @for (session of sessions(); track session.id) {
          <a
            [routerLink]="['/history', session.id]"
            class="block rounded-xl border border-zinc-800 bg-zinc-900/60 p-4 hover:border-zinc-600"
          >
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 class="font-semibold">{{ session.name }}</h2>
                <p class="text-sm text-zinc-500">
                  {{ session.finishedAt | date: 'medium' }} · {{ session.exerciseCount }}
                  {{ 'progress.exercises' | translate }} · {{ session.setCount }}
                  {{ 'progress.sets' | translate }}
                </p>
              </div>
              <div class="text-right">
                <p class="font-semibold">{{ session.totalVolume | weight }}</p>
                <p class="text-xs text-zinc-500">{{ duration(session.durationSeconds) }}</p>
              </div>
            </div>
            <div class="mt-2 flex flex-wrap gap-2 text-xs">
              @if (session.status === 'AUTO_CLOSED') {
                <span class="rounded bg-amber-950 px-2 py-0.5 text-amber-300">
                  {{ 'progress.autoClosed' | translate }}
                </span>
              }
              @if (session.hasPr) {
                <span class="rounded bg-emerald-950 px-2 py-0.5 text-emerald-300">
                  {{ 'progress.prBadge' | translate }}
                </span>
              }
            </div>
          </a>
        }
      </div>

      @if (totalPages() > 1) {
        <div class="flex items-center justify-between text-sm">
          <button
            type="button"
            (click)="go(-1)"
            [disabled]="page() === 0"
            class="rounded border border-zinc-700 px-3 py-1.5 disabled:opacity-40"
          >
            ←
          </button>
          <span class="text-zinc-500">{{ page() + 1 }} / {{ totalPages() }}</span>
          <button
            type="button"
            (click)="go(1)"
            [disabled]="page() + 1 >= totalPages()"
            class="rounded border border-zinc-700 px-3 py-1.5 disabled:opacity-40"
          >
            →
          </button>
        </div>
      }
    </section>
  `,
})
export class HistoryPage implements OnInit {
  private readonly api = inject(ProgressApi);
  protected readonly sessions = signal<HistorySession[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly from = signal('');
  protected readonly to = signal('');
  protected readonly exerciseFilter = signal<Exercise | null>(null);

  async ngOnInit(): Promise<void> {
    await this.load();
  }

  protected value(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  protected filterExercise(exercise: Exercise): void {
    this.exerciseFilter.set(exercise);
    this.page.set(0);
    void this.load();
  }

  protected clearExercise(): void {
    this.exerciseFilter.set(null);
    this.page.set(0);
    void this.load();
  }

  protected async go(delta: number): Promise<void> {
    const target = this.page() + delta;
    if (target < 0 || target >= this.totalPages()) {
      return;
    }
    this.page.set(target);
    await this.load();
  }

  protected duration(seconds: number): string {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    return hours ? `${hours} h ${minutes} min` : `${minutes} min`;
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(false);
    try {
      const from = this.from() ? new Date(`${this.from()}T00:00:00`).toISOString() : undefined;
      const to = this.to() ? new Date(`${this.to()}T23:59:59`).toISOString() : undefined;
      const result = await firstValueFrom(
        this.api.history({
          from,
          to,
          exerciseId: this.exerciseFilter()?.id,
          page: this.page(),
          size: 20,
        }),
      );
      this.sessions.set(result.content);
      this.totalPages.set(result.totalPages);
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }
}
