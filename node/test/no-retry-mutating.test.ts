import assert from 'node:assert/strict';
import test from 'node:test';
import { WataClient, WataNetworkError, WataServerError } from '../src/index.js';
import { createMockFetch } from './helpers.js';

test('изменяющий запрос (создание ссылки) не повторяется при 5xx', async () => {
  const { fetchImpl, calls } = createMockFetch(() => ({ status: 502 }));
  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl, maxRetries: 3 });

  await assert.rejects(
    () => client.acquiring.links.create({ amount: 100, currency: 'RUB' }),
    WataServerError,
  );

  assert.equal(calls.length, 1, 'POST не должен повторяться даже при серверной ошибке — иначе можно создать вторую ссылку');
});

test('изменяющий запрос (создание ссылки) не повторяется при сетевой ошибке', async () => {
  let callCount = 0;
  const failingFetch = (async () => {
    callCount++;
    throw new Error('ECONNRESET');
  }) as typeof fetch;

  const client = new WataClient({ acquiring: 'acquiring-token', fetch: failingFetch, maxRetries: 3 });

  await assert.rejects(
    () => client.acquiring.links.create({ amount: 100, currency: 'RUB' }),
    WataNetworkError,
  );

  assert.equal(callCount, 1, 'POST не должен повторяться при сетевой ошибке');
});

test('идемпотентный GET повторяется при 5xx до maxRetries раз', async () => {
  let attempts = 0;
  const { fetchImpl } = createMockFetch(() => {
    attempts++;
    if (attempts < 3) return { status: 503 };
    return {
      status: 200,
      body: { id: 'link-1', amount: 100, currency: 'RUB', status: 'Opened', url: 'https://pay.wata.pro/x', terminalName: 't', terminalPublicId: 'tp', creationTime: '2026-01-01T00:00:00Z', type: 'OneTime' },
    };
  });

  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl, maxRetries: 3 });
  const link = await client.acquiring.links.get('link-1');

  assert.equal(link.id, 'link-1');
  assert.equal(attempts, 3);
});

test('GET перестаёт повторяться и бросает WataServerError после исчерпания попыток', async () => {
  const { fetchImpl, calls } = createMockFetch(() => ({ status: 500 }));
  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl, maxRetries: 2 });

  await assert.rejects(() => client.acquiring.links.get('link-1'), WataServerError);
  assert.equal(calls.length, 2);
});
