import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 50  },  // 워밍업
        { duration: '5s',  target: 500 },  // 스파이크 (Tomcat 200 2.5배)
        { duration: '20s', target: 500 },  // 스파이크 유지
        { duration: '5s',  target: 0   },  // 종료
    ]
};

export default function () {
    const res = http.post(
        `http://host.docker.internal:8081/test/notifications/trigger?userId=${__VU}`,
        null,
        { timeout: '10s' }
    );
    check(res, { 'status 200': (r) => r.status === 200 });
}