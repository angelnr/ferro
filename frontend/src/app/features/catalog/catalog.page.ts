import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { debounceTime, distinctUntilChanged, firstValueFrom } from 'rxjs';
import { CatalogApi } from './catalog.api';
import { Exercise } from './catalog.models';

const MUSCLES = [
  'CHEST',
  'BACK',
  'SHOULDERS',
  'BICEPS',
  'TRICEPS',
  'QUADS',
  'HAMSTRINGS',
  'GLUTES',
  'CALVES',
  'CORE',
  'FOREARMS',
  'NECK',
  'FULL_BODY',
];

const EQUIPMENT = [
  'BARBELL',
  'DUMBBELL',
  'MACHINE',
  'CABLE',
  'BODYWEIGHT',
  'KETTLEBELL',
  'BAND',
  'OTHER',
];

@Component({
  selector: 'app-catalog-page',
  imports: [ReactiveFormsModule, TranslatePipe],
  template: `
    <section class="mt-8 space-y-6">
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p class="text-sm uppercase tracking-wider text-emerald-400">
            {{ 'catalog.title' | translate }}
          </p>
          <h1 class="mt-1 text-3xl font-black">{{ 'catalog.heading' | translate }}</h1>
          <p class="mt-1 text-sm text-zinc-400">{{ 'catalog.subtitle' | translate }}</p>
        </div>
        <button
          type="button"
          (click)="showCreate.set(!showCreate())"
          class="rounded-lg border border-emerald-700 px-3 py-2 text-sm font-semibold text-emerald-300 hover:border-emerald-500"
        >
          {{ 'catalog.create' | translate }}
        </button>
      </div>

      <form
        class="grid gap-3 rounded-xl border border-zinc-800 bg-zinc-900/60 p-4 sm:grid-cols-3"
        [formGroup]="filters"
      >
        <label class="block text-sm text-zinc-300 sm:col-span-1" for="catalogSearch">
          {{ 'catalog.search' | translate }}
          <input
            id="catalogSearch"
            type="search"
            formControlName="q"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-emerald-500"
          />
        </label>
        <label class="block text-sm text-zinc-300" for="catalogMuscle">
          {{ 'catalog.muscle' | translate }}
          <select
            id="catalogMuscle"
            formControlName="muscle"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
          >
            <option value="">{{ 'catalog.all' | translate }}</option>
            @for (muscle of muscles; track muscle) {
              <option [value]="muscle">{{ muscle }}</option>
            }
          </select>
        </label>
        <label class="block text-sm text-zinc-300" for="catalogEquipment">
          {{ 'catalog.equipment' | translate }}
          <select
            id="catalogEquipment"
            formControlName="equipment"
            class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
          >
            <option value="">{{ 'catalog.all' | translate }}</option>
            @for (item of equipment; track item) {
              <option [value]="item">{{ item }}</option>
            }
          </select>
        </label>
      </form>

      @if (showCreate()) {
        <form
          class="grid gap-3 rounded-xl border border-emerald-900 bg-emerald-950/20 p-4 sm:grid-cols-2"
          [formGroup]="createForm"
          (ngSubmit)="create()"
        >
          <label class="text-sm text-zinc-300" for="newNameEs">
            {{ 'catalog.nameEs' | translate }}
            <input
              id="newNameEs"
              formControlName="nameEs"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
            />
          </label>
          <label class="text-sm text-zinc-300" for="newNameEn">
            {{ 'catalog.nameEn' | translate }}
            <input
              id="newNameEn"
              formControlName="nameEn"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
            />
          </label>
          <label class="text-sm text-zinc-300" for="newMuscle">
            {{ 'catalog.muscle' | translate }}
            <select
              id="newMuscle"
              formControlName="muscleGroup"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
            >
              @for (muscle of muscles; track muscle) {
                <option [value]="muscle">{{ muscle }}</option>
              }
            </select>
          </label>
          <label class="text-sm text-zinc-300" for="newEquipment">
            {{ 'catalog.equipment' | translate }}
            <select
              id="newEquipment"
              formControlName="equipment"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
            >
              @for (item of equipment; track item) {
                <option [value]="item">{{ item }}</option>
              }
            </select>
          </label>
          <label class="text-sm text-zinc-300" for="newMechanics">
            {{ 'catalog.mechanics' | translate }}
            <select
              id="newMechanics"
              formControlName="mechanics"
              class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2"
            >
              <option value="COMPOUND">COMPOUND</option>
              <option value="ISOLATION">ISOLATION</option>
            </select>
          </label>
          <label class="flex items-center gap-2 text-sm text-zinc-300">
            <input type="checkbox" formControlName="bodyweight" class="accent-emerald-500" />
            {{ 'catalog.bodyweight' | translate }}
          </label>
          @if (createError(); as error) {
            <p class="text-sm text-red-400 sm:col-span-2">{{ error }}</p>
          }
          <button
            type="submit"
            [disabled]="createForm.invalid || creating()"
            class="rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white disabled:opacity-50 sm:col-span-2"
          >
            {{ 'catalog.save' | translate }}
          </button>
        </form>
      }

      @if (error(); as message) {
        <p class="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2 text-sm text-red-300">
          {{ message }}
        </p>
      }

      @if (loading()) {
        <p class="text-zinc-400">{{ 'auth.loading' | translate }}</p>
      } @else {
        <p class="text-sm text-zinc-500">{{ 'catalog.results' | translate: { count: total() } }}</p>
        <ul class="grid gap-3 sm:grid-cols-2">
          @for (exercise of exercises(); track exercise.id) {
            <li class="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="font-semibold">{{ exercise.name }}</p>
                  <p class="text-sm text-zinc-500">{{ exercise.nameEn }}</p>
                </div>
                @if (exercise.mine) {
                  <span class="rounded bg-emerald-950 px-2 py-0.5 text-xs text-emerald-300">{{
                    'catalog.mine' | translate
                  }}</span>
                }
              </div>
              <p class="mt-3 text-xs uppercase tracking-wider text-zinc-500">
                {{ exercise.muscleGroup }} · {{ exercise.equipment }} · {{ exercise.mechanics }}
              </p>
            </li>
          } @empty {
            <li class="text-sm text-zinc-500">{{ 'catalog.empty' | translate }}</li>
          }
        </ul>
      }
    </section>
  `,
})
export class CatalogPage {
  private readonly api = inject(CatalogApi);
  private readonly formBuilder = inject(FormBuilder);
  protected readonly muscles = MUSCLES;
  protected readonly equipment = EQUIPMENT;
  protected readonly exercises = signal<Exercise[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly showCreate = signal(false);
  protected readonly creating = signal(false);
  protected readonly createError = signal<string | null>(null);

  protected readonly filters = this.formBuilder.nonNullable.group({
    q: [''],
    muscle: [''],
    equipment: [''],
  });

  protected readonly createForm = this.formBuilder.nonNullable.group({
    nameEs: ['', [Validators.required, Validators.maxLength(120)]],
    nameEn: [''],
    muscleGroup: ['CHEST', [Validators.required]],
    equipment: ['BARBELL', [Validators.required]],
    mechanics: ['COMPOUND', [Validators.required]],
    bodyweight: [false],
  });

  constructor() {
    this.filters.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => void this.load());
    void this.load();
  }

  protected async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const value = this.filters.getRawValue();
      const page = await firstValueFrom(this.api.search(value.q, value.muscle, value.equipment));
      this.exercises.set(page.content);
      this.total.set(page.totalElements);
    } catch {
      this.error.set('No se pudo cargar el catálogo');
    } finally {
      this.loading.set(false);
    }
  }

  protected async create(): Promise<void> {
    if (this.createForm.invalid || this.creating()) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.creating.set(true);
    this.createError.set(null);
    try {
      const value = this.createForm.getRawValue();
      await firstValueFrom(
        this.api.create({
          nameEs: value.nameEs,
          nameEn: value.nameEn || undefined,
          muscleGroup: value.muscleGroup,
          equipment: value.equipment,
          mechanics: value.mechanics,
          bodyweight: value.bodyweight,
        }),
      );
      this.createForm.reset({
        nameEs: '',
        nameEn: '',
        muscleGroup: 'CHEST',
        equipment: 'BARBELL',
        mechanics: 'COMPOUND',
        bodyweight: false,
      });
      this.showCreate.set(false);
      await this.load();
    } catch {
      this.createError.set('No se pudo crear el ejercicio');
    } finally {
      this.creating.set(false);
    }
  }
}
