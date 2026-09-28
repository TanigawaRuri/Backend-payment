import http from 'k6/http';
import { sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

const successes = new Counter('claim_success');
const exhausted = new Counter('claim_exhausted_409');
const otherErrors = new Counter('claim_other_error');

const BASE_URL = 'http://localhost:8080';
const NUM_USERS = 500;   
const TOTAL_REWARD_COUNT = 50; 

export const options = {
  scenarios: {
    reward_race: {
      executor: 'per-vu-iterations',
      vus: NUM_USERS,
      iterations: 1,    
      maxDuration: '30s',
    },
  },
};

function generateIdempotencyKey(vu) {
  return `${Date.now()}-${vu}-${Math.random().toString(36).slice(2)}`;
}

export function setup() {
  const tokens = [];

  for (let i = 0; i < NUM_USERS; i++) {
    const email = `loadtest-user-${i}@example.com`;
    const password = 'password123';

    const registerRes = http.post(`${BASE_URL}/api/users/register`, JSON.stringify({
      email: email,
      password: password,
      nickname: email,
    }), { headers: { 'Content-Type': 'application/json' } });

    if (registerRes.status >= 200 && registerRes.status < 300) {
      console.log(`User ${i} registered.`);
    } else if (registerRes.status !== 400 && registerRes.status !== 409) {
      console.warn(`Unexpected register status for user ${i}: ${registerRes.status} ${registerRes.body}`);
    }

    const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
      email: email,
      password: password,
    }), { headers: { 'Content-Type': 'application/json' } });

    if (loginRes.status !== 200) {
      throw new Error(`Login failed for user ${i}: ${loginRes.status} ${loginRes.body}`);
    }

    tokens.push(loginRes.json('accessToken'));
  }

  console.log(`${tokens.length} users registered and logged in.`);

  const createEventRes = http.post(`${BASE_URL}/api/reward-events`, JSON.stringify({
    name: `LoadTestLimitedEvent-${Date.now()}`,
    description: 'Load test limited reward event',
    rewardAmount: 1000,
    enabled: true,
    quantityType: 'LIMITED',
    remainingCount: TOTAL_REWARD_COUNT,
  }), {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${tokens[0]}`,
    },
  });

  console.log(`Create event status: ${createEventRes.status}`);
  console.log(`Create event body: ${createEventRes.body}`);

  if (createEventRes.status !== 200 && createEventRes.status !== 201) {
    throw new Error(`Failed to create reward event: ${createEventRes.status} ${createEventRes.body}`);
  }

  const rewardEventId = createEventRes.json('id');

  return { tokens, rewardEventId };
}

export default function (data) {
  const myIndex = __VU - 1;
  const token = data.tokens[myIndex];

  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };

  const res = http.post(`${BASE_URL}/api/reward-events/claims`, JSON.stringify({
    rewardEventId: data.rewardEventId,
    idempotencyKey: uuidv4(),
  }), { headers });

  if (res.status === 200 || res.status === 201) {
    successes.add(1);
    console.log(`User ${myIndex} success status ${res.status}: ${res.body}`);
  } else if (res.status === 409) {
    exhausted.add(1);
    console.log(`User ${myIndex} expected status ${res.status}: ${res.body}`);
  } else {
    otherErrors.add(1);
    console.error(`User ${myIndex} unexpected status ${res.status}: ${res.body}`);
  }

  sleep(0.1);
}

export function teardown(data) {
  console.log(`Race complete for rewardEventId=${data.rewardEventId}, total reward count was ${TOTAL_REWARD_COUNT}`);
}

export function handleSummary(data) {
  const successCount = data.metrics.claim_success ? data.metrics.claim_success.values.count : 0;
  const exhaustedCount = data.metrics.claim_exhausted_409 ? data.metrics.claim_exhausted_409.values.count : 0;
  const otherErrorCount = data.metrics.claim_other_error ? data.metrics.claim_other_error.values.count : 0;

  const reportContent = `# Reward Race Load Test Report

Generated on: ${new Date().toISOString()}

## Test Configuration
- Total virtual users: ${NUM_USERS}
- Reward pool size: ${TOTAL_REWARD_COUNT}
- Base URL: ${BASE_URL}

## Results
- Successful claims: ${successCount}
- Exhausted (409) responses: ${exhaustedCount}
- Other errors: ${otherErrorCount}

${successCount === TOTAL_REWARD_COUNT
  ? '✅ Success count matches the reward pool size — no over-issuance detected.'
  : `⚠️ Success count (${successCount}) does NOT match reward pool size (${TOTAL_REWARD_COUNT}) — check for race condition bugs.`}

## HTTP Metrics
- Total requests: ${data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 'N/A'}
- Failed request rate: ${data.metrics.http_req_failed ? (data.metrics.http_req_failed.values.rate * 100).toFixed(2) + '%' : 'N/A'}
- Avg duration: ${data.metrics.http_req_duration ? data.metrics.http_req_duration.values.avg.toFixed(2) + 'ms' : 'N/A'}
- p95 duration: ${data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'].toFixed(2) + 'ms' : 'N/A'}
`;

  return {
    'REPORT.md': reportContent,
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}