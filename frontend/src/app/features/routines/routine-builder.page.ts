import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { Exercise } from '../catalog/catalog.models';
import { ExercisePicker } from '../catalog/exercise-picker/exercise-picker';
import { RoutineApi } from '../training/routine.api';
import { RoutinePayload } from '../training/routine.models';

interface DraftSet {
  key: string;
  setType: string;
  targetReps: number;
  targetWeight: number | null;
  targetRir: number | null;
}

interface DraftExercise {
  key: string;
  exerciseId: string;
  name: string;
  restSeconds: number;
  supersetGroup: number | null;
  notes: string;
  sets: DraftSet[];
}

@Component({
  selector: 'app-routine-builder-page',
  imports: [ReactiveFormsModule, FormsModule, RouterLink, TranslatePipe, ExercisePicker],
  template: `
    <section class="mx-auto mt-8 max-w-4xl space-y-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <a routerLink="/routines" class="text-sm text-emerald-400">{{
            'routines.back' | translate
          }}</a>
          <h1 class="mt-1 text-3xl font-black">
            {{ (isNew() ? 'routines.new' : 'routines.edit') | translate }}
          </h1>
        </div>
        <div class="flex gap-2">
          <button
            type="button"
            (click)="cancel()"
            class="rounded-lg border border-zinc-700 px-4 py-2 text-sm"
          >
            {{ 'training.cancel' | translate }}
          </button>
          <button
            type="button"
            (click)="save()"
            [disabled]="saving() || form.invalid"
            class="rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white disabled:opacity-50"
          >
            {{ 'training.save' | translate }}
          </button>
        </div>
      </div>

      @if (error()) {
        <p class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
          {{ errorMessage() ?? ('routines.error' | translate) }}
        </p>
      }

      <form
        class="grid gap-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-4 sm:grid-cols-2"
        [formGroup]="form"
      >
        <label class="text-sm text-zinc-300" for="routineName">
          {{ 'routines.name' | translate }}
          <input
            id="routineName"
            formControlName="name"
            maxlength="80"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
          />
        </label>
        <label class="text-sm text-zinc-300" for="routineNotes">
          {{ 'training.notes' | translate }}
          <input
            id="routineNotes"
            formControlName="notes"
            maxlength="500"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
          />
        </label>
      </form>

      <app-exercise-picker (selectedExercise)="addExercise($event)" />

      <div class="flex flex-wrap items-center gap-2">
        <button
          type="button"
          (click)="groupSelected()"
          [disabled]="selected().size < 2"
          class="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm disabled:opacity-40"
        >
          {{ 'training.group' | translate }}
        </button>
        <button
          type="button"
          (click)="ungroupSelected()"
          [disabled]="!selected().size"
          class="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm disabled:opacity-40"
        >
          {{ 'training.ungroup' | translate }}
        </button>
      </div>

      @if (!exercises().length) {
        <p class="rounded-xl border border-dashed border-zinc-800 p-6 text-sm text-zinc-500">
          {{ 'routines.emptyBuilder' | translate }}
        </p>
      }

      <div class="space-y-4">
        @for (exercise of exercises(); track exercise.key; let index = $index) {
          <article
            class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4"
            draggable="true"
            (dragstart)="startDrag(exercise.key)"
            (dragover)="$event.preventDefault()"
            (drop)="dropExercise(exercise)"
          >
            <div class="flex items-center gap-3">
              <input
                type="checkbox"
                [checked]="selected().has(exercise.key)"
                (change)="toggle(exercise.key)"
                class="accent-emerald-500"
                [attr.aria-label]="'training.select' | translate"
              />
              <h2 class="font-semibold">{{ exercise.name }}</h2>
              @if (exercise.supersetGroup !== null) {
                <span class="rounded bg-sky-950 px-2 py-0.5 text-xs text-sky-300">
                  {{ supersetLabel(exercise) }}
                </span>
              }
              <div class="ml-auto flex gap-1">
                <button
                  type="button"
                  (click)="move(index, -1)"
                  [disabled]="index === 0"
                  class="rounded border border-zinc-700 px-1.5 text-xs disabled:opacity-30"
                  [attr.aria-label]="'training.moveUp' | translate"
                >
                  ↑
                </button>
                <button
                  type="button"
                  (click)="move(index, 1)"
                  [disabled]="index === exercises().length - 1"
                  class="rounded border border-zinc-700 px-1.5 text-xs disabled:opacity-30"
                  [attr.aria-label]="'training.moveDown' | translate"
                >
                  ↓
                </button>
                <button
                  type="button"
                  (click)="removeExercise(exercise)"
                  class="rounded border border-red-900 px-2 text-xs text-red-300"
                >
                  {{ 'training.delete' | translate }}
                </button>
              </div>
            </div>

            <div class="mt-3 grid gap-2 sm:grid-cols-2">
              <label class="text-xs text-zinc-400" for="routineRest">
                {{ 'training.restSeconds' | translate }}
                <input
                  id="routineRest"
                  type="number"
                  min="15"
                  max="600"
                  [(ngModel)]="exercise.restSeconds"
                  class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                />
              </label>
              <label class="text-xs text-zinc-400" for="routineExerciseNotes">
                {{ 'training.notes' | translate }}
                <input
                  id="routineExerciseNotes"
                  maxlength="500"
                  [(ngModel)]="exercise.notes"
                  class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                />
              </label>
            </div>

            <div class="mt-3">
              <p class="text-xs uppercase tracking-wider text-sky-400">
                {{ 'routines.targetSets' | translate }}
              </p>
              <div class="mt-2 grid gap-2 sm:grid-cols-3">
                <label class="text-xs text-zinc-400" for="compactSeries">
                  {{ 'routines.series' | translate }}
                  <input
                    id="compactSeries"
                    type="number"
                    min="0"
                    max="15"
                    [value]="exercise.sets.length"
                    (change)="setSeriesCount(exercise, $event)"
                    class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                  />
                </label>
                <label class="text-xs text-zinc-400" for="compactReps">
                  {{ 'routines.reps' | translate }}
                  <input
                    id="compactReps"
                    type="number"
                    min="1"
                    max="1000"
                    [value]="exercise.sets[0]?.targetReps ?? 8"
                    (change)="setTargetReps(exercise, $event)"
                    class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                  />
                </label>
                <p class="self-end text-xs text-zinc-500">
                  {{ exercise.sets.length }} × {{ exercise.sets[0]?.targetReps ?? 8 }}
                </p>
              </div>

              <details class="mt-2 text-xs text-zinc-400">
                <summary class="cursor-pointer">{{ 'routines.advancedSets' | translate }}</summary>
                <div class="mt-2 space-y-2">
                  @for (set of exercise.sets; track set.key; let setIndex = $index) {
                    <div
                      class="grid items-end gap-2 rounded border border-zinc-800 p-2 sm:grid-cols-7"
                    >
                      <label class="text-xs text-zinc-400" for="targetType">
                        {{ 'training.setType' | translate }}
                        <select
                          id="targetType"
                          [(ngModel)]="set.setType"
                          class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                        >
                          <option value="NORMAL">{{ 'training.normal' | translate }}</option>
                          <option value="WARMUP">{{ 'training.warmup' | translate }}</option>
                          <option value="FAILURE">{{ 'training.failure' | translate }}</option>
                          <option value="DROP">{{ 'training.drop' | translate }}</option>
                        </select>
                      </label>
                      <label class="text-xs text-zinc-400" for="targetReps">
                        {{ 'training.reps' | translate }}
                        <input
                          id="targetReps"
                          type="number"
                          min="1"
                          max="1000"
                          [(ngModel)]="set.targetReps"
                          class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                        />
                      </label>
                      <label class="text-xs text-zinc-400" for="targetWeight">
                        {{ 'training.weight' | translate }}
                        <input
                          id="targetWeight"
                          type="number"
                          step="0.5"
                          [(ngModel)]="set.targetWeight"
                          class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                        />
                      </label>
                      <label class="text-xs text-zinc-400" for="targetRir">
                        RIR
                        <input
                          id="targetRir"
                          type="number"
                          min="0"
                          max="10"
                          [(ngModel)]="set.targetRir"
                          class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                        />
                      </label>
                      <span class="text-xs text-zinc-500">#{{ setIndex + 1 }}</span>
                      <button
                        type="button"
                        (click)="removeSet(exercise, set)"
                        class="rounded border border-red-900 px-2 py-1 text-xs text-red-300"
                      >
                        {{ 'training.delete' | translate }}
                      </button>
                    </div>
                  }
                </div>
              </details>
            </div>
          </article>
        }
      </div>

      <div class="flex justify-end">
        <button
          type="button"
          (click)="save()"
          [disabled]="saving() || form.invalid"
          class="rounded-lg bg-emerald-600 px-5 py-2 font-semibold text-white disabled:opacity-50"
        >
          {{ 'training.save' | translate }}
        </button>
      </div>
    </section>
  `,
})
export class RoutineBuilderPage implements OnInit {
  private readonly api = inject(RoutineApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private counter = 0;
  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    notes: [''],
  });
  protected readonly routineId = signal<string | null>(null);
  protected readonly exercises = signal<DraftExercise[]>([]);
  protected readonly selected = signal<Set<string>>(new Set());
  protected readonly saving = signal(false);
  protected readonly error = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  private dragged: string | null = null;

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.routineId.set(id);
      try {
        const routine = await firstValueFrom(this.api.get(id));
        this.form.setValue({ name: routine.name, notes: routine.notes ?? '' });
        this.exercises.set(
          routine.exercises.map((exercise) => ({
            key: this.key(),
            exerciseId: exercise.exerciseId,
            name: exercise.name,
            restSeconds: exercise.restSeconds ?? 90,
            supersetGroup: exercise.supersetGroup,
            notes: exercise.notes ?? '',
            sets: exercise.sets.map((set) => ({
              key: this.key(),
              setType: set.setType,
              targetReps: set.targetReps,
              targetWeight: set.targetWeight,
              targetRir: set.targetRir,
            })),
          })),
        );
      } catch {
        this.error.set(true);
      }
    }
  }

  protected isNew(): boolean {
    return this.routineId() === null;
  }

  protected addExercise(exercise: Exercise): void {
    if (this.exercises().length >= 30) {
      this.error.set(true);
      return;
    }
    this.exercises.update((current) => [
      ...current,
      {
        key: this.key(),
        exerciseId: exercise.id,
        name: exercise.name,
        restSeconds: 90,
        supersetGroup: null,
        notes: '',
        sets: Array.from({ length: 3 }, () => this.newSet(8, null, null)),
      },
    ]);
  }

  protected addSet(exercise: DraftExercise): void {
    if (exercise.sets.length >= 15) {
      return;
    }
    const last = exercise.sets[exercise.sets.length - 1];
    exercise.sets.push(
      this.newSet(last?.targetReps ?? 8, last?.targetWeight ?? null, last?.targetRir ?? null),
    );
    this.exercises.update((current) => [...current]);
  }

  protected setSeriesCount(exercise: DraftExercise, event: Event): void {
    const count = Math.max(0, Math.min(15, Number((event.target as HTMLInputElement).value)));
    const last = exercise.sets[exercise.sets.length - 1];
    if (count > exercise.sets.length) {
      for (let index = exercise.sets.length; index < count; index++) {
        exercise.sets.push(
          this.newSet(last?.targetReps ?? 8, last?.targetWeight ?? null, last?.targetRir ?? null),
        );
      }
    } else {
      exercise.sets = exercise.sets.slice(0, count);
    }
    this.exercises.update((current) => [...current]);
  }

  protected setTargetReps(exercise: DraftExercise, event: Event): void {
    const reps = Math.max(1, Math.min(1000, Number((event.target as HTMLInputElement).value)));
    exercise.sets.forEach((set) => (set.targetReps = reps));
    this.exercises.update((current) => [...current]);
  }

  protected removeSet(exercise: DraftExercise, set: DraftSet): void {
    exercise.sets = exercise.sets.filter((candidate) => candidate.key !== set.key);
    this.exercises.update((current) => [...current]);
  }

  protected removeExercise(exercise: DraftExercise): void {
    if (
      exercise.sets.length &&
      !window.confirm('Este ejercicio tiene series objetivo. ¿Eliminarlo?')
    ) {
      return;
    }
    this.exercises.update((current) =>
      current.filter((candidate) => candidate.key !== exercise.key),
    );
    this.selected.update((current) => {
      const next = new Set(current);
      next.delete(exercise.key);
      return next;
    });
  }

  protected move(index: number, delta: number): void {
    const exercises = [...this.exercises()];
    const target = index + delta;
    if (target < 0 || target >= exercises.length) {
      return;
    }
    const [moved] = exercises.splice(index, 1);
    exercises.splice(target, 0, moved);
    this.exercises.set(exercises);
  }

  protected startDrag(key: string): void {
    this.dragged = key;
  }

  protected dropExercise(target: DraftExercise): void {
    const key = this.dragged;
    this.dragged = null;
    if (!key || key === target.key) {
      return;
    }
    const exercises = [...this.exercises()];
    const from = exercises.findIndex((exercise) => exercise.key === key);
    const to = exercises.findIndex((exercise) => exercise.key === target.key);
    if (from < 0 || to < 0) {
      return;
    }
    const [moved] = exercises.splice(from, 1);
    exercises.splice(to, 0, moved);
    this.exercises.set(exercises);
  }

  protected toggle(key: string): void {
    this.selected.update((current) => {
      const next = new Set(current);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  }

  protected groupSelected(): void {
    if (this.selected().size < 2 || this.selected().size > 5) {
      this.error.set(true);
      return;
    }
    const group =
      this.exercises().reduce((max, exercise) => Math.max(max, exercise.supersetGroup ?? 0), 0) + 1;
    this.exercises.update((current) =>
      current.map((exercise) =>
        this.selected().has(exercise.key) ? { ...exercise, supersetGroup: group } : exercise,
      ),
    );
    this.selected.set(new Set());
  }

  protected ungroupSelected(): void {
    this.exercises.update((current) =>
      current.map((exercise) =>
        this.selected().has(exercise.key) ? { ...exercise, supersetGroup: null } : exercise,
      ),
    );
    this.selected.set(new Set());
  }

  protected supersetLabel(exercise: DraftExercise): string {
    if (exercise.supersetGroup === null) {
      return '';
    }
    const group = this.exercises().filter(
      (candidate) => candidate.supersetGroup === exercise.supersetGroup,
    );
    const index = group.findIndex((candidate) => candidate.key === exercise.key);
    return `${String.fromCharCode(64 + exercise.supersetGroup)}${index + 1}`;
  }

  protected async save(): Promise<void> {
    if (this.form.invalid || !this.exercises().length || this.saving()) {
      this.error.set(true);
      return;
    }
    const value = this.form.getRawValue();
    const payload: RoutinePayload = {
      name: value.name.trim(),
      notes: value.notes.trim() || null,
      exercises: this.exercises().map((exercise) => ({
        exerciseId: exercise.exerciseId,
        restSeconds: exercise.restSeconds,
        supersetGroup: exercise.supersetGroup,
        notes: exercise.notes.trim() || null,
        sets: exercise.sets.map((set) => ({
          setType: set.setType,
          targetReps: Number(set.targetReps),
          targetWeight:
            set.targetWeight === null || set.targetWeight === undefined
              ? null
              : Number(set.targetWeight),
          targetRir:
            set.targetRir === null || set.targetRir === undefined ? null : Number(set.targetRir),
        })),
      })),
    };
    this.saving.set(true);
    this.error.set(false);
    try {
      const id = this.routineId();
      if (id) {
        await firstValueFrom(this.api.update(id, payload));
      } else {
        await firstValueFrom(this.api.create(payload));
      }
      await this.router.navigateByUrl('/routines');
    } catch (error) {
      this.error.set(true);
      this.errorMessage.set(
        error instanceof HttpErrorResponse
          ? ((error.error as { detail?: string } | null)?.detail ?? null)
          : null,
      );
    } finally {
      this.saving.set(false);
    }
  }

  protected async cancel(): Promise<void> {
    await this.router.navigateByUrl('/routines');
  }

  private key(): string {
    this.counter += 1;
    return `draft-${this.counter}`;
  }

  private newSet(reps: number, weight: number | null, rir: number | null): DraftSet {
    return {
      key: this.key(),
      setType: 'NORMAL',
      targetReps: reps,
      targetWeight: weight,
      targetRir: rir,
    };
  }
}
