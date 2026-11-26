import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { AuthApi } from './auth.api';
import {
  LoginCredentials,
  ProblemDetail,
  RegisterPayload,
  UpdateProfilePayload,
} from './auth.models';
import { SessionStore } from './session.store';

interface AuthState {
  loading: boolean;
  error: string | null;
  initialized: boolean;
}

const initialState: AuthState = {
  loading: false,
  error: null,
  initialized: false,
};

export const AuthStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withMethods((store) => {
    const api = inject(AuthApi);
    const session = inject(SessionStore);

    const fail = (error: unknown): void => {
      patchState(store, { error: errorMessage(error) });
    };

    return {
      async restore(): Promise<void> {
        try {
          const response = await firstValueFrom(api.refresh());
          session.setSession(response);
        } catch {
          session.clear();
        } finally {
          session.markInitialized();
          patchState(store, { initialized: true });
        }
      },

      async login(credentials: LoginCredentials): Promise<boolean> {
        patchState(store, { loading: true, error: null });
        try {
          const response = await firstValueFrom(api.login(credentials));
          session.setSession(response);
          return true;
        } catch (error) {
          fail(error);
          return false;
        } finally {
          patchState(store, { loading: false });
        }
      },

      async register(payload: RegisterPayload): Promise<boolean> {
        patchState(store, { loading: true, error: null });
        try {
          const response = await firstValueFrom(api.register(payload));
          session.setSession(response);
          return true;
        } catch (error) {
          fail(error);
          return false;
        } finally {
          patchState(store, { loading: false });
        }
      },

      async logout(): Promise<void> {
        try {
          await firstValueFrom(api.logout());
        } catch {
          // La cookie puede estar ya expirada; el estado local se limpia igualmente.
        } finally {
          session.clear();
        }
      },

      async logoutAll(): Promise<void> {
        try {
          await firstValueFrom(api.logoutAll());
        } catch {
          // Idem: el objetivo es dejar de tener sesión en el cliente.
        } finally {
          session.clear();
        }
      },

      async confirmEmail(token: string): Promise<boolean> {
        patchState(store, { loading: true, error: null });
        try {
          await firstValueFrom(api.confirmEmail(token));
          return true;
        } catch (error) {
          fail(error);
          return false;
        } finally {
          patchState(store, { loading: false });
        }
      },

      async resendVerification(): Promise<boolean> {
        patchState(store, { loading: true, error: null });
        try {
          await firstValueFrom(api.resendVerification());
          return true;
        } catch (error) {
          fail(error);
          return false;
        } finally {
          patchState(store, { loading: false });
        }
      },

      async requestPasswordReset(email: string): Promise<boolean> {
        patchState(store, { loading: true, error: null });
        try {
          await firstValueFrom(api.requestPasswordReset(email));
          return true;
        } catch (error) {
          fail(error);
          return false;
        } finally {
          patchState(store, { loading: false });
        }
      },

      async resetPassword(token: string, newPassword: string): Promise<boolean> {
        patchState(store, { loading: true, error: null });
        try {
          await firstValueFrom(api.resetPassword(token, newPassword));
          return true;
        } catch (error) {
          fail(error);
          return false;
        } finally {
          patchState(store, { loading: false });
        }
      },

      async updateProfile(payload: UpdateProfilePayload): Promise<boolean> {
        patchState(store, { loading: true, error: null });
        try {
          await firstValueFrom(api.updateProfile(payload));
          const me = await firstValueFrom(api.me());
          session.setUser(me.user);
          return true;
        } catch (error) {
          fail(error);
          return false;
        } finally {
          patchState(store, { loading: false });
        }
      },

      clearError(): void {
        patchState(store, { error: null });
      },

      async oauthCallback(provider: string, code: string, state: string): Promise<boolean> {
        patchState(store, { loading: true, error: null });
        try {
          const response = await firstValueFrom(api.oauthCallback(provider, code, state));
          session.setSession(response);
          return true;
        } catch (error) {
          fail(error);
          return false;
        } finally {
          patchState(store, { loading: false });
        }
      },
    };
  }),
);

function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as ProblemDetail | null;
    const fieldError = problem?.errors
      ? Object.values(problem.errors)
          .flat()
          .find((message) => Boolean(message))
      : undefined;
    return fieldError ?? problem?.detail ?? problem?.code ?? 'No se pudo completar la operación';
  }
  return 'No se pudo completar la operación';
}
