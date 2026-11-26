import { Component, OnDestroy, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { Exercise } from '../catalog/catalog.models';
import { ExercisePicker } from '../catalog/exercise-picker/exercise-picker';
import { NewPersonalRecord, WorkoutSession } from './training.models';
import { TrainingApi } from './training.api';

@Component({
  selector: 'app-training-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe, ExercisePicker],
  template: `
    <section class="mx-auto mt-8 max-w-4xl space-y-6">
      <a routerLink="/" class="text-sm text-emerald-400">{{ 'training.back' | translate }}</a>

      @if (session(); as current) {
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <p class="text-sm uppercase tracking-wider text-emerald-400">
              {{ 'training.live' | translate }}
            </p>
            <h1 class="mt-1 text-3xl font-black">{{ current.name }}</h1>
            <p class="text-sm text-zinc-400">
              {{ current.exercises.length }} {{ 'training.exercises' | translate }} ·
              {{ current.totalVolume }} kg
            </p>
          </div>
          <div class="flex gap-2">
            <button
              type="button"
              (click)="discard()"
              class="rounded-lg border border-red-900 px-3 py-2 text-sm text-red-300"
            >
              {{ 'training.discard' | translate }}
            </button>
            <button
              type="button"
              (click)="finish()"
              [disabled]="finishing()"
              class="rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white disabled:opacity-50"
            >
              {{ 'training.finish' | translate }}
            </button>
          </div>
        </div>

        @if (newPrs().length) {
          <div class="rounded-xl border border-emerald-800 bg-emerald-950/40 p-4">
            <p class="text-sm font-semibold text-emerald-300">
              {{ 'training.newPrs' | translate }}
            </p>
            <ul class="mt-2 text-sm">
              @for (pr of newPrs(); track pr.prType + pr.exerciseId) {
                <li>{{ pr.exerciseName }} · {{ pr.prType }}: {{ pr.value }} kg</li>
              }
            </ul>
          </div>
        }

        <app-exercise-picker (selectedExercise)="addExercise($event)" />

        @if (!current.exercises.length) {
          <p class="rounded-xl border border-dashed border-zinc-800 p-6 text-sm text-zinc-500">
            {{ 'training.empty' | translate }}
          </p>
        }

        <form
          class="grid gap-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-4 sm:grid-cols-5"
          [formGroup]="setForm"
          (ngSubmit)="logSet()"
        >
          <label class="text-sm text-zinc-300 sm:col-span-2" for="trainingEntry">
            {{ 'training.exercise' | translate }}
            <select
              id="trainingEntry"
              formControlName="entryId"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
            >
              @for (entry of current.exercises; track entry.id) {
                <option [value]="entry.id">{{ entry.name }}</option>
              }
            </select>
          </label>
          <label class="text-sm text-zinc-300" for="trainingReps">
            {{ 'training.reps' | translate }}
            <input
              id="trainingReps"
              type="number"
              min="1"
              formControlName="reps"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
            />
          </label>
          <label class="text-sm text-zinc-300" for="trainingWeight">
            {{ 'training.weight' | translate }}
            <input
              id="trainingWeight"
              type="number"
              step="0.5"
              formControlName="weight"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
            />
          </label>
          <label class="text-sm text-zinc-300" for="trainingRir">
            RIR
            <input
              id="trainingRir"
              type="number"
              min="0"
              max="10"
              formControlName="rir"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
            />
          </label>
          <button
            type="submit"
            [disabled]="setForm.invalid || logging()"
            class="rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white disabled:opacity-50 sm:col-span-5"
          >
            {{ 'training.logSet' | translate }}
          </button>
        </form>

        @if (restRemaining(); as remaining) {
          <div
            class="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-sky-900 bg-sky-950/40 p-4"
          >
            <div>
              <p class="text-xs uppercase tracking-wider text-sky-400">
                {{ 'training.rest' | translate }}
              </p>
              <p class="text-3xl font-black tabular-nums">{{ formatRest(remaining) }}</p>
            </div>
            <div class="flex gap-2">
              <button
                type="button"
                (click)="addRest()"
                class="rounded-lg border border-sky-700 px-3 py-1.5 text-sm"
              >
                +15 s
              </button>
              <button
                type="button"
                (click)="skipRest()"
                class="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm"
              >
                {{ 'training.skipRest' | translate }}
              </button>
            </div>
          </div>
        }

        <div class="space-y-4">
          @for (entry of current.exercises; track entry.id) {
            <article class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
              <h2 class="font-semibold">{{ entry.name }}</h2>
              @if (!entry.sets.length) {
                <p class="mt-2 text-sm text-zinc-500">{{ 'training.noSets' | translate }}</p>
              } @else {
                <ul class="mt-3 space-y-1 text-sm">
                  @for (set of entry.sets; track set.id) {
                    <li class="flex justify-between gap-3 text-zinc-300">
                      <span>{{ set.setType }} · {{ set.reps }} × {{ set.weight }} kg</span>
                      <span class="text-zinc-500">{{ set.volume }} kg</span>
                    </li>
                  }
                </ul>
              }
            </article>
          }
        </div>
      } @else if (error()) {
        <p class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
          {{ 'training.notFound' | translate }}
        </p>
      } @else {
        <p class="text-zinc-400">{{ 'auth.loading' | translate }}</p>
      }
    </section>
  `,
})
export class TrainingPage implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(TrainingApi);
  private readonly formBuilder = inject(FormBuilder);
  protected readonly session = signal<WorkoutSession | null>(null);
  protected readonly error = signal(false);
  protected readonly logging = signal(false);
  protected readonly finishing = signal(false);
  protected readonly newPrs = signal<NewPersonalRecord[]>([]);
  protected readonly restRemaining = signal<number | null>(null);
  private restHandle: ReturnType<typeof setInterval> | null = null;

  protected readonly setForm = this.formBuilder.nonNullable.group({
    entryId: ['', [Validators.required]],
    reps: [8, [Validators.required, Validators.min(1), Validators.max(1000)]],
    weight: [20, [Validators.required]],
    rir: [null as number | null, [Validators.min(0), Validators.max(10)]],
  });

  constructor() {
    void this.load();
  }

  protected async addExercise(exercise: Exercise): Promise<void> {
    const session = this.session();
    if (!session) {
      return;
    }
    try {
      this.session.set(await firstValueFrom(this.api.addExercise(session.id, exercise.id)));
    } catch {
      this.error.set(true);
    }
  }

  protected async logSet(): Promise<void> {
    const session = this.session();
    if (!session || this.setForm.invalid || this.logging()) {
      return;
    }
    this.logging.set(true);
    try {
      const value = this.setForm.getRawValue();
      await firstValueFrom(
        this.api.logSet(session.id, value.entryId, {
          reps: value.reps,
          weight: value.weight,
          rir: value.rir,
          setType: 'NORMAL',
        }),
      );
      const updated = await firstValueFrom(this.api.get(session.id));
      this.session.set(updated);
      this.setForm.patchValue({ reps: value.reps, weight: value.weight, rir: null });
      const entry = updated.exercises.find((candidate) => candidate.id === value.entryId);
      this.startRest(entry?.restSeconds ?? 90);
    } catch {
      this.error.set(true);
    } finally {
      this.logging.set(false);
    }
  }

  protected async finish(): Promise<void> {
    const session = this.session();
    if (!session || this.finishing()) {
      return;
    }
    this.finishing.set(true);
    try {
      const result = await firstValueFrom(this.api.finish(session.id));
      this.session.set(result.session);
      this.newPrs.set(result.newPersonalRecords);
    } catch {
      this.error.set(true);
    } finally {
      this.finishing.set(false);
    }
  }

  protected async discard(): Promise<void> {
    const session = this.session();
    if (!session) {
      return;
    }
    try {
      await firstValueFrom(this.api.discard(session.id));
      await this.router.navigateByUrl('/');
    } catch {
      this.error.set(true);
    }
  }

  protected formatRest(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const rest = seconds % 60;
    return `${minutes}:${rest.toString().padStart(2, '0')}`;
  }

  protected addRest(): void {
    this.restRemaining.update((seconds) => (seconds ?? 0) + 15);
  }

  protected skipRest(): void {
    this.stopRest();
    this.restRemaining.set(null);
  }

  ngOnDestroy(): void {
    this.stopRest();
  }

  private startRest(seconds: number): void {
    this.stopRest();
    this.restRemaining.set(seconds);
    this.restHandle = setInterval(() => {
      const current = this.restRemaining();
      if (current === null || current <= 1) {
        this.stopRest();
        this.restRemaining.set(null);
      } else {
        this.restRemaining.set(current - 1);
      }
    }, 1000);
  }

  private stopRest(): void {
    if (this.restHandle) {
      clearInterval(this.restHandle);
      this.restHandle = null;
    }
  }

  private async load(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set(true);
      return;
    }
    try {
      const session = await firstValueFrom(this.api.get(id));
      this.session.set(session);
      if (session.exercises.length) {
        this.setForm.patchValue({ entryId: session.exercises[0].id });
      }
    } catch {
      this.error.set(true);
    }
  }
}
