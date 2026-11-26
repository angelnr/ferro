import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { AuthStore } from './core/auth/auth.store';
import { authInterceptor } from './core/auth/auth.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideRouter(routes),
    provideAppInitializer(() => inject(AuthStore).restore()),
    provideTranslateService({
      loader: provideTranslateHttpLoader({
        prefix: './assets/i18n/',
        suffix: '.json',
        failOnError: true,
      }),
      lang: 'es',
      fallbackLang: 'en',
    }),
  ],
};
