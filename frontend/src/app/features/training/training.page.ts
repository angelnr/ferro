import { Component, OnDestroy, inject, signal } from '@angular/core';
import { WeightPipe } from '../../core/units/units.pipes';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { SessionStore } from '../../core/auth/session.store';
import { CatalogApi } from '../catalog/catalog.api';
import { Exercise, PersonalRecord } from '../catalog/catalog.models';
import { ExercisePicker } from '../catalog/exercise-picker/exercise-picker';
import {
  NewPersonalRecord,
  PreviousSession,
  PreviousSet,
  SessionExercise,
  SessionSet,
  TargetSet,
  WorkoutSession,
} from './training.models';
import { TrainingApi } from './training.api';

@Component({
  selector: 'app-training-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe, ExercisePicker, WeightPipe],
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
            @if (current.notes) {
              <p class="mt-1 max-w-xl text-sm text-zinc-400">{{ current.notes }}</p>
            }
            <p class="text-sm text-zinc-400">
              {{ current.exercises.length }} {{ 'training.exercises' | translate }} ·
              {{ current.totalVolume | weight }} · {{ formatDuration(elapsed()) }}
            </p>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              type="button"
              (click)="startSessionEdit()"
              class="rounded-lg border border-zinc-700 px-3 py-2 text-sm"
            >
              {{ 'training.editSession' | translate }}
            </button>
            <button
              type="button"
              (click)="discard()"
              class="rounded-lg border border-red-900 px-3 py-2 text-sm text-red-300"
            >
              {{ 'training.discard' | translate }}
            </button>
            @if (current.status === 'ACTIVE') {
              <button
                type="button"
                (click)="finish()"
                [disabled]="finishing() || totalSets() === 0"
                class="rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white disabled:opacity-50"
              >
                {{ 'training.finish' | translate }}
              </button>
            }
          </div>
        </div>

        @if (editingSession()) {
          <form
            class="grid gap-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-4"
            [formGroup]="sessionForm"
            (ngSubmit)="saveSession()"
          >
            <label class="text-sm text-zinc-300" for="sessionName">
              {{ 'training.name' | translate }}
              <input
                id="sessionName"
                formControlName="name"
                maxlength="80"
                class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
              />
            </label>
            <label class="text-sm text-zinc-300" for="sessionNotes">
              {{ 'training.notes' | translate }}
              <textarea
                id="sessionNotes"
                formControlName="notes"
                maxlength="500"
                rows="2"
                class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
              ></textarea>
            </label>
            <div class="flex gap-2">
              <button
                type="submit"
                [disabled]="sessionForm.invalid"
                class="rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white disabled:opacity-50"
              >
                {{ 'training.save' | translate }}
              </button>
              <button
                type="button"
                (click)="cancelSessionEdit()"
                class="rounded-lg border border-zinc-700 px-4 py-2"
              >
                {{ 'training.cancel' | translate }}
              </button>
            </div>
          </form>
        }

        @if (current.status !== 'ACTIVE') {
          <div class="rounded-xl border border-emerald-800 bg-emerald-950/30 p-4">
            <h2 class="text-lg font-bold text-emerald-200">{{ 'training.summary' | translate }}</h2>
            <p class="mt-1 text-sm text-zinc-300">
              {{ 'training.duration' | translate }}:
              {{ formatDuration(current.durationSeconds ?? 0) }} ·
              {{ 'training.volume' | translate }}: {{ current.totalVolume | weight }} ·
              {{ 'training.sets' | translate }}: {{ totalSets() }}
            </p>
          </div>
        }

        @if (newPrs().length) {
          <div class="rounded-xl border border-emerald-800 bg-emerald-950/40 p-4">
            <p class="text-sm font-semibold text-emerald-300">
              {{ 'training.newPrs' | translate }}
            </p>
            <ul class="mt-2 text-sm">
              @for (pr of newPrs(); track pr.prType + pr.exerciseId) {
                <li>{{ pr.exerciseName }} · {{ pr.prType }}: {{ pr.value | weight }}</li>
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
          class="grid gap-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-4 sm:grid-cols-6"
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
          <label class="text-sm text-zinc-300" for="trainingSetType">
            {{ 'training.setType' | translate }}
            <select
              id="trainingSetType"
              formControlName="setType"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
            >
              <option value="NORMAL">{{ 'training.normal' | translate }}</option>
              <option value="WARMUP">{{ 'training.warmup' | translate }}</option>
              <option value="FAILURE">{{ 'training.failure' | translate }}</option>
              <option value="DROP">{{ 'training.drop' | translate }}</option>
            </select>
          </label>
          <label class="text-sm text-zinc-300 sm:col-span-2" for="trainingNotes">
            {{ 'training.notes' | translate }}
            <input
              id="trainingNotes"
              maxlength="500"
              formControlName="notes"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
            />
          </label>
          <button
            type="submit"
            [disabled]="setForm.invalid || logging()"
            class="rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white disabled:opacity-50 sm:col-span-6"
          >
            {{ 'training.logSet' | translate }}
          </button>
        </form>

        @if (restRemaining(); as remaining) {
          <div
            class="sticky bottom-4 z-10 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-sky-900 bg-sky-950/95 p-4 shadow-lg backdrop-blur"
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

        <div class="flex flex-wrap items-center gap-2">
          <button
            type="button"
            (click)="groupSelected()"
            [disabled]="selectedEntries().size < 2"
            class="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm disabled:opacity-40"
          >
            {{ 'training.group' | translate }}
          </button>
          <button
            type="button"
            (click)="ungroupSelected()"
            [disabled]="!selectedEntries().size"
            class="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm disabled:opacity-40"
          >
            {{ 'training.ungroup' | translate }}
          </button>
        </div>

        <div class="space-y-4">
          @for (entry of current.exercises; track entry.id) {
            <article
              class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4"
              draggable="true"
              (dragstart)="startDrag(entry.id)"
              (dragover)="$event.preventDefault()"
              (drop)="dropEntry(entry)"
            >
              @if (previous()[entry.exerciseId]; as sessions) {
                @if (sessions.length) {
                  <div class="mt-2 space-y-1 text-xs text-zinc-500">
                    @for (past of sessions; track past.sessionId) {
                      <p>
                        <span class="font-semibold text-zinc-400">{{
                          past.startedAt.slice(0, 10)
                        }}</span>:
                        @for (set of past.sets; track set.loggedAt) {
                          <span
                            class="ml-1"
                            [class.font-bold]="bestSet(past)?.loggedAt === set.loggedAt"
                            [class.text-zinc-300]="bestSet(past)?.loggedAt === set.loggedAt"
                          >
                            {{ set.reps }}×{{ set.weight }}
                            @if (set.setType !== 'NORMAL') {
                              ({{ set.setType }})
                            }
                          </span>
                        }
                      </p>
                    }
                  </div>
                }
              }
              @if (personalRecords()[entry.exerciseId]; as records) {
                @if (records.length) {
                  <p class="mt-1 text-xs text-emerald-400">
                    @for (record of records; track record.type) {
                      <span class="mr-2">PR {{ record.type }}: {{ record.value | weight }}</span>
                    }
                  </p>
                }
              }
              <div class="flex items-center gap-3">
                <input
                  type="checkbox"
                  [checked]="selectedEntries().has(entry.id)"
                  (change)="toggleEntry(entry.id)"
                  class="accent-emerald-500"
                  [attr.aria-label]="'training.select' | translate"
                />
                <h2 class="font-semibold">{{ entry.name }}</h2>
                <div class="ml-auto flex gap-1">
                  <button
                    type="button"
                    (click)="move(entry.id, -1)"
                    [disabled]="entry.position === 0"
                    class="rounded border border-zinc-700 px-1.5 text-xs disabled:opacity-30"
                    [attr.aria-label]="'training.moveUp' | translate"
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    (click)="move(entry.id, 1)"
                    [disabled]="entry.position === current.exercises.length - 1"
                    class="rounded border border-zinc-700 px-1.5 text-xs disabled:opacity-30"
                    [attr.aria-label]="'training.moveDown' | translate"
                  >
                    ↓
                  </button>
                </div>
                @if (entry.supersetGroup !== null) {
                  <span class="rounded bg-sky-950 px-2 py-0.5 text-xs text-sky-300">
                    {{ supersetLabel(entry) }}
                  </span>
                }
              </div>
              @if (entry.targetSets.length) {
                <div class="mt-3">
                  <p class="text-xs uppercase tracking-wider text-sky-400">
                    {{ 'training.pendingTargets' | translate }}
                  </p>
                  <div class="mt-1 flex flex-wrap gap-2">
                    @for (target of entry.targetSets; track target.id) {
                      <button
                        type="button"
                        (click)="useTarget(entry, target)"
                        class="rounded-full border border-sky-800 bg-sky-950/60 px-2.5 py-1 text-xs text-sky-200"
                      >
                        {{ target.setType }} · {{ target.targetReps }}×{{
                          target.targetWeight ?? '—'
                        }}
                        kg
                      </button>
                    }
                  </div>
                </div>
              }
              @if (!entry.sets.length) {
                <p class="mt-2 text-sm text-zinc-500">{{ 'training.noSets' | translate }}</p>
              } @else {
                <ul class="mt-3 space-y-2 text-sm">
                  @for (set of entry.sets; track set.id) {
                    <li class="rounded-lg border border-zinc-800 px-3 py-2 text-zinc-300">
                      @if (editingSet()?.set?.id === set.id) {
                        <form
                          class="grid gap-2 sm:grid-cols-6"
                          [formGroup]="editSetForm"
                          (ngSubmit)="saveSetEdit()"
                        >
                          <label class="text-xs text-zinc-400" for="editReps">
                            {{ 'training.reps' | translate }}
                            <input
                              id="editReps"
                              type="number"
                              min="1"
                              formControlName="reps"
                              class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                            />
                          </label>
                          <label class="text-xs text-zinc-400" for="editWeight">
                            {{ 'training.weight' | translate }}
                            <input
                              id="editWeight"
                              type="number"
                              step="0.5"
                              formControlName="weight"
                              class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                            />
                          </label>
                          <label class="text-xs text-zinc-400" for="editRir">
                            RIR
                            <input
                              id="editRir"
                              type="number"
                              min="0"
                              max="10"
                              formControlName="rir"
                              class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                            />
                          </label>
                          <label class="text-xs text-zinc-400" for="editType">
                            {{ 'training.setType' | translate }}
                            <select
                              id="editType"
                              formControlName="setType"
                              class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                            >
                              <option value="NORMAL">{{ 'training.normal' | translate }}</option>
                              <option value="WARMUP">{{ 'training.warmup' | translate }}</option>
                              <option value="FAILURE">{{ 'training.failure' | translate }}</option>
                              <option value="DROP">{{ 'training.drop' | translate }}</option>
                            </select>
                          </label>
                          <label class="text-xs text-zinc-400 sm:col-span-2" for="editNotes">
                            {{ 'training.notes' | translate }}
                            <input
                              id="editNotes"
                              maxlength="500"
                              formControlName="notes"
                              class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                            />
                          </label>
                          <div class="flex gap-2 sm:col-span-6">
                            <button
                              type="submit"
                              [disabled]="editSetForm.invalid"
                              class="rounded bg-emerald-600 px-3 py-1 text-xs font-semibold text-white disabled:opacity-50"
                            >
                              {{ 'training.save' | translate }}
                            </button>
                            <button
                              type="button"
                              (click)="cancelSetEdit()"
                              class="rounded border border-zinc-700 px-3 py-1 text-xs"
                            >
                              {{ 'training.cancel' | translate }}
                            </button>
                          </div>
                        </form>
                      } @else {
                        <div class="flex flex-wrap items-center justify-between gap-2">
                          <div>
                            <span
                              class="rounded px-1.5 py-0.5 text-xs font-semibold"
                              [class]="setTypeClass(set.setType)"
                            >
                              {{ set.setType }}
                            </span>
                            <span class="ml-1">{{ set.reps }} × {{ set.weight | weight }}</span>
                            @if (set.notes) {
                              <p class="text-xs text-zinc-500">{{ set.notes }}</p>
                            }
                          </div>
                          <div class="flex items-center gap-2">
                            <span class="text-zinc-500">{{ set.volume | weight }}</span>
                            <button
                              type="button"
                              (click)="startSetEdit(entry.id, set)"
                              class="rounded border border-zinc-700 px-2 py-0.5 text-xs"
                            >
                              {{ 'training.edit' | translate }}
                            </button>
                            <button
                              type="button"
                              (click)="deleteSet(entry.id, set)"
                              class="rounded border border-red-900 px-2 py-0.5 text-xs text-red-300"
                            >
                              {{ 'training.delete' | translate }}
                            </button>
                          </div>
                        </div>
                      }
                    </li>
                  }
                </ul>
              }
              <details class="mt-3 text-sm text-zinc-400">
                <summary class="cursor-pointer">
                  {{ 'training.exerciseSettings' | translate }}
                </summary>
                <div class="mt-2 grid gap-2 sm:grid-cols-2">
                  <label class="text-xs" for="entryRest">
                    {{ 'training.restSeconds' | translate }}
                    <input
                      id="entryRest"
                      type="number"
                      min="15"
                      max="600"
                      [value]="entry.restSeconds ?? 90"
                      (change)="changeRest(entry.id, $event)"
                      class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                    />
                  </label>
                  <label class="text-xs" for="entryNotes">
                    {{ 'training.notes' | translate }}
                    <input
                      id="entryNotes"
                      maxlength="500"
                      [value]="entry.notes ?? ''"
                      (blur)="changeNotes(entry.id, $event)"
                      class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                    />
                  </label>
                </div>
                <button
                  type="button"
                  (click)="deleteEntry(entry)"
                  class="mt-2 rounded border border-red-900 px-3 py-1 text-xs text-red-300"
                >
                  {{ 'training.deleteExercise' | translate }}
                </button>
              </details>
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
  private readonly catalog = inject(CatalogApi);
  private readonly sessionStore = inject(SessionStore);
  private readonly formBuilder = inject(FormBuilder);
  protected readonly session = signal<WorkoutSession | null>(null);
  protected readonly error = signal(false);
  protected readonly logging = signal(false);
  protected readonly finishing = signal(false);
  protected readonly newPrs = signal<NewPersonalRecord[]>([]);
  protected readonly restRemaining = signal<number | null>(null);
  protected readonly elapsed = signal(0);
  protected readonly selectedEntries = signal<Set<string>>(new Set());
  protected readonly previous = signal<Record<string, PreviousSession[]>>({});
  protected readonly editingSession = signal(false);
  protected readonly editingSet = signal<{ entryId: string; set: SessionSet } | null>(null);
  private restHandle: ReturnType<typeof setInterval> | null = null;
  private elapsedHandle: ReturnType<typeof setInterval> | null = null;
  private readonly previousLoading = new Set<string>();
  protected readonly personalRecords = signal<Record<string, PersonalRecord[]>>({});
  protected readonly draggedEntry = signal<string | null>(null);
  private readonly prLoading = new Set<string>();

  protected readonly setForm = this.formBuilder.nonNullable.group({
    entryId: ['', [Validators.required]],
    reps: [8, [Validators.required, Validators.min(1), Validators.max(1000)]],
    weight: [20, [Validators.required]],
    rir: [null as number | null, [Validators.min(0), Validators.max(10)]],
    setType: ['NORMAL', [Validators.required]],
    notes: [''],
    targetSetId: [''],
  });

  protected readonly sessionForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    notes: [''],
  });

  protected readonly editSetForm = this.formBuilder.nonNullable.group({
    reps: [8, [Validators.required, Validators.min(1), Validators.max(1000)]],
    weight: [20, [Validators.required]],
    rir: [null as number | null, [Validators.min(0), Validators.max(10)]],
    setType: ['NORMAL', [Validators.required]],
    notes: [''],
  });

  constructor() {
    void this.load();
    this.setForm.controls.entryId.valueChanges.pipe(takeUntilDestroyed()).subscribe((entryId) => {
      const session = this.session();
      const entry = session?.exercises.find((candidate) => candidate.id === entryId);
      if (entry) {
        void this.loadPrevious(entry.exerciseId);
      }
    });
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
    const value = this.setForm.getRawValue();
    const entry = session.exercises.find((candidate) => candidate.id === value.entryId);
    if (!entry) {
      this.logging.set(false);
      return;
    }
    const optimisticSet: SessionSet = {
      id: `pending-${Date.now()}`,
      position: entry.sets.length,
      setType: value.setType,
      reps: value.reps,
      weight: value.weight,
      rir: value.rir,
      volume: value.reps * value.weight,
      e1rm: null,
      loggedAt: new Date().toISOString(),
      notes: value.notes || null,
    };
    const optimisticSession: WorkoutSession = {
      ...session,
      exercises: session.exercises.map((candidate) =>
        candidate.id === entry.id
          ? { ...candidate, sets: [...candidate.sets, optimisticSet] }
          : candidate,
      ),
    };
    this.session.set(optimisticSession);
    try {
      await firstValueFrom(
        this.api.logSet(session.id, value.entryId, {
          reps: value.reps,
          weight: value.weight,
          rir: value.rir,
          setType: value.setType,
          notes: value.notes,
          targetSetId: value.targetSetId || null,
        }),
      );
      const updated = await firstValueFrom(this.api.get(session.id));
      this.session.set(updated);
      this.setForm.patchValue({
        reps: value.reps,
        weight: value.weight,
        rir: null,
        notes: '',
        setType: 'NORMAL',
        targetSetId: '',
      });
      const updatedEntry = updated.exercises.find((candidate) => candidate.id === value.entryId);
      if (updatedEntry) {
        this.startRest(this.nextRestSeconds(updatedEntry, updated));
      }
    } catch {
      this.session.set(session);
      this.error.set(true);
    } finally {
      this.logging.set(false);
    }
  }

  protected async finish(): Promise<void> {
    const session = this.session();
    if (!session || this.finishing() || this.totalSets() === 0) {
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
    const message = this.totalSets()
      ? 'La sesión tiene series registradas. ¿Descartarla de todos modos?'
      : '¿Descartar la sesión?';
    if (!window.confirm(message)) {
      return;
    }
    try {
      await firstValueFrom(this.api.discard(session.id));
      await this.router.navigateByUrl('/');
    } catch {
      this.error.set(true);
    }
  }

  protected async move(entryId: string, delta: number): Promise<void> {
    const session = this.session();
    if (!session) {
      return;
    }
    const current = session.exercises.find((entry) => entry.id === entryId);
    if (!current) {
      return;
    }
    await this.moveTo(entryId, current.position + delta);
  }

  protected startDrag(entryId: string): void {
    this.draggedEntry.set(entryId);
  }

  protected async dropEntry(target: SessionExercise): Promise<void> {
    const dragged = this.draggedEntry();
    this.draggedEntry.set(null);
    if (!dragged || dragged === target.id) {
      return;
    }
    await this.moveTo(dragged, target.position);
  }

  private async moveTo(entryId: string, target: number): Promise<void> {
    const session = this.session();
    if (!session || target < 0 || target >= session.exercises.length) {
      return;
    }
    try {
      this.session.set(
        await firstValueFrom(this.api.updateExercise(session.id, entryId, { position: target })),
      );
    } catch {
      this.error.set(true);
    }
  }

  protected toggleEntry(id: string): void {
    this.selectedEntries.update((current) => {
      const next = new Set(current);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  protected async groupSelected(): Promise<void> {
    const session = this.session();
    if (!session || this.selectedEntries().size < 2) {
      return;
    }
    const group =
      session.exercises.reduce((max, entry) => Math.max(max, entry.supersetGroup ?? 0), 0) + 1;
    try {
      this.session.set(
        await firstValueFrom(
          this.api.groupSuperset(session.id, Array.from(this.selectedEntries()), group),
        ),
      );
      this.selectedEntries.set(new Set());
    } catch {
      this.error.set(true);
    }
  }

  protected async ungroupSelected(): Promise<void> {
    const session = this.session();
    if (!session || !this.selectedEntries().size) {
      return;
    }
    try {
      this.session.set(
        await firstValueFrom(
          this.api.groupSuperset(session.id, Array.from(this.selectedEntries()), null),
        ),
      );
      this.selectedEntries.set(new Set());
    } catch {
      this.error.set(true);
    }
  }

  protected setTypeClass(type: string): string {
    switch (type) {
      case 'WARMUP':
        return 'bg-sky-950 text-sky-300';
      case 'FAILURE':
        return 'bg-red-950 text-red-300';
      case 'DROP':
        return 'bg-amber-950 text-amber-300';
      default:
        return 'bg-zinc-800 text-zinc-300';
    }
  }

  protected totalSets(): number {
    return this.session()?.exercises.reduce((total, entry) => total + entry.sets.length, 0) ?? 0;
  }

  protected formatDuration(seconds: number): string {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const rest = seconds % 60;
    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${rest.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${rest.toString().padStart(2, '0')}`;
  }

  protected supersetLabel(entry: SessionExercise): string {
    const session = this.session();
    if (!session || entry.supersetGroup === null) {
      return '';
    }
    const group = session.exercises
      .filter((candidate) => candidate.supersetGroup === entry.supersetGroup)
      .sort((a, b) => a.position - b.position);
    const index = group.findIndex((candidate) => candidate.id === entry.id);
    return `${String.fromCharCode(64 + entry.supersetGroup)}${index + 1}`;
  }

  protected bestSet(session: PreviousSession): PreviousSet | null {
    return session.sets.reduce<PreviousSet | null>((best, set) => {
      if (set.setType === 'WARMUP' || set.weight <= 0) {
        return best;
      }
      return !best || set.weight > best.weight ? set : best;
    }, null);
  }

  protected startSessionEdit(): void {
    const session = this.session();
    if (!session) {
      return;
    }
    this.sessionForm.setValue({ name: session.name, notes: session.notes ?? '' });
    this.editingSession.set(true);
  }

  protected cancelSessionEdit(): void {
    this.editingSession.set(false);
  }

  protected async saveSession(): Promise<void> {
    const session = this.session();
    if (!session || this.sessionForm.invalid) {
      return;
    }
    const value = this.sessionForm.getRawValue();
    try {
      this.session.set(
        await firstValueFrom(
          this.api.updateSession(session.id, { name: value.name, notes: value.notes }),
        ),
      );
      this.editingSession.set(false);
    } catch {
      this.error.set(true);
    }
  }

  protected useTarget(entry: SessionExercise, target: TargetSet): void {
    this.setForm.patchValue(
      {
        entryId: entry.id,
        reps: target.targetReps,
        weight: target.targetWeight ?? 0,
        rir: target.targetRir,
        setType: target.setType,
        notes: '',
        targetSetId: target.id,
      },
      { emitEvent: false },
    );
    void this.loadPrevious(entry.exerciseId);
    void this.loadPersonalRecords(entry.exerciseId);
  }

  protected changeRest(entryId: string, event: Event): void {
    const seconds = Number((event.target as HTMLInputElement).value);
    if (seconds >= 15 && seconds <= 600) {
      void this.updateEntry(entryId, { restSeconds: seconds });
    }
  }

  protected changeNotes(entryId: string, event: Event): void {
    const notes = (event.target as HTMLInputElement).value.trim();
    void this.updateEntry(entryId, { notes });
  }

  protected async updateEntry(
    entryId: string,
    payload: { restSeconds?: number | null; notes?: string | null },
  ): Promise<void> {
    const session = this.session();
    if (!session) {
      return;
    }
    try {
      this.session.set(await firstValueFrom(this.api.updateExercise(session.id, entryId, payload)));
    } catch {
      this.error.set(true);
    }
  }

  protected async deleteEntry(entry: SessionExercise): Promise<void> {
    const session = this.session();
    if (!session) {
      return;
    }
    if (entry.sets.length && !window.confirm('Este ejercicio tiene series. ¿Eliminarlo?')) {
      return;
    }
    try {
      this.session.set(await firstValueFrom(this.api.deleteExercise(session.id, entry.id)));
    } catch {
      this.error.set(true);
    }
  }

  protected startSetEdit(entryId: string, set: SessionSet): void {
    this.editSetForm.setValue({
      reps: set.reps,
      weight: set.weight,
      rir: set.rir,
      setType: set.setType,
      notes: set.notes ?? '',
    });
    this.editingSet.set({ entryId, set });
  }

  protected cancelSetEdit(): void {
    this.editingSet.set(null);
  }

  protected async saveSetEdit(): Promise<void> {
    const session = this.session();
    const editing = this.editingSet();
    if (!session || !editing || this.editSetForm.invalid) {
      return;
    }
    const value = this.editSetForm.getRawValue();
    try {
      this.session.set(
        await firstValueFrom(
          this.api.updateSet(session.id, editing.entryId, editing.set.id, {
            reps: value.reps,
            weight: value.weight,
            rir: value.rir,
            setType: value.setType,
            notes: value.notes,
          }),
        ),
      );
      this.editingSet.set(null);
    } catch {
      this.error.set(true);
    }
  }

  protected async deleteSet(entryId: string, set: SessionSet): Promise<void> {
    const session = this.session();
    if (!session || !window.confirm('¿Eliminar esta serie?')) {
      return;
    }
    try {
      this.session.set(await firstValueFrom(this.api.deleteSet(session.id, entryId, set.id)));
    } catch {
      this.error.set(true);
    }
  }

  private nextRestSeconds(entry: SessionExercise, session: WorkoutSession): number {
    const fallback = this.sessionStore.user()?.defaultRestSeconds ?? 90;
    if (entry.supersetGroup === null) {
      return entry.restSeconds ?? fallback;
    }
    const group = session.exercises
      .filter((candidate) => candidate.supersetGroup === entry.supersetGroup)
      .sort((a, b) => a.position - b.position);
    const index = group.findIndex((candidate) => candidate.id === entry.id);
    const next = group[(index + 1) % group.length];
    return next.restSeconds ?? entry.restSeconds ?? fallback;
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
    if (this.elapsedHandle) {
      clearInterval(this.elapsedHandle);
      this.elapsedHandle = null;
    }
  }

  private startElapsed(session: WorkoutSession): void {
    if (this.elapsedHandle) {
      clearInterval(this.elapsedHandle);
    }
    const started = new Date(session.startedAt).getTime();
    const tick = (): void => {
      this.elapsed.set(Math.max(0, Math.floor((Date.now() - started) / 1000)));
    };
    tick();
    this.elapsedHandle = setInterval(tick, 1000);
  }

  private async loadPersonalRecords(exerciseId: string): Promise<void> {
    if (this.personalRecords()[exerciseId] || this.prLoading.has(exerciseId)) {
      return;
    }
    this.prLoading.add(exerciseId);
    try {
      const detail = await firstValueFrom(this.catalog.detail(exerciseId));
      this.personalRecords.update((current) => ({
        ...current,
        [exerciseId]: detail.stats.personalRecords,
      }));
    } catch {
      // El PR actual es opcional; no rompe la sesión.
    } finally {
      this.prLoading.delete(exerciseId);
    }
  }

  private async loadPrevious(exerciseId: string): Promise<void> {
    if (this.previous()[exerciseId] || this.previousLoading.has(exerciseId)) {
      return;
    }
    this.previousLoading.add(exerciseId);
    try {
      const sessions = await firstValueFrom(this.api.previous(exerciseId));
      this.previous.update((current) => ({ ...current, [exerciseId]: sessions }));
    } catch {
      // La performance anterior es opcional; no rompe la sesión.
    } finally {
      this.previousLoading.delete(exerciseId);
    }
  }

  private startRest(seconds: number): void {
    this.stopRest();
    this.restRemaining.set(seconds);
    this.restHandle = setInterval(() => {
      const current = this.restRemaining();
      if (current === null || current <= 1) {
        this.stopRest();
        this.restRemaining.set(null);
        this.notifyRestFinished();
      } else {
        this.restRemaining.set(current - 1);
      }
    }, 1000);
  }

  private notifyRestFinished(): void {
    navigator.vibrate?.(200);
    if (this.sessionStore.user()?.timerSound === false) {
      return;
    }
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification('Descanso terminado', { body: 'Siguiente serie' });
    }
    try {
      const audio = new AudioContext();
      const oscillator = audio.createOscillator();
      oscillator.frequency.value = 880;
      oscillator.connect(audio.destination);
      oscillator.start();
      oscillator.stop(audio.currentTime + 0.15);
    } catch {
      // Sin audio disponible; el temporizador sigue funcionando.
    }
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
      this.startElapsed(session);
      if (session.exercises.length) {
        this.setForm.patchValue({ entryId: session.exercises[0].id });
        void this.loadPrevious(session.exercises[0].exerciseId);
        void this.loadPersonalRecords(session.exercises[0].exerciseId);
      }
    } catch {
      this.error.set(true);
    }
  }
}
