import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 10,
    duration: '30s',
    summaryTrendStats: [
        'avg',
        'min',
        'med',
        'max',
        'p(90)',
        'p(95)',
        'p(99)',
    ],
};

export default function () {
    const response = http.get(
        'http://localhost:8080/internal/baseline/ping',
        { timeout: '15s' }
    );

    check(response, {
        'status is 200': (r) => r.status === 200,
        'response contains data': (r) =>
            r.status === 200 &&
            r.body !== null &&
            r.json('data') === 'success',
    });

    if (response.status !== 200) {
        console.error(
            `request failed: status=${response.status}, error=${response.error}, code=${response.error_code}`
        );
    }
}