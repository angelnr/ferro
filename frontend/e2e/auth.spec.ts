import { Page, expect, test } from '@playwright/test';

const USER = {
  id: '01936a1e-0000-7000-8000-000000000000',
  displayName: 'E2E Tester',
  email: 'e2e@ferro.dev',
  status: 'ACTIVE',
  locale: 'es',
  unitSystem: 'KG',
  theme: 'DARK',
  timeZone: 'Europe/Madrid',
  defaultRestSeconds: 90,
};

const AUTH_RESPONSE = { user: USER, accessToken: 'e2e-access-token' };

const REFRESH_INVALID = {
  status: 401,
  contentType: 'application/problem+json',
  body: JSON.stringify({ code: 'REFRESH_INVALID', detail: 'Refresh token no válido' }),
};

async function anonymous(page: Page): Promise<void> {
  await page.route('**/api/v1/auth/refresh', (route) => route.fulfill(REFRESH_INVALID));
  await page.route('**/api/v1/auth/oauth/providers', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: '{"google":false,"github":false}',
    }),
  );
}

async function authenticated(page: Page): Promise<void> {
  await page.route('**/api/v1/auth/oauth/providers', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: '{"google":false,"github":false}',
    }),
  );
  await page.route('**/api/v1/auth/refresh', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(AUTH_RESPONSE),
    }),
  );
}

test('redirects anonymous users to login', async ({ page }) => {
  await anonymous(page);
  await page.goto('/');

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('heading', { name: 'Entra en Ferro' })).toBeVisible();
});

test('logs in and reaches the home page', async ({ page }) => {
  await anonymous(page);
  await page.route('**/api/v1/auth/login', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(AUTH_RESPONSE),
    }),
  );

  await page.goto('/login');
  await page.getByLabel('Email').fill(USER.email);
  await page.getByLabel('Contraseña').fill('Ferro12345');
  await page.getByRole('button', { name: 'Entrar' }).click();

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole('heading', { name: `Hola, ${USER.displayName}` })).toBeVisible();
  await expect(page.getByRole('main').getByText(USER.email)).toBeVisible();
});

test('registers a new account and shows the verification banner', async ({ page }) => {
  await anonymous(page);
  await page.route('**/api/v1/auth/register', (route) =>
    route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        user: { ...USER, status: 'UNVERIFIED' },
        accessToken: 'e2e-access-token',
      }),
    }),
  );

  await page.goto('/register');
  await page.getByLabel('Nombre').fill('E2E Tester');
  await page.getByLabel('Email').fill('nuevo@ferro.dev');
  await page.getByLabel('Contraseña').fill('Ferro12345');
  await page.getByLabel('Acepto el tratamiento de mis datos de salud para usar Ferro.').check();
  await page.getByRole('button', { name: 'Crear cuenta' }).click();

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByText('Tu email aún no está verificado.')).toBeVisible();
});

test('updates preferences from settings', async ({ page }) => {
  await authenticated(page);
  await page.route('**/api/v1/users/me', (route) => route.fulfill({ status: 204, body: '' }));
  await page.route('**/api/v1/auth/me', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        user: { ...USER, displayName: 'Nombre Nuevo', locale: 'en', unitSystem: 'LB' },
        preferences: {
          locale: 'en',
          unitSystem: 'LB',
          theme: 'DARK',
          timeZone: USER.timeZone,
          defaultRestSeconds: 120,
          timerSound: true,
        },
      }),
    }),
  );

  await page.goto('/settings');
  await page.getByLabel('Nombre').fill('Nombre Nuevo');
  await page.getByLabel('Idioma').selectOption('en');
  await page.getByLabel('Unidades').selectOption('LB');
  await page.getByLabel('Descanso por defecto (segundos)').fill('120');
  await page.getByRole('button', { name: 'Guardar cambios' }).click();

  await expect(page.getByText(/Preferences saved\.|Preferencias guardadas\./)).toBeVisible();
});

