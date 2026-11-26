import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from './core/auth/auth.store';
import { SessionStore } from './core/auth/session.store';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslatePipe],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly session = inject(SessionStore);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }
}
