import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-privacy-page',
  imports: [RouterLink, TranslatePipe],
  template: `
    <section class="mx-auto mt-8 max-w-3xl space-y-5 text-sm text-zinc-300">
      <a routerLink="/" class="text-emerald-400">{{ 'privacy.back' | translate }}</a>
      <h1 class="text-3xl font-black text-zinc-100">{{ 'privacy.title' | translate }}</h1>
      <p>{{ 'privacy.intro' | translate }}</p>
      <h2 class="text-lg font-bold text-zinc-100">{{ 'privacy.dataTitle' | translate }}</h2>
      <p>{{ 'privacy.data' | translate }}</p>
      <h2 class="text-lg font-bold text-zinc-100">{{ 'privacy.purposeTitle' | translate }}</h2>
      <p>{{ 'privacy.purpose' | translate }}</p>
      <h2 class="text-lg font-bold text-zinc-100">{{ 'privacy.rightsTitle' | translate }}</h2>
      <p>{{ 'privacy.rights' | translate }}</p>
      <h2 class="text-lg font-bold text-zinc-100">{{ 'privacy.retentionTitle' | translate }}</h2>
      <p>{{ 'privacy.retention' | translate }}</p>
      <p class="text-xs text-zinc-500">{{ 'privacy.contact' | translate }}</p>
    </section>
  `,
})
export class PrivacyPage {}
