import { Component, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { CatalogApi } from '../catalog/catalog.api';
import { RoutineApi } from '../training/routine.api';
import { SessionStore } from '../../core/auth/session.store';

const KEY = 'ferro.onboarding.seen';

@Component({
  selector: 'app-onboarding',
  imports: [TranslatePipe],
  template: `
    @if (visible()) {
      <div
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="'onboarding.title' | translate"
      >
        <section
          class="w-full max-w-lg rounded-2xl border border-zinc-700 bg-zinc-950 p-6 shadow-2xl"
        >
          <p class="text-xs uppercase tracking-wider text-emerald-400">
            {{ 'onboarding.title' | translate }}
          </p>
          <h2 class="mt-2 text-2xl font-black">{{ card().title | translate }}</h2>
          <p class="mt-2 text-sm text-zinc-400">{{ card().body | translate }}</p>

          <div class="mt-6 flex items-center justify-between">
            <div class="flex gap-1">
              @for (item of cards; track item.title; let index = $index) {
                <span
                  class="h-1.5 w-6 rounded"
                  [class.bg-emerald-500]="index === step()"
                  [class.bg-zinc-700]="index !== step()"
                ></span>
              }
            </div>
            <div class="flex gap-2">
              <button
                type="button"
                (click)="finish()"
                class="rounded-lg border border-zinc-700 px-3 py-2 text-sm"
              >
                {{ 'onboarding.skip' | translate }}
              </button>
              @if (step() < cards.length - 1) {
                <button
                  type="button"
                  (click)="next()"
                  class="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white"
                >
                  {{ 'onboarding.next' | translate }}
                </button>
              } @else {
                <button
                  type="button"
                  (click)="finish()"
                  class="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white"
                >
                  {{ 'onboarding.start' | translate }}
                </button>
              }
            </div>
          </div>

          <button
            type="button"
            (click)="seed()"
            [disabled]="seeding()"
            class="mt-4 w-full rounded-lg border border-emerald-800 px-3 py-2 text-sm text-emerald-300 disabled:opacity-50"
          >
            {{ 'onboarding.seed' | translate }}
          </button>
          @if (seedError()) {
            <p class="mt-2 text-xs text-red-300">{{ 'onboarding.seedError' | translate }}</p>
          }
        </section>
      </div>
    }
  `,
})
export class OnboardingComponent {
  private readonly session = inject(SessionStore);
  private readonly catalog = inject(CatalogApi);
  private readonly routines = inject(RoutineApi);
  protected readonly cards = [
    { title: 'onboarding.step1Title', body: 'onboarding.step1Body' },
    { title: 'onboarding.step2Title', body: 'onboarding.step2Body' },
    { title: 'onboarding.step3Title', body: 'onboarding.step3Body' },
  ];
  protected readonly step = signal(0);
  protected readonly visible = signal(
    this.session.user() !== null && localStorage.getItem(KEY) !== '1',
  );
  protected readonly seeding = signal(false);
  protected readonly seedError = signal(false);

  protected card(): { title: string; body: string } {
    return this.cards[this.step()];
  }

  protected next(): void {
    this.step.update((current) => Math.min(current + 1, this.cards.length - 1));
  }

  protected finish(): void {
    localStorage.setItem(KEY, '1');
    this.visible.set(false);
  }

  protected async seed(): Promise<void> {
    this.seeding.set(true);
    this.seedError.set(false);
    try {
      const templates = [
        { name: 'Push', query: 'press de banca' },
        { name: 'Pull', query: 'remo con barra' },
        { name: 'Legs', query: 'sentadilla trasera' },
      ];
      for (const template of templates) {
        const page = await firstValueFrom(this.catalog.search(template.query, '', ''));
        const exercise = page.content[0];
        if (!exercise) {
          continue;
        }
        await firstValueFrom(
          this.routines.create({
            name: template.name,
            notes: 'Rutina de ejemplo',
            exercises: [
              {
                exerciseId: exercise.id,
                restSeconds: 90,
                supersetGroup: null,
                notes: null,
                sets: [
                  {
                    setType: 'NORMAL',
                    targetReps: 8,
                    targetWeight: null,
                    targetRir: 2,
                  },
                ],
              },
            ],
          }),
        );
      }
      this.finish();
    } catch {
      this.seedError.set(true);
    } finally {
      this.seeding.set(false);
    }
  }
}
