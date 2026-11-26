import { Injectable, computed, signal } from '@angular/core';
import { AuthResponse, User } from './auth.models';

/**
 * Estado de sesión en memoria. El access token nunca toca localStorage (ADR-0006).
 */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly userState = signal<User | null>(null);
  private readonly accessTokenState = signal<string | null>(null);
  private readonly initializedState = signal(false);

  readonly user = this.userState.asReadonly();
  readonly accessToken = this.accessTokenState.asReadonly();
  readonly initialized = this.initializedState.asReadonly();
  readonly isAuthenticated = computed(() => this.accessTokenState() !== null);

  setSession(response: AuthResponse): void {
    this.userState.set(response.user);
    this.accessTokenState.set(response.accessToken);
  }

  setUser(user: User): void {
    this.userState.set(user);
  }

  markInitialized(): void {
    this.initializedState.set(true);
  }

  clear(): void {
    this.userState.set(null);
    this.accessTokenState.set(null);
  }
}
