import assert from 'node:assert/strict';
import test from 'node:test';
import { WataClient, WataConfigError } from '../src/index.js';
import { createMockFetch, constantHandler } from './helpers.js';

test('обращение к продукту без токена бросает WataConfigError до сетевого вызова', async () => {
  const { fetchImpl, calls } = createMockFetch(constantHandler({ status: 200, body: {} }));
  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  assert.throws(
    () => client.stars,
    (err: unknown) => {
      assert.ok(err instanceof WataConfigError);
      assert.match((err as Error).message, /stars/);
      return true;
    },
  );

  assert.equal(calls.length, 0, 'сетевой вызов не должен происходить, если токен продукта не задан');
});

test('каждый непереданный токен называется в сообщении об ошибке', async () => {
  const { fetchImpl } = createMockFetch(constantHandler({ status: 200, body: {} }));
  const client = new WataClient({ fetch: fetchImpl });

  assert.throws(() => client.acquiring, /acquiring/);
  assert.throws(() => client.stars, /stars/);
  assert.throws(() => client.steam, /steam/);
  assert.throws(() => client.topup, /topup/);
  assert.throws(() => client.vouchers, /vouchers/);
});

test('заданный токен продукта делает соответствующий namespace доступным', () => {
  const { fetchImpl } = createMockFetch(constantHandler({ status: 200, body: {} }));
  const client = new WataClient({ stars: 'stars-token', steam: 'steam-token', fetch: fetchImpl });

  assert.ok(client.stars);
  assert.ok(client.steam);
  assert.throws(() => client.acquiring, /acquiring/);
});
