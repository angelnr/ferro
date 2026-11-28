import { Component, OnDestroy, effect, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthStore } from './core/auth/auth.store';
import { SessionStore } from './core/auth/session.store';
import { OnboardingComponent } from './features/onboarding/onboarding.component';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslatePipe, OnboardingComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnDestroy {
  protected readonly session = inject(SessionStore);
  protected readonly menuOpen = signal(false);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  protected readonly offline = signal(!navigator.onLine);
  private readonly online = (): void => this.offline.set(false);
  private readonly offlineHandler = (): void => this.offline.set(true);

  constructor() {
    window.addEventListener('online', this.online);
    window.addEventListener('offline', this.offlineHandler);
    effect(() => {
      const user = this.session.user();
      if (user) {
        void this.translate.use(user.locale);
        document.documentElement.lang = user.locale;
        document.documentElement.dataset['theme'] = user.theme.toLowerCase();
      }
    });
  }

  ngOnDestroy(): void {
    window.removeEventListener('online', this.online);
    window.removeEventListener('offline', this.offlineHandler);
  }

  protected retry(): void {
    window.location.reload();
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }
}
