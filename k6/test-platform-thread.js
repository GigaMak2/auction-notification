import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 200,
    duration: '30s',
};

export default function () {
    const res = http.post('http://host.docker.internal:8081/test/platform-thread');
    check(res, { 'status 200': (r) => r.status === 200 });
}
