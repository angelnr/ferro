import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SessionStore } from './session.store';

/** Añade el Bearer en memoria; nunca hay access token en localStorage (ADR-0006). */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionStore);
  const token = session.accessToken();
  const locale =
    session.user()?.locale ??
    (typeof navigator !== 'undefined' && navigator.language.toLowerCase().startsWith('en')
      ? 'en'
      : 'es');
  const headers: Record<string, string> = { 'Accept-Language': locale };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return next(request.clone({ withCredentials: true, setHeaders: headers }));
};
