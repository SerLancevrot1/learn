import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8088';
const path = __ENV.API_PATH || '/ok';

export const options = {
    vus: 5,
    duration: '20s',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<300'],
        checks: ['rate>0.99'],
    },
};

export default function () {
    const response = http.get(`${baseUrl}${path}`, {
        timeout: '5s',
        redirects: 0,
    });
    check(response, {
        'status is 200': (res) => res.status === 200,
    });
    sleep(1);
}
