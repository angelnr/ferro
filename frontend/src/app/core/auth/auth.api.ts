import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  LoginCredentials,
  MeResponse,
  RegisterPayload,
  UpdateProfilePayload,
} from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/auth`;

  login(credentials: LoginCredentials): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, credentials, {
      withCredentials: true,
    });
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/register`, payload, {
      withCredentials: true,
    });
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/refresh`, {}, { withCredentials: true });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.base}/logout`, {}, { withCredentials: true });
  }

  logoutAll(): Observable<void> {
    return this.http.post<void>(`${this.base}/logout-all`, {});
  }

  me(): Observable<MeResponse> {
    return this.http.get<MeResponse>(`${this.base}/me`);
  }

  confirmEmail(token: string): Observable<void> {
    return this.http.post<void>(`${this.base}/email-verification/confirm`, { token });
  }

  resendVerification(): Observable<void> {
    return this.http.post<void>(`${this.base}/email-verification/resend`, {});
  }

  requestPasswordReset(email: string): Observable<void> {
    return this.http.post<void>(`${this.base}/password-reset/request`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.base}/password-reset/confirm`, { token, newPassword });
  }

  updateProfile(payload: UpdateProfilePayload): Observable<void> {
    return this.http.patch<void>(`${environment.apiBaseUrl}/users/me`, payload);
  }
}
