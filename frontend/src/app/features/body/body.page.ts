import { DatePipe, DecimalPipe } from '@angular/common';
import { LengthPipe, WeightPipe } from '../../core/units/units.pipes';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { BodyApi } from './body.api';
import { BodyMeasurement, BodyPayload, BodyStats } from './body.models';

type Field = 'weight' | 'chest' | 'waist' | 'hip' | 'arm' | 'thigh';

@Component({
  selector: 'app-body-page',
  imports: [
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
    WeightPipe,
    LengthPipe,
  ],
  template: `
    <section class="mx-auto mt-8 max-w-4xl space-y-6">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p class="text-sm uppercase tracking-wider text-emerald-400">
            {{ 'body.title' | translate }}
          </p>
          <h1 class="mt-1 text-3xl font-black">{{ 'body.measurements' | translate }}</h1>
        </div>
        <a routerLink="/history" class="text-sm text-emerald-400">{{
          'progress.backHistory' | translate
        }}</a>
      </div>

      @if (error()) {
        <p class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
          {{ errorMessage() ?? ('body.error' | translate) }}
        </p>
      }

      <form
        class="grid gap-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-4 sm:grid-cols-4"
        [formGroup]="form"
        (ngSubmit)="save()"
      >
        <label class="text-sm text-zinc-300" for="bodyDate">
          {{ 'body.date' | translate }}
          <input
            id="bodyDate"
            type="date"
            formControlName="date"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
          />
        </label>
        @for (field of fields; track field) {
          <label class="text-sm text-zinc-300" [attr.for]="'body-' + field">
            {{ 'body.' + field | translate }}
            <input
              [id]="'body-' + field"
              type="number"
              step="0.1"
              [formControlName]="field"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
            />
          </label>
        }
        <label class="text-sm text-zinc-300 sm:col-span-3" for="bodyNotes">
          {{ 'training.notes' | translate }}
          <input
            id="bodyNotes"
            formControlName="notes"
            maxlength="500"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
          />
        </label>
        <div class="flex items-end gap-2">
          <button
            type="submit"
            [disabled]="form.invalid || saving()"
            class="rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white disabled:opacity-50"
          >
            {{ (editingId() ? 'training.save' : 'body.add') | translate }}
          </button>
          @if (editingId()) {
            <button
              type="button"
              (click)="cancelEdit()"
              class="rounded-lg border border-zinc-700 px-3 py-2 text-sm"
            >
              {{ 'training.cancel' | translate }}
            </button>
          }
        </div>
      </form>

      <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <p class="text-xs uppercase tracking-wider text-zinc-500">
            {{ 'body.evolution' | translate }}
          </p>
          <div class="flex flex-wrap gap-1">
            @for (option of fieldOptions; track option) {
              <button
                type="button"
                (click)="field.set(option)"
                [class.bg-emerald-600]="field() === option"
                class="rounded border border-zinc-700 px-2 py-0.5 text-xs"
              >
                {{ 'body.' + option | translate }}
              </button>
            }
          </div>
          <div class="flex gap-1">
            @for (option of rangeOptions; track option) {
              <button
                type="button"
                (click)="loadStats(option)"
                [class.bg-sky-700]="months() === option"
                class="rounded border border-zinc-700 px-2 py-0.5 text-xs"
              >
                {{ option }}m
              </button>
            }
          </div>
        </div>
        @if (points().length) {
          <svg
            viewBox="0 0 100 40"
            class="mt-3 h-40 w-full"
            role="img"
            [attr.aria-label]="'body.' + field() | translate"
          >
            <polyline
              [attr.points]="points()"
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
            <span>{{ points().length }} {{ 'body.points' | translate }}</span>
            <span>
              {{ firstValue() | number: '1.0-1' }} → {{ lastValue() | number: '1.0-1' }}
              {{ field() === 'weight' ? ('weight' | translate) : 'cm' }}
            </span>
          </div>
        } @else {
          <p class="mt-3 text-sm text-zinc-500">{{ 'body.noStats' | translate }}</p>
        }
      </div>

      <div class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
        <p class="text-xs uppercase tracking-wider text-zinc-500">
          {{ 'body.recent' | translate }}
        </p>
        @if (!measurements().length) {
          <p class="mt-3 text-sm text-zinc-500">{{ 'body.empty' | translate }}</p>
        }
        <ul class="mt-2 divide-y divide-zinc-800 text-sm">
          @for (measurement of measurements(); track measurement.id) {
            <li class="py-2">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <p class="font-medium">{{ measurement.measuredAt | date: 'mediumDate' }}</p>
                  <p class="text-xs text-zinc-500">
                    @if (measurement.weight !== null) {
                      {{ measurement.weight | weight }}
                    }
                    @for (fieldName of fields; track fieldName) {
                      @if (measurement[fieldName] !== null && fieldName !== 'weight') {
                        · {{ fieldName }} {{ measurement[fieldName] | length }}
                      }
                    }
                  </p>
                </div>
                <div class="flex gap-2">
                  <button
                    type="button"
                    (click)="edit(measurement)"
                    class="rounded border border-zinc-700 px-2 py-0.5 text-xs"
                  >
                    {{ 'progress.edit' | translate }}
                  </button>
                  <button
                    type="button"
                    (click)="remove(measurement)"
                    class="rounded border border-red-900 px-2 py-0.5 text-xs text-red-300"
                  >
                    {{ 'progress.delete' | translate }}
                  </button>
                </div>
              </div>
              @if (measurement.notes) {
                <p class="text-xs text-zinc-500">{{ measurement.notes }}</p>
              }
            </li>
          }
        </ul>
      </div>
    </section>
  `,
})
export class BodyPage implements OnInit {
  private readonly api = inject(BodyApi);
  private readonly formBuilder = inject(FormBuilder);
  protected readonly fields: Field[] = ['weight', 'chest', 'waist', 'hip', 'arm', 'thigh'];
  protected readonly fieldOptions = this.fields;
  protected readonly rangeOptions = [1, 3, 6, 12];
  protected readonly measurements = signal<BodyMeasurement[]>([]);
  protected readonly stats = signal<BodyStats | null>(null);
  protected readonly field = signal<Field>('weight');
  protected readonly months = signal(3);
  protected readonly editingId = signal<string | null>(null);
  protected readonly error = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly form = this.formBuilder.group({
    date: [new Date().toISOString().slice(0, 10), [Validators.required]],
    weight: [null as number | null],
    chest: [null as number | null],
    waist: [null as number | null],
    hip: [null as number | null],
    arm: [null as number | null],
    thigh: [null as number | null],
    notes: [''],
  });

