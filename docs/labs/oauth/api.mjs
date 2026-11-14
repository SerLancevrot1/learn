import { createServer } from 'node:http';
import { createRemoteJWKSet, jwtVerify } from 'jose';

const issuer = process.env.ISSUER;
const audience = 'lab-api';
const oidcEndpoint = `${process.env.KEYCLOAK_REALM_URL}/protocol/openid-connect`;
const jwks = createRemoteJWKSet(new URL(`${oidcEndpoint}/certs`));
const credentials = Buffer.from(`${audience}:${process.env.INTROSPECTION_SECRET}`).toString('base64');

async function verifyJwt(token) {
    try {
        const { payload } = await jwtVerify(token, jwks, {
            algorithms: ['RS256'], issuer, audience, requiredClaims: ['exp', 'sub'],
        });
        return payload.typ === 'Bearer' ? payload : null;
    } catch (error) {
        if (error.code === 'ERR_JWKS_TIMEOUT' || error.code === 'ERR_JOSE_GENERIC' || error instanceof TypeError) {
            throw error;
        }
        return null;
    }
}

async function introspect(token) {
    const response = await fetch(`${oidcEndpoint}/token/introspect`, {
        method: 'POST',
        headers: {
            authorization: `Basic ${credentials}`,
            'content-type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({ token, token_type_hint: 'access_token' }),
        signal: AbortSignal.timeout(3000),
    });
    if (!response.ok) {
        throw new Error(`Introspection HTTP ${response.status}`);
    }
    const claims = await response.json();
    const audiences = [claims.aud].flat();
    return claims.active === true && claims.iss === issuer && audiences.includes(audience)
        && claims.token_type === 'Bearer' && typeof claims.sub === 'string' ? claims : null;
}

function json(res, status, payload, headers = {}) {
    res.writeHead(status, { ...headers, 'content-type': 'application/json', 'cache-control': 'no-store' });
    res.end(JSON.stringify(payload));
}

createServer(async (req, res) => {
    const path = new URL(req.url, 'http://localhost').pathname;
    if (path === '/callback') {
        json(res, 200, { message: 'Copy the callback URL to the PowerShell exercise.' });
        return;
    }
    if (req.method !== 'GET' || !['/api/jwt', '/api/introspection'].includes(path)) {
        json(res, 404, { error: 'not_found' });
        return;
    }

    const token = req.headers.authorization?.match(/^Bearer (\S+)$/i)?.[1];
    try {
        const verify = path === '/api/jwt' ? verifyJwt : introspect;
        const claims = token ? await verify(token) : null;
        if (!claims) {
            json(res, 401, { error: 'invalid_token' }, { 'www-authenticate': 'Bearer' });
            return;
        }
        const scopes = (claims.scope ?? '').split(' ');
        if (!scopes.includes('email')) {
            json(res, 403, { error: 'insufficient_scope' }, {
                'www-authenticate': 'Bearer error="insufficient_scope", scope="email"',
            });
            return;
        }
        json(res, 200, { subject: claims.sub });
    } catch (error) {
        console.error(error.message);
        json(res, 503, { error: 'token_validation_unavailable' });
    }
}).listen(8080, '0.0.0.0');
