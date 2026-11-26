import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SessionStore } from './session.store';

/** Añade el Bearer en memoria; nunca hay access token en localStorage (ADR-0006). */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionStore);
  const token = session.accessToken();
  return next(
    request.clone({
      withCredentials: true,
      setHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    }),
  );
};
