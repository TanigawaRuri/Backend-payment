import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const conflicts = new Counter('wallet_conflicts_409');
const successes = new Counter('wallet_claims_success');
const otherErrors = new Counter('wallet_claims_other_error');

const BASE_URL = 'http://localhost:8080';

export const options = {
  scenarios: {
    wallet_race: {
      executor: 'shared-iterations',
      vus: 50,          // 50 virtual users
      iterations: 200,  // all hitting the SAME wallet 200 times total
      maxDuration: '30s',
    },
  },
};

// --- Setup: runs once, logs in and creates the reward event to claim against ---
export function setup() {
  const register = http.post(`${BASE_URL}/api/users/register`, JSON.stringify({
    email: 'loadtest@example.com',
    password: 'password123',
    nickname: 'loadtest@example.com'
  }), { headers: { 'Content-Type': 'application/json' } });

  console.log(`Register status: ${register.status}`);

  if (register.status >= 200 && register.status < 300) {
    console.log('User registered fresh.');
  } else if (register.status === 400 || register.status === 409) {
    console.log('User already exists, continuing to login.');
  } else {
    console.warn(`Unexpected register status ${register.status}: ${register.body}`);
  }

  const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    email: 'loadtest@example.com',
    password: 'password123',
  }), { headers: { 'Content-Type': 'application/json' } });

  console.log(`Login status: ${loginRes.status}`);
  console.log(`Login body: ${loginRes.body}`);

  check(loginRes, { 'login succeeded': (r) => r.status === 200 });

  const token = loginRes.json('accessToken');
  
  const rewardEventId = 2;

  return { token, rewardEventId };
}

// --- Main: every VU iteration tries to claim the SAME reward event ---
export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${data.token}`,
  };

  const res = http.post(
    `${BASE_URL}/api/reward-events/claims`, // adjust path to your actual endpoint
    JSON.stringify({
        rewardEventId: data.rewardEventId,
        idempotencyKey: uuidv4()
    }),
    { headers }
  );

  if (res.status === 200 || res.status === 201) {
    successes.add(1);
    console.log(`Success status ${res.status}: ${res.body}`);
  } else if (res.status === 409) {
    conflicts.add(1);
    console.error(`Expected status ${res.status}: ${res.body}`);
  } else {
    otherErrors.add(1);
    console.error(`Unexpected status ${res.status}: ${res.body}`);
  }

  check(res, {
    'status is 200/201 or 409 (no 5xx)': (r) => r.status < 500,
  });

  sleep(0.1);
}

// --- Teardown: print a clean summary for your README ---
export function teardown(data) {
  console.log(`Load test complete against rewardEventId=${data.rewardEventId}`);
}