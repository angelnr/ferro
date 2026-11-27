import AxeBuilder from '@axe-core/playwright';
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
  await page.addInitScript(() => localStorage.setItem('ferro.onboarding.seen', '1'));
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
  await page.route('**/api/v1/workout-sessions/active', (route) =>
    route.fulfill({ status: 204, body: '' }),
  );
  await page.route('**/api/v1/routines?**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [],
        page: 0,
        size: 5,
        totalElements: 0,
        totalPages: 0,
      }),
    }),
  );
  await page.route('**/api/v1/exercises/*', (route) =>
    route.fulfill({ status: 404, contentType: 'application/problem+json', body: '{}' }),
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

test('runs a full live training session', async ({ page }) => {
  await authenticated(page);
  interface MockSet {
    id: string;
    position: number;
    setType: string;
    reps: number;
    weight: number;
    rir: number | null;
    volume: number;
    e1rm: number;
    loggedAt: string;
  }
  interface MockEntry {
    id: string;
    exerciseId: string;
    name: string;
    position: number;
    restSeconds: number | null;
    supersetGroup: number | null;
    notes: string | null;
    sets: MockSet[];
    targetSets: unknown[];
  }
  interface MockSession {
    id: string;
    name: string;
    status: string;
    startedAt: string;
    finishedAt: string | null;
    durationSeconds: number | null;
    totalVolume: number;
    exercises: MockEntry[];
  }
  const session: MockSession = {
    id: 'session-1',
    name: 'Sesión del 2026-11-26',
    status: 'ACTIVE',
    startedAt: '2026-11-26T10:00:00Z',
    finishedAt: null,
    durationSeconds: null,
    totalVolume: 0,
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
        targetSets: [],
      },
      {
        id: 'entry-2',
        exerciseId: 'exercise-2',
        name: 'Sentadilla trasera con barra',
        position: 1,
        restSeconds: null,
        supersetGroup: null,
        notes: null,
        sets: [],
        targetSets: [],
      },
      {
        id: 'entry-3',
        exerciseId: 'exercise-3',
        name: 'Peso muerto con barra',
        position: 2,
        restSeconds: null,
        supersetGroup: null,
        notes: null,
        sets: [],
        targetSets: [],
      },
    ],
  };

  await page.route('**/api/v1/workout-sessions', (route) =>
    route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(session) }),
  );
  await page.route('**/api/v1/workout-sessions/session-1', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(session) }),
  );
  await page.route('**/api/v1/exercises/*/history?**', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
  );
  let setCounter = 0;
  await page.route('**/api/v1/workout-sessions/session-1/exercises/*/sets', (route) => {
    const entryId = new URL(route.request().url()).pathname.split('/').at(-2);
    const entry = session.exercises.find((candidate) => candidate.id === entryId);
    if (!entry) {
      return route.fulfill({ status: 404, body: '{}' });
    }
    const body = route.request().postDataJSON() as { reps: number; weight: number };
    setCounter += 1;
    const set: MockSet = {
      id: `set-${setCounter}`,
      position: entry.sets.length,
      setType: 'NORMAL',
      reps: body.reps,
      weight: body.weight,
      rir: null,
      volume: body.reps * body.weight,
      e1rm: body.weight * (1 + body.reps / 30),
      loggedAt: '2026-11-26T10:05:00Z',
    };
    entry.sets.push(set);
    session.totalVolume += set.volume;
    return route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify(set),
    });
  });
  await page.route('**/api/v1/workout-sessions/session-1/supersets', (route) => {
    const body = route.request().postDataJSON() as { entryIds: string[]; group: number | null };
    for (const entry of session.exercises) {
      if (body.entryIds.includes(entry.id)) {
        entry.supersetGroup = body.group;
      }
    }
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(session),
    });
  });
  await page.route('**/api/v1/workout-sessions/session-1/finish', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        session: { ...session, status: 'FINISHED', finishedAt: '2026-11-26T11:00:00Z' },
        newPersonalRecords: [
          {
            exerciseId: 'exercise-1',
            exerciseName: 'Press de banca con barra',
            prType: 'MAX_WEIGHT',
            value: 20,
            previousValue: null,
            achievedAt: '2026-11-26T11:00:00Z',
          },
        ],
      }),
    }),
  );

  await page.goto('/');
  await page.getByRole('button', { name: 'Empezar a entrenar' }).click();

  await expect(page).toHaveURL(/\/training\/session-1/);
  await expect(page.getByRole('heading', { name: 'Sesión del 2026-11-26' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Press de banca con barra' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Sentadilla trasera con barra' })).toBeVisible();

  await page.getByLabel('Seleccionar ejercicio').nth(0).check();
  await page.getByLabel('Seleccionar ejercicio').nth(1).check();
  await page.getByRole('button', { name: 'Agrupar superset' }).click();
  await expect(page.getByText('A1', { exact: true })).toBeVisible();
  await expect(page.getByText('A2', { exact: true })).toBeVisible();

  for (let index = 0; index < 12; index += 1) {
    const refreshed = page.waitForResponse(
      (response) =>
        response.url().endsWith('/workout-sessions/session-1') &&
        response.request().method() === 'GET',
    );
    await page.getByRole('button', { name: 'Registrar serie' }).click();
    await refreshed;
  }

  await expect(page.getByText('Descanso', { exact: true })).toBeVisible();
  await expect(page.getByText('1920 kg')).toBeVisible();

  await page.getByRole('button', { name: 'Finalizar' }).click();
  await expect(page.getByText('MAX_WEIGHT')).toBeVisible();
});

