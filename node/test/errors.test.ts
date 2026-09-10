import assert from 'node:assert/strict';
import test from 'node:test';
import { WataApiError, WataAuthError, WataClient, WataRateLimitError } from '../src/index.js';
import { createMockFetch, constantHandler } from './helpers.js';

test('4xx с кодом WATA бросает WataApiError с кодом, сообщением и статусом', async () => {
  const { fetchImpl } = createMockFetch(
    constantHandler({
      status: 400,
      body: {
        error: {
          code: 'PL_INVALID_AMOUNT',
          message: 'Сумма вне допустимого диапазона.',
          details: { min: 10, max: 999999.99 },
        },
      },
    }),
  );

  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  await assert.rejects(
    () => client.acquiring.links.create({ amount: 0.01, currency: 'RUB' }),
    (err: unknown) => {
      assert.ok(err instanceof WataApiError);
      const apiErr = err as WataApiError;
      assert.equal(apiErr.wataCode, 'PL_INVALID_AMOUNT');
      assert.equal(apiErr.httpStatus, 400);
      assert.match(apiErr.message, /допустимого диапазона/);
      assert.deepEqual(apiErr.details, { min: 10, max: 999999.99 });
      return true;
    },
  );
});

test('401 бросает WataAuthError', async () => {
  const { fetchImpl } = createMockFetch(constantHandler({ status: 401, body: { error: { message: 'Токен недействителен' } } }));
  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  await assert.rejects(() => client.acquiring.links.get('some-id'), WataAuthError);
});

test('429 бросает WataRateLimitError с временем повтора из Retry-After', async () => {
  const { fetchImpl } = createMockFetch(
    constantHandler({ status: 429, headers: { 'Retry-After': '30' } }),
  );
  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  await assert.rejects(
    () => client.acquiring.links.get('some-id'),
    (err: unknown) => {
      assert.ok(err instanceof WataRateLimitError);
      const rateLimitErr = err as WataRateLimitError;
      assert.equal(rateLimitErr.retryAfterMs, 30_000);
      assert.ok(rateLimitErr.retryAt instanceof Date);
      return true;
    },
  );
});
