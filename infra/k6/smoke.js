import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 2,
  duration: '20s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
  },
};

const BASE = __ENV.FERRO_BASE_URL || 'http://localhost:8082';

export default function () {
  const health = http.get(`${BASE}/actuator/health/readiness`);
  check(health, { 'readiness 200': (r) => r.status === 200 });
  const flags = http.get(`${BASE}/api/v1/config/flags`);
  check(flags, { 'flags 200': (r) => r.status === 200 });
  sleep(1);
}