test('creates a routine and completes its pending target sets', async ({ page }) => {
  await authenticated(page);
  const routine = {
    id: 'routine-1',
    name: 'PPL',
    notes: 'Empuje',
    exercises: [
      {
        id: 'routine-exercise-1',
        exerciseId: 'exercise-1',
        name: 'Press de banca con barra',
        position: 0,
        restSeconds: 120,
        supersetGroup: null,
        notes: null,
        sets: [
          {
            id: 'target-routine-1',
            position: 0,
            setType: 'NORMAL',
            targetReps: 8,
            targetWeight: 60,
            targetRir: 2,
          },
        ],
      },
    ],
  };
  const session = {
    id: 'session-routine',
    name: 'PPL',
    status: 'ACTIVE',
    startedAt: '2026-11-26T12:00:00Z',
    finishedAt: null,
    durationSeconds: null,
    totalVolume: 0,
    exercises: [
      {
        id: 'entry-routine-1',
        exerciseId: 'exercise-1',
        name: 'Press de banca con barra',
        position: 0,
        restSeconds: 120,
        supersetGroup: null,
        notes: null,
        sets: [] as unknown[],
        targetSets: [
          {
            id: 'target-1',
            position: 0,
            setType: 'NORMAL',
            targetReps: 8,
            targetWeight: 60,
            targetRir: 2,
          },
        ],
      },
    ],
  };

  await page.route('**/api/v1/routines?**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [{ id: routine.id, name: routine.name, exerciseCount: 1, lastUsedAt: null }],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }),
    }),
  );
  await page.route('**/api/v1/routines', (route) =>
    route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify(routine),
    }),
  );
  await page.route('**/api/v1/routines/routine-1', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(routine),
    }),
  );
  await page.route('**/api/v1/routines/routine-1/start', (route) =>
    route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify(session),
    }),
  );
  await page.route('**/api/v1/workout-sessions/session-routine', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(session),
    }),
  );
  await page.route('**/api/v1/workout-sessions/session-routine/exercises/*/sets', (route) => {
    const body = route.request().postDataJSON() as {
      reps: number;
      weight: number;
      targetSetId?: string;
    };
    expect(body.targetSetId).toBe('target-1');
    const set = {
      id: 'set-routine-1',
      position: 0,
      setType: 'NORMAL',
      reps: body.reps,
      weight: body.weight,
      rir: 2,
      volume: body.reps * body.weight,
      e1rm: body.weight * (1 + body.reps / 30),
      loggedAt: '2026-11-26T12:05:00Z',
      notes: null,
    };
    (session.exercises[0].sets as unknown[]).push(set);
    session.exercises[0].targetSets = [];
    return route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify(set),
    });
  });
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

  await page.goto('/routines/new');
  await page.getByLabel('Nombre de la rutina').fill('PPL');
  await page.getByLabel('Buscar ejercicio').fill('press');
  await page.getByRole('option', { name: /Press de banca con barra/ }).click();
  await page.getByRole('button', { name: 'Guardar' }).first().click();

  await expect(page).toHaveURL(/\/routines$/);
  await expect(page.getByRole('heading', { name: 'PPL' })).toBeVisible();
  await page.getByRole('button', { name: 'Empezar' }).click();

  await expect(page).toHaveURL(/\/training\/session-routine/);
  await expect(page.getByText('Series objetivo pendientes')).toBeVisible();
  await page.getByRole('button', { name: /NORMAL · 8×60/ }).click();
  await page.getByRole('button', { name: 'Registrar serie' }).click();

  await expect(page.getByText('Series objetivo pendientes')).toHaveCount(0);
  await expect(page.getByText(/8 × 60 kg/)).toBeVisible();
});

