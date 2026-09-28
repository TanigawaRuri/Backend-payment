# Reward Race Load Test Report

Generated on: 2026-09-18T09:39:33.011Z

## Test Configuration
- Total virtual users: 500
- Reward pool size: 50
- Base URL: http://localhost:8080

## Results
- Successful claims: 50
- Exhausted (409) responses: 450
- Other errors: 0

✅ Success count matches the reward pool size — no over-issuance detected.

## HTTP Metrics
- Total requests: 1501
- Failed request rate: 63.29%
- Avg duration: 145.43ms
- p95 duration: 480.88ms
