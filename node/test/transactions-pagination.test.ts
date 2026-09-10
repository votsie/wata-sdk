import assert from 'node:assert/strict';
import test from 'node:test';
import { WataClient } from '../src/index.js';
import { createMockFetch } from './helpers.js';

test('iterate() переносит CursorId/CursorAmount/CursorDate между двумя страницами', async () => {
  const { fetchImpl, calls } = createMockFetch((call) => {
    const url = new URL(call.url);
    const cursorId = url.searchParams.get('CursorId');

    if (!cursorId) {
      // Первая страница: курсор ещё не задан.
      return {
        status: 200,
        body: {
          items: [{ id: 'tx-1', amount: 100, currency: 'RUB', status: 'Paid', kind: 'Payment', creationTime: '2026-01-01T00:00:00Z' }],
          hasNextPage: true,
          nextCursorId: 'tx-1',
          nextCursorAmount: 100,
          nextCursorDate: '2026-01-01T00:00:00Z',
        },
      };
    }

    assert.equal(cursorId, 'tx-1');
    assert.equal(url.searchParams.get('CursorAmount'), '100');
    assert.equal(url.searchParams.get('CursorDate'), '2026-01-01T00:00:00Z');

    return {
      status: 200,
      body: {
        items: [{ id: 'tx-2', amount: 200, currency: 'RUB', status: 'Paid', kind: 'Payment', creationTime: '2026-01-02T00:00:00Z' }],
        hasNextPage: false,
      },
    };
  });

  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  const ids: string[] = [];
  for await (const tx of client.acquiring.transactions.iterate({ statuses: ['Paid'] })) {
    ids.push(tx.id);
  }

  assert.deepEqual(ids, ['tx-1', 'tx-2']);
  assert.equal(calls.length, 2, 'должно быть ровно два запроса — по одному на страницу');
});

test('iterate() останавливается, если сервер сообщил hasNextPage без курсора', async () => {
  const { fetchImpl } = createMockFetch(() => ({
    status: 200,
    body: {
      items: [{ id: 'tx-1', amount: 100, currency: 'RUB', status: 'Paid', kind: 'Payment', creationTime: '2026-01-01T00:00:00Z' }],
      hasNextPage: true,
      // намеренно нет nextCursorId/nextCursorDate/nextCursorAmount
    },
  }));

  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  const ids: string[] = [];
  for await (const tx of client.acquiring.transactions.iterate({})) {
    ids.push(tx.id);
  }

  assert.deepEqual(ids, ['tx-1']);
});
