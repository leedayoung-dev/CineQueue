
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 10 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const response = http.get('http://localhost:8080/api/movies');

  check(response, {
    'status is 200': (r) => r.status === 200,
    'response contains movies': (r) =>
      r.headers['Content-Type']?.includes('application/json'),
  });

  sleep(1);
}