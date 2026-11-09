// CK-P01 (NFR-01): 50 concurrent users. Runner: GitHub Actions, see the E2E workflow.
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  insecureSkipTLSVerify: true,
  stages: [
    { duration: '20s', target: 50 },
    { duration: '60s', target: 50 },
    { duration: '10s', target: 0 },
  ],
  thresholds: { 'http_req_duration{kind:api}': ['p(95)<2000'], http_req_failed: ['rate<0.01'] },
};

const BASE = __ENV.BASE_URL || 'https://localhost';
const tagged = { tags: { kind: 'api' } };

export function setup() {
  const response = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ email: 'motcua@ehoso.local', password: __ENV.DEMO_PASSWORD || 'Demo12345' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  return { token: response.json('accessToken') };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` };
  check(http.get(`${BASE}/api/v1/procedures`, tagged), { catalog: (r) => r.status === 200 });
  check(http.get(`${BASE}/api/v1/staff/dossiers?size=20`, { ...tagged, headers }), {
    queue: (r) => r.status === 200,
  });
  sleep(1);
}
