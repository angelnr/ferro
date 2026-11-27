import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '20s', target: 10 },
    { duration: '40s', target: 20 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<300'],
    'http_req_duration{endpoint:read}': ['p(95)<300'],
    'http_req_duration{endpoint:write}': ['p(95)<400'],
  },
};

const BASE = __ENV.FERRO_BASE_URL || 'http://localhost:8082';
const EMAIL = __ENV.FERRO_USER || 'demo@ferro.app';
const PASSWORD = __ENV.FERRO_PASSWORD || 'FerroDemo26';

export function setup() {
  const login = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { endpoint: 'write' } },
  );
  check(login, { 'login ok': (r) => r.status === 200 });
  return { token: login.json('accessToken') };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` };
  const read = http.get(`${BASE}/api/v1/progress/sessions?page=0&size=20`, {
    headers,
    tags: { endpoint: 'read' },
  });
  check(read, { 'history 200': (r) => r.status === 200 });
  const write = http.post(
    `${BASE}/api/v1/body-measurements`,
    JSON.stringify({ measuredAt: new Date().toISOString(), weight: 80 }),
    { headers: { ...headers, 'Content-Type': 'application/json' }, tags: { endpoint: 'write' } },
  );
  check(write, { 'measurement accepted or conflict': (r) => r.status === 201 || r.status === 409 });
  sleep(1);
}
