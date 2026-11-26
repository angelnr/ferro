import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { TrainingApi } from '../training/training.api';
import { ProgressApi } from './progress.api';
import { SessionDetail, SessionExerciseDetail, SessionSetDetail } from './progress.models';

@Component({
  selector: 'app-session-detail-page',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, TranslatePipe],
  template: `
    <section class="mx-auto mt-8 max-w-4xl space-y-6">
      <a routerLink="/history" class="text-sm text-emerald-400">{{
        'progress.backHistory' | translate
      }}</a>

      @if (session(); as current) {
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            @if (editing()) {
              <div class="space-y-2">
                <input
                  [value]="editName()"
                  (input)="editName.set(value($event))"
                  maxlength="80"
                  class="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
                  [attr.aria-label]="'routines.name' | translate"
                />
                <input
                  [value]="editNotes()"
                  (input)="editNotes.set(value($event))"
                  maxlength="500"
                  class="w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
                  [attr.aria-label]="'training.notes' | translate"
                />
              </div>
            } @else {
              <h1 class="text-3xl font-black">{{ current.name }}</h1>
              @if (current.notes) {
                <p class="text-sm text-zinc-400">{{ current.notes }}</p>
              }
            }
            <p class="mt-1 text-sm text-zinc-500">
              {{ current.finishedAt | date: 'medium' }} · {{ sessionDuration() }} ·
              {{ current.totalVolume }} kg
              @if (current.status === 'AUTO_CLOSED') {
                · {{ 'progress.autoClosed' | translate }}
              }
            </p>
          </div>
          <div class="flex flex-wrap gap-2">
            @if (editing()) {
              <button
                type="button"
                (click)="saveSession()"
                class="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white"
              >
                {{ 'training.save' | translate }}
              </button>
              <button
                type="button"
                (click)="editing.set(false)"
                class="rounded-lg border border-zinc-700 px-4 py-2 text-sm"
              >
                {{ 'training.cancel' | translate }}
              </button>
            } @else {
              <button
                type="button"
                (click)="startEdit()"
                class="rounded-lg border border-zinc-700 px-4 py-2 text-sm"
              >
                {{ 'progress.edit' | translate }}
              </button>
              <button
                type="button"
                (click)="removeSession()"
                class="rounded-lg border border-red-900 px-4 py-2 text-sm text-red-300"
              >
                {{ 'progress.delete' | translate }}
              </button>
            }
          </div>
        </div>

        @if (error()) {
          <p class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
            {{ 'progress.error' | translate }}
          </p>
        }

        <div class="space-y-4">
          @for (exercise of current.exercises; track exercise.id) {
            <article class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <h2 class="font-semibold">{{ exercise.name }}</h2>
                <span class="text-sm text-zinc-500">{{ exercise.volume }} kg</span>
              </div>
              @if (exercise.notes) {
                <p class="mt-1 text-xs text-zinc-500">{{ exercise.notes }}</p>
              }
              @if (exercise.personalRecords.length) {
                <p class="mt-1 text-xs text-emerald-400">
                  @for (pr of exercise.personalRecords; track pr.type) {
                    <span class="mr-2">PR {{ pr.type }}: {{ pr.value }} kg</span>
                  }
                </p>
              }
              <ul class="mt-3 space-y-2 text-sm">
                @for (set of exercise.sets; track set.id) {
                  <li class="rounded-lg border border-zinc-800 px-3 py-2">
                    @if (editingSet()?.id === set.id) {
                      <form
                        class="grid gap-2 sm:grid-cols-6"
                        [formGroup]="setForm"
                        (ngSubmit)="saveSet(exercise, set)"
                      >
                        <label class="text-xs text-zinc-400" for="historyReps">
                          {{ 'training.reps' | translate }}
                          <input
                            id="historyReps"
                            type="number"
                            min="1"
                            formControlName="reps"
                            class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                          />
                        </label>
                        <label class="text-xs text-zinc-400" for="historyWeight">
                          {{ 'training.weight' | translate }}
                          <input
                            id="historyWeight"
                            type="number"
                            step="0.5"
                            formControlName="weight"
                            class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                          />
                        </label>
                        <label class="text-xs text-zinc-400" for="historyRir">
                          RIR
                          <input
                            id="historyRir"
                            type="number"
                            min="0"
                            max="10"
                            formControlName="rir"
                            class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                          />
                        </label>
                        <label class="text-xs text-zinc-400" for="historyType">
                          {{ 'training.setType' | translate }}
                          <select
                            id="historyType"
                            formControlName="setType"
                            class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                          >
                            <option value="NORMAL">NORMAL</option>
                            <option value="WARMUP">WARMUP</option>
                            <option value="FAILURE">FAILURE</option>
                            <option value="DROP">DROP</option>
                          </select>
                        </label>
                        <label class="text-xs text-zinc-400 sm:col-span-2" for="historyNotes">
                          {{ 'training.notes' | translate }}
                          <input
                            id="historyNotes"
                            maxlength="500"
                            formControlName="notes"
                            class="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
                          />
                        </label>
                        <div class="flex gap-2 sm:col-span-6">
                          <button
                            type="submit"
                            [disabled]="setForm.invalid"
                            class="rounded bg-emerald-600 px-3 py-1 text-xs font-semibold text-white disabled:opacity-50"
                          >
                            {{ 'training.save' | translate }}
                          </button>
                          <button
                            type="button"
                            (click)="editingSet.set(null)"
                            class="rounded border border-zinc-700 px-3 py-1 text-xs"
                          >
                            {{ 'training.cancel' | translate }}
                          </button>
                        </div>
                      </form>
                    } @else {
                      <div class="flex flex-wrap items-center justify-between gap-2">
                        <div>
                          <span>{{ set.setType }} · {{ set.reps }} × {{ set.weight }} kg</span>
                          @if (set.notes) {
                            <p class="text-xs text-zinc-500">{{ set.notes }}</p>
                          }
                        </div>
                        <div class="flex items-center gap-2">
                          <span class="text-zinc-500">{{ set.volume }} kg</span>
                          <button
                            type="button"
                            (click)="startSetEdit(exercise, set)"
                            class="rounded border border-zinc-700 px-2 py-0.5 text-xs"
                          >
                            {{ 'progress.edit' | translate }}
                          </button>
                          <button
                            type="button"
                            (click)="removeSet(exercise, set)"
                            class="rounded border border-red-900 px-2 py-0.5 text-xs text-red-300"
                          >
                            {{ 'progress.delete' | translate }}
                          </button>
                        </div>
                      </div>
                    }
                  </li>
                }
              </ul>
            </article>
          }
        </div>
      } @else if (error()) {
        <p class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
          {{ 'progress.error' | translate }}
        </p>
      } @else {
        <p class="text-zinc-400">{{ 'auth.loading' | translate }}</p>
      }
    </section>
  `,
})
export class SessionDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly progress = inject(ProgressApi);
  private readonly training = inject(TrainingApi);
  private readonly formBuilder = inject(FormBuilder);
  protected readonly session = signal<SessionDetail | null>(null);
  protected readonly error = signal(false);
  protected readonly editing = signal(false);
  protected readonly editName = signal('');
  protected readonly editNotes = signal('');
  protected readonly editingSet = signal<SessionSetDetail | null>(null);
  protected readonly setForm = this.formBuilder.nonNullable.group({
    reps: [8, [Validators.required, Validators.min(1), Validators.max(1000)]],
    weight: [20, [Validators.required]],
    rir: [null as number | null, [Validators.min(0), Validators.max(10)]],
    setType: ['NORMAL', [Validators.required]],
    notes: [''],
  });

  async ngOnInit(): Promise<void> {
    await this.load();
  }

  protected value(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  protected sessionDuration(): string {
    const seconds = this.session()?.durationSeconds ?? 0;
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    return hours ? `${hours} h ${minutes} min` : `${minutes} min`;
  }

  protected startEdit(): void {
    const current = this.session();
    if (current) {
      this.editName.set(current.name);
      this.editNotes.set(current.notes ?? '');
      this.editing.set(true);
    }
  }

  protected async saveSession(): Promise<void> {
    const current = this.session();
    if (!current) {
      return;
    }
    try {
      await firstValueFrom(
        this.training.updateSession(current.id, {
          name: this.editName(),
          notes: this.editNotes(),
        }),
      );
      this.editing.set(false);
      await this.load();
    } catch {
      this.error.set(true);
    }
  }

  protected startSetEdit(exercise: SessionExerciseDetail, set: SessionSetDetail): void {
    this.setForm.setValue({
      reps: set.reps,
      weight: set.weight,
      rir: set.rir,
      setType: set.setType,
      notes: set.notes ?? '',
    });
    this.editingSet.set(set);
  }

  protected async saveSet(exercise: SessionExerciseDetail, set: SessionSetDetail): Promise<void> {
    const current = this.session();
    if (!current || this.setForm.invalid) {
      return;
    }
    const value = this.setForm.getRawValue();
    try {
      await firstValueFrom(
        this.training.updateSet(current.id, exercise.id, set.id, {
          reps: value.reps,
          weight: value.weight,
          rir: value.rir,
          setType: value.setType,
          notes: value.notes,
        }),
      );
      this.editingSet.set(null);
      await this.load();
    } catch {
      this.error.set(true);
    }
  }

  protected async removeSet(exercise: SessionExerciseDetail, set: SessionSetDetail): Promise<void> {
    const current = this.session();
    if (!current || !window.confirm('¿Eliminar esta serie?')) {
      return;
    }
    try {
      await firstValueFrom(this.training.deleteSet(current.id, exercise.id, set.id));
      await this.load();
    } catch {
      this.error.set(true);
    }
  }

  protected async removeSession(): Promise<void> {
    const current = this.session();
    if (!current || !window.confirm('¿Eliminar esta sesión del historial?')) {
      return;
    }
    try {
      await firstValueFrom(this.progress.deleteSession(current.id));
      await this.router.navigateByUrl('/history');
    } catch {
      this.error.set(true);
    }
  }

  private async load(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set(true);
      return;
    }
    try {
      this.session.set(await firstValueFrom(this.progress.session(id)));
    } catch {
      this.error.set(true);
    }
  }
}
