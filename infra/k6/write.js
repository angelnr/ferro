import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

export const options = {
  vus: 1,
  iterations: 10,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{endpoint:write}': ['p(95)<400'],
    finish_duration: ['p(95)<800'],
  },
};

const BASE = __ENV.FERRO_BASE_URL || 'http://localhost:8082';
const EMAIL = __ENV.FERRO_USER || 'demo@ferro.app';
const PASSWORD = __ENV.FERRO_PASSWORD || 'FerroDemo26';
const finishDuration = new Trend('finish_duration');

export function setup() {
  const login = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { endpoint: 'write' } },
  );
  check(login, { 'login ok': (r) => r.status === 200 });
  const headers = { Authorization: `Bearer ${login.json('accessToken')}` };
  const search = http.get(`${BASE}/api/v1/exercises?q=press%20banca&size=5`, { headers });
  check(search, { 'exercise search ok': (r) => r.status === 200 });
  return { headers, exerciseId: search.json('content.0.id') };
}

export default function (data) {
  const jsonHeaders = { ...data.headers, 'Content-Type': 'application/json' };
  const session = http.post(`${BASE}/api/v1/workout-sessions`, '{}', {
    headers: jsonHeaders,
    tags: { endpoint: 'write' },
  });
  check(session, { 'session created': (r) => r.status === 201 });
  if (session.status !== 201) {
    return;
  }
  const sessionId = session.json('id');
  const entry = http.post(
    `${BASE}/api/v1/workout-sessions/${sessionId}/exercises`,
    JSON.stringify({ exerciseId: data.exerciseId }),
    { headers: jsonHeaders, tags: { endpoint: 'write' } },
  );
  check(entry, { 'exercise added': (r) => r.status === 201 });
  const entryId = entry.json('exercises.0.id');
  for (let index = 0; index < 6; index += 1) {
    const set = http.post(
      `${BASE}/api/v1/workout-sessions/${sessionId}/exercises/${entryId}/sets`,
      JSON.stringify({ reps: 8, weight: 60 + index, setType: 'NORMAL' }),
      { headers: jsonHeaders, tags: { endpoint: 'write' } },
    );
    check(set, { 'set logged': (r) => r.status === 201 });
  }
  const started = Date.now();
  const finish = http.post(`${BASE}/api/v1/workout-sessions/${sessionId}/finish`, '{}', {
    headers: jsonHeaders,
    tags: { endpoint: 'write' },
  });
  finishDuration.add(Date.now() - started);
  check(finish, { 'session finished': (r) => r.status === 200 });
  sleep(1);
}