test('confirms an email from the verification link', async ({ page }) => {
  await anonymous(page);
  await page.route('**/api/v1/auth/email-verification/confirm', (route) =>
    route.fulfill({ status: 204, body: '' }),
  );

  await page.goto('/verify-email?token=e2e-token');

  await expect(page.getByRole('heading', { name: 'Email verificado' })).toBeVisible();
});

test('searches the exercise catalog', async ({ page }) => {
  await authenticated(page);
  await page.route('**/api/v1/exercises**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'exercise-1',
            name: 'Press de banca con barra',
            nameEn: 'Barbell Bench Press',
            muscleGroup: 'CHEST',
            secondaryMuscles: ['TRICEPS'],
            equipment: 'BARBELL',
            mechanics: 'COMPOUND',
            bodyweight: false,
            mine: false,
          },
        ],
        page: 0,
        size: 30,
        totalElements: 1,
        totalPages: 1,
      }),
    }),
  );

  await page.goto('/catalog');

  await expect(page.getByRole('heading', { name: 'Ejercicios' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Press de banca con barra' })).toBeVisible();
});

test('opens an exercise detail with stats', async ({ page }) => {
  await authenticated(page);
  await page.route('**/api/v1/exercises/exercise-1', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        exercise: {
          id: 'exercise-1',
          name: 'Press de banca con barra',
          nameEn: 'Barbell Bench Press',
          muscleGroup: 'CHEST',
          secondaryMuscles: ['TRICEPS'],
          equipment: 'BARBELL',
          mechanics: 'COMPOUND',
          bodyweight: false,
          mine: false,
        },
        stats: {
          personalRecords: [{ type: 'MAX_WEIGHT', value: 100, achievedAt: '2026-11-20T10:00:00Z' }],
          bestE1rm: 133.33,
          totalVolume: 1000,
          sessionCount: 1,
          e1rmSeries: [{ date: '2026-11-20T10:00:00Z', value: 133.33 }],
        },
      }),
    }),
  );
  await page.route('**/api/v1/exercises?**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'exercise-1',
            name: 'Press de banca con barra',
            nameEn: 'Barbell Bench Press',
            muscleGroup: 'CHEST',
            secondaryMuscles: ['TRICEPS'],
            equipment: 'BARBELL',
            mechanics: 'COMPOUND',
            bodyweight: false,
            mine: false,
          },
        ],
        page: 0,
        size: 30,
        totalElements: 1,
        totalPages: 1,
      }),
    }),
  );

  await page.goto('/catalog');
  await page.getByRole('link', { name: 'Press de banca con barra' }).click();

  await expect(page).toHaveURL(/\/catalog\/exercise-1/);
  await expect(page.getByRole('heading', { name: 'Press de banca con barra' })).toBeVisible();
  await expect(page.getByText('MAX_WEIGHT')).toBeVisible();
  await expect(page.getByText('133.33').first()).toBeVisible();
});

test('starts a live training session', async ({ page }) => {
  await authenticated(page);
  const session = {
    id: 'session-1',
    name: 'Sesión del 2026-11-26',
    status: 'ACTIVE',
    startedAt: '2026-11-26T10:00:00Z',
    finishedAt: null,
    durationSeconds: null,
    totalVolume: 640,
    exercises: [
      {
        id: 'entry-1',
        exerciseId: 'exercise-1',
        name: 'Press de banca con barra',
        position: 0,
        restSeconds: null,
        supersetGroup: null,
        notes: null,
        sets: [],
      },
    ],
  };
  await page.route('**/api/v1/workout-sessions', (route) =>
    route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(session) }),
  );
  await page.route('**/api/v1/workout-sessions/session-1', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(session) }),
  );

  await page.goto('/');
  await page.getByRole('button', { name: 'Empezar a entrenar' }).click();

  await expect(page).toHaveURL(/\/training\/session-1/);
  await expect(page.getByRole('heading', { name: 'Sesión del 2026-11-26' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Press de banca con barra' })).toBeVisible();
});
