import { Component, forwardRef, inject, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ControlValueAccessor,
  FormControl,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
} from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { debounceTime, distinctUntilChanged, firstValueFrom } from 'rxjs';
import { CatalogApi } from '../catalog.api';
import { Exercise } from '../catalog.models';
import { CatalogLabelPipe } from '../catalog-label.pipe';
import { EQUIPMENT, MUSCLES } from '../catalog.options';

@Component({
  selector: 'app-exercise-picker',
  imports: [ReactiveFormsModule, TranslatePipe, CatalogLabelPipe],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ExercisePicker),
      multi: true,
    },
  ],
  template: `
    <div class="relative">
      <label class="block text-sm font-medium text-zinc-300" for="exercisePicker">
        {{ 'catalog.pick' | translate }}
        <input
          id="exercisePicker"
          type="search"
          role="combobox"
          aria-autocomplete="list"
          aria-controls="exercisePickerList"
          [attr.aria-expanded]="open()"
          [attr.aria-activedescendant]="
            open() && results().length ? 'exercise-option-' + activeIndex() : null
          "
          [formControl]="query"
          (focus)="open.set(true)"
          (keydown)="onKeydown($event)"
          class="mt-1 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100 outline-none focus:border-emerald-500"
          autocomplete="off"
        />
      </label>

      <div class="mt-2 grid grid-cols-2 gap-2">
        <select
          [value]="muscle()"
          (change)="setMuscle($event)"
          class="rounded-lg border border-zinc-700 bg-zinc-950 px-2 py-1 text-xs text-zinc-300"
          [attr.aria-label]="'catalog.muscle' | translate"
        >
          <option value="">
            {{ 'catalog.muscle' | translate }}: {{ 'catalog.all' | translate }}
          </option>
          @for (item of muscles; track item) {
            <option [value]="item">{{ item | catalogLabel: 'muscle' }}</option>
          }
        </select>
        <select
          [value]="equipment()"
          (change)="setEquipment($event)"
          class="rounded-lg border border-zinc-700 bg-zinc-950 px-2 py-1 text-xs text-zinc-300"
          [attr.aria-label]="'catalog.equipment' | translate"
        >
          <option value="">
            {{ 'catalog.equipment' | translate }}: {{ 'catalog.all' | translate }}
          </option>
          @for (item of equipmentOptions; track item) {
            <option [value]="item">{{ item | catalogLabel: 'equipment' }}</option>
          }
        </select>
      </div>

      @if (open() && (results().length || query.value.trim())) {
        <ul
          id="exercisePickerList"
          role="listbox"
          class="absolute z-20 mt-1 max-h-72 w-full overflow-auto rounded-lg border border-zinc-700 bg-zinc-900 py-1 shadow-xl"
        >
          @for (exercise of results(); track exercise.id; let index = $index) {
            <li
              [id]="'exercise-option-' + index"
              role="option"
              [attr.aria-selected]="index === activeIndex()"
              (mousedown)="select(exercise)"
              class="cursor-pointer px-3 py-2 text-sm hover:bg-zinc-800"
              [class.bg-zinc-800]="index === activeIndex()"
            >
              <span class="font-medium">{{ exercise.name }}</span>
              <span class="ml-2 text-xs uppercase tracking-wider text-zinc-500">
                {{ exercise.muscleGroup | catalogLabel: 'muscle' }} ·
                {{ exercise.equipment | catalogLabel: 'equipment' }}
              </span>
            </li>
          }
          @if (!results().length && query.value.trim()) {
            <li class="border-t border-zinc-800 px-3 py-2">
              <button
                type="button"
                (mousedown)="requestCreate()"
                class="text-sm text-emerald-400 hover:text-emerald-300"
              >
                {{ 'catalog.createNamed' | translate: { name: query.value.trim() } }}
              </button>
            </li>
          }
        </ul>
      }
    </div>
  `,
})
export class ExercisePicker implements ControlValueAccessor {
  private readonly api = inject(CatalogApi);
  private onChange: (value: Exercise | null) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  protected readonly query = new FormControl('', { nonNullable: true });
  protected readonly results = signal<Exercise[]>([]);
  protected readonly open = signal(false);
  protected readonly activeIndex = signal(0);
  protected readonly muscle = signal('');
  protected readonly equipment = signal('');
  protected readonly muscles = MUSCLES;
  protected readonly equipmentOptions = EQUIPMENT;
  readonly selectedExercise = output<Exercise>();
  readonly createRequested = output<string>();

  constructor() {
    this.query.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((value) => void this.search(value));
  }

  writeValue(value: Exercise | null): void {
    this.query.setValue(value?.name ?? '', { emitEvent: false });
  }

  registerOnChange(fn: (value: Exercise | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.query.disable({ emitEvent: false });
    } else {
      this.query.enable({ emitEvent: false });
    }
  }

  protected onKeydown(event: KeyboardEvent): void {
    const items = this.results();
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.open.set(true);
      this.activeIndex.set(Math.min(this.activeIndex() + 1, Math.max(items.length - 1, 0)));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.activeIndex.set(Math.max(this.activeIndex() - 1, 0));
    } else if (event.key === 'Enter' && this.open() && items.length) {
      event.preventDefault();
      this.select(items[this.activeIndex()]);
    } else if (event.key === 'Escape') {
      this.open.set(false);
    }
  }

  protected setMuscle(event: Event): void {
    this.muscle.set((event.target as HTMLSelectElement).value);
    void this.search(this.query.value);
  }

  protected setEquipment(event: Event): void {
    this.equipment.set((event.target as HTMLSelectElement).value);
    void this.search(this.query.value);
  }

  protected select(exercise: Exercise): void {
    this.query.setValue('', { emitEvent: false });
    this.open.set(false);
    this.onChange(exercise);
    this.onTouched();
    this.selectedExercise.emit(exercise);
  }

  protected requestCreate(): void {
    this.createRequested.emit(this.query.value.trim());
    this.open.set(false);
  }

  private async search(value: string): Promise<void> {
    if (!value.trim() && !this.muscle() && !this.equipment()) {
      this.results.set([]);
      this.open.set(false);
      return;
    }
    try {
      const page = await firstValueFrom(this.api.search(value, this.muscle(), this.equipment()));
      this.results.set(page.content);
      this.activeIndex.set(0);
      this.open.set(true);
    } catch {
      this.results.set([]);
    }
  }
}