  async ngOnInit(): Promise<void> {
    await Promise.all([this.load(), this.loadStats(3)]);
  }

  protected async save(): Promise<void> {
    if (this.form.invalid || this.saving()) {
      return;
    }
    const payload = this.payload();
    if (!this.fields.some((field) => this.form.getRawValue()[field] !== null)) {
      this.error.set(true);
      this.errorMessage.set('Registra al menos un peso o perímetro');
      return;
    }
    this.saving.set(true);
    this.error.set(false);
    this.errorMessage.set(null);
    try {
      const editing = this.editingId();
      if (editing) {
        await firstValueFrom(this.api.update(editing, payload));
      } else {
        try {
          await firstValueFrom(this.api.create(payload));
        } catch (error) {
          const existingId =
            error instanceof HttpErrorResponse
              ? (error.error as { measurementId?: string } | null)?.measurementId
              : undefined;
          if (existingId && window.confirm('Ya hay una medición ese día. ¿Sobrescribirla?')) {
            await firstValueFrom(this.api.update(existingId, payload));
          } else {
            throw error;
          }
        }
      }
      this.cancelEdit();
      await Promise.all([this.load(), this.loadStats(this.months())]);
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

  protected edit(measurement: BodyMeasurement): void {
    this.editingId.set(measurement.id);
    this.form.setValue({
      date: measurement.measuredAt.slice(0, 10),
      weight: measurement.weight,
      chest: measurement.chest,
      waist: measurement.waist,
      hip: measurement.hip,
      arm: measurement.arm,
      thigh: measurement.thigh,
      notes: measurement.notes ?? '',
    });
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
    this.form.reset({
      date: new Date().toISOString().slice(0, 10),
      weight: null,
      chest: null,
      waist: null,
      hip: null,
      arm: null,
      thigh: null,
      notes: '',
    });
  }

  protected async remove(measurement: BodyMeasurement): Promise<void> {
    if (!window.confirm('¿Eliminar esta medición?')) {
      return;
    }
    try {
      await firstValueFrom(this.api.delete(measurement.id));
      await Promise.all([this.load(), this.loadStats(this.months())]);
    } catch {
      this.error.set(true);
    }
  }

  protected async loadStats(months: number): Promise<void> {
    this.months.set(months);
    try {
      this.stats.set(await firstValueFrom(this.api.stats(months)));
    } catch {
      this.error.set(true);
    }
  }

  protected points(): string {
    const series = this.stats()?.[this.field()] ?? [];
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
    const series = this.stats()?.[this.field()] ?? [];
    const values = series.map((point) => point.value);
    const min = values.length ? Math.min(...values) : 0;
    const max = values.length ? Math.max(...values) : 1;
    return series.map((point, index) => ({
      x: series.length > 1 ? (index / (series.length - 1)) * 100 : 50,
      y: max === min ? 20 : 38 - ((point.value - min) / (max - min)) * 36,
      date: point.date,
    }));
  }

  protected firstValue(): number {
    return this.stats()?.[this.field()]?.[0]?.value ?? 0;
  }

  protected lastValue(): number {
    const series = this.stats()?.[this.field()] ?? [];
    return series[series.length - 1]?.value ?? 0;
  }

  private async load(): Promise<void> {
    try {
      this.measurements.set((await firstValueFrom(this.api.list())).content);
    } catch {
      this.error.set(true);
    }
  }

  private payload(): BodyPayload {
    const value = this.form.getRawValue();
    return {
      measuredAt: new Date(`${value.date}T12:00:00`).toISOString(),
      weight: value.weight,
      chest: value.chest,
      waist: value.waist,
      hip: value.hip,
      arm: value.arm,
      thigh: value.thigh,
      notes: value.notes?.trim() || null,
    };
  }
}