test('shows workout history and statistics', async ({ page }) => {
  await authenticated(page);
  await page.route('**/api/v1/progress/sessions?**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'session-history',
            name: 'PPL cerrada',
            startedAt: '2026-11-20T10:00:00Z',
            finishedAt: '2026-11-20T11:00:00Z',
            durationSeconds: 3600,
            totalVolume: 1000,
            exerciseCount: 2,
            setCount: 8,
            hasPr: true,
            status: 'FINISHED',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }),
    }),
  );
  await page.route('**/api/v1/progress/summary?**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        weeks: 12,
        weeklyVolume: [{ weekStart: '2026-11-16T00:00:00Z', value: 1000 }],
        weeklySessions: [{ weekStart: '2026-11-16T00:00:00Z', value: 1 }],
        currentStreak: 2,
        bestStreak: 4,
        current: { volume: 1000, sessions: 1, sets: 8 },
        previous: { volume: 900, sessions: 1, sets: 7 },
        muscleDistribution: [{ muscleGroup: 'CHEST', volume: 600 }],
        periodStart: '2026-09-01T00:00:00Z',
        periodEnd: '2026-11-24T00:00:00Z',
      }),
    }),
  );

  await page.goto('/history');
  await expect(page.getByRole('heading', { name: 'Historial' })).toBeVisible();
  await expect(page.getByText('PPL cerrada')).toBeVisible();
  await page.getByRole('link', { name: 'Estadísticas' }).click();
  await expect(page).toHaveURL(/\/stats$/);
  await expect(page.getByText('1000 kg')).toBeVisible();
  await expect(page.getByText('CHEST')).toBeVisible();
});

test('shows records and records a body measurement', async ({ page }) => {
  await authenticated(page);
  await page.route('**/api/v1/progress/prs**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          exerciseId: 'exercise-1',
          exerciseName: 'Press de banca con barra',
          current: [
            { type: 'MAX_WEIGHT', value: 100, achievedAt: '2026-11-20T10:00:00Z' },
            { type: 'MAX_E1RM', value: 126.67, achievedAt: '2026-11-20T10:00:00Z' },
          ],
          history: [],
        },
      ]),
    }),
  );
  await page.route('**/api/v1/body-measurements/stats?**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        weight: [
          { date: '2026-11-01T00:00:00Z', value: 81 },
          { date: '2026-11-20T00:00:00Z', value: 80 },
        ],
        chest: [],
        waist: [],
        hip: [],
        arm: [],
        thigh: [],
      }),
    }),
  );
  await page.route('**/api/v1/body-measurements?**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'body-1',
            measuredAt: '2026-11-20T10:00:00Z',
            weight: 80,
            chest: null,
            waist: 82,
            hip: null,
            arm: null,
            thigh: null,
            notes: null,
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }),
    }),
  );
  await page.route('**/api/v1/body-measurements', (route) =>
    route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 'body-2',
        measuredAt: '2026-11-21T12:00:00Z',
        weight: 79.5,
        chest: null,
        waist: null,
        hip: null,
        arm: null,
        thigh: null,
        notes: null,
      }),
    }),
  );

  await page.goto('/records');
  await expect(page.getByText('Press de banca con barra')).toBeVisible();
  await expect(page.getByText('100 kg')).toBeVisible();

  await page.goto('/body');
  await expect(page.getByRole('heading', { name: 'Mediciones' })).toBeVisible();
  await page.locator('#body-weight').fill('79.5');
  await page.getByRole('button', { name: 'Registrar' }).click();
  await expect(page.getByRole('button', { name: 'Registrar' })).toBeEnabled();
});

test('supports keyboard navigation with a skip link', async ({ page }) => {
  await authenticated(page);
  await page.goto('/');
  const skip = page.getByRole('link', { name: 'Saltar al contenido' });
  for (let index = 0; index < 3; index += 1) {
    await page.keyboard.press('Tab');
    if (await skip.evaluate((element) => element === document.activeElement)) {
      break;
    }
  }
  await expect(skip).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/#main$/);
  await expect(page.locator('#main')).toBeFocused();
});

test('has no critical or serious accessibility violations on the main screens', async ({
  page,
}) => {
  await authenticated(page);
  await page.goto('/');
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();
  const critical = results.violations.filter((violation) =>
    ['critical', 'serious'].includes(violation.impact ?? ''),
  );
  expect(critical, JSON.stringify(critical.map((violation) => violation.id))).toEqual([]);

  await page.goto('/settings');
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  const settings = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();
  expect(
    settings.violations.filter((violation) =>
      ['critical', 'serious'].includes(violation.impact ?? ''),
    ),
    JSON.stringify(settings.violations.map((violation) => violation.id)),
  ).toEqual([]);
});
