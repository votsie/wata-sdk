import assert from 'node:assert/strict';
import test from 'node:test';
import { WataClient, WataConfigError } from '../src/index.js';
import { createMockFetch } from './helpers.js';

function isoDate(offsetDays: number): string {
  const date = new Date(Date.now() + offsetDays * 24 * 60 * 60 * 1000);
  return date.toISOString().slice(0, 10);
}

test('баланс на сегодня и вчера по UTC проходит без ошибки', async () => {
  const { fetchImpl, calls } = createMockFetch(() => ({
    status: 200,
    body: { terminalPublicId: 'tp', date: isoDate(0), balance: 100, currency: 'RUB' },
  }));
  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  await client.acquiring.balance.get(isoDate(0));
  await client.acquiring.balance.get(isoDate(-1));

  assert.equal(calls.length, 2);
});

test('баланс на позавчера бросает WataConfigError до сетевого вызова', async () => {
  const { fetchImpl, calls } = createMockFetch(() => ({ status: 200, body: {} }));
  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  assert.throws(() => client.acquiring.balance.get(isoDate(-2)), WataConfigError);
  assert.equal(calls.length, 0);
});

test('баланс на завтра бросает WataConfigError до сетевого вызова', async () => {
  const { fetchImpl, calls } = createMockFetch(() => ({ status: 200, body: {} }));
  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  assert.throws(() => client.acquiring.balance.get(isoDate(1)), WataConfigError);
  assert.equal(calls.length, 0);
});

test('некорректный формат даты баланса бросает WataConfigError', async () => {
  const { fetchImpl, calls } = createMockFetch(() => ({ status: 200, body: {} }));
  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  assert.throws(() => client.acquiring.balance.get('01.01.2026'), WataConfigError);
  assert.equal(calls.length, 0);
});
