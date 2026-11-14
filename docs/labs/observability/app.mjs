import { createServer } from 'node:http';
import { randomUUID } from 'node:crypto';

const counts = new Map([[200, 0], [404, 0], [500, 0]]);

createServer((req, res) => {
    const path = new URL(req.url, 'http://app').pathname;
    if (path === '/metrics') {
        const metrics = [
            '# TYPE lab_http_requests_total counter',
            ...Array.from(counts, ([status, count]) =>
                `lab_http_requests_total{status="${status}"} ${count}`),
        ];
        res.writeHead(200, { 'content-type': 'text/plain; version=0.0.4' });
        res.end(`${metrics.join('\n')}\n`);
        return;
    }

    const status = path === '/ok' ? 200 : path === '/fail' ? 500 : 404;
    const requestId = req.headers['x-request-id'] ?? randomUUID();
    counts.set(status, counts.get(status) + 1);
    const event = JSON.stringify({ service: 'lab-app', request_id: requestId, path, status });
    (status === 500 ? process.stderr : process.stdout).write(`${event}\n`);
    res.writeHead(status, { 'content-type': 'application/json' });
    res.end(JSON.stringify({ status, request_id: requestId }));
}).listen(8080, '0.0.0.0');
