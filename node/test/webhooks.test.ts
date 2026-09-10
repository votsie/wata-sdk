import assert from 'node:assert/strict';
import { generateKeyPairSync, sign as cryptoSign } from 'node:crypto';
import test from 'node:test';
import { WataClient } from '../src/index.js';
import { createMockFetch } from './helpers.js';

function generateRsaKeyPair() {
  return generateKeyPairSync('rsa', {
    modulusLength: 2048,
    publicKeyEncoding: { type: 'spki', format: 'pem' },
    privateKeyEncoding: { type: 'pkcs8', format: 'pem' },
  });
}

function signBody(privateKeyPem: string, rawBody: string): string {
  return cryptoSign('RSA-SHA512', Buffer.from(rawBody, 'utf8'), privateKeyPem).toString('base64');
}

test('verify() возвращает true для подлинной подписи SHA512withRSA', async () => {
  const { publicKey, privateKey } = generateRsaKeyPair();
  const rawBody = JSON.stringify({ id: 'evt-1', transactionStatus: 'Paid', amount: 100 });
  const signature = signBody(privateKey, rawBody);

  const { fetchImpl, calls } = createMockFetch((call) => {
    assert.match(call.url, /\/public-key$/);
    assert.equal(call.headers.authorization, undefined, 'public-key вызывается без заголовка авторизации');
    return { status: 200, body: { value: publicKey } };
  });

  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });
  const ok = await client.webhooks.verify(rawBody, signature);

  assert.equal(ok, true);
  assert.equal(calls.length, 1, 'ключ должен запрашиваться и кэшироваться');

  // Повторная проверка не должна запрашивать ключ снова.
  const ok2 = await client.webhooks.verify(rawBody, signature);
  assert.equal(ok2, true);
  assert.equal(calls.length, 1, 'ключ должен браться из кэша при повторной проверке');
});

test('verify() возвращает false для подделанной подписи', async () => {
  const { publicKey } = generateRsaKeyPair();
  const { publicKey: otherPublicKey } = generateRsaKeyPair();
  void otherPublicKey;
  const { privateKey: attackerPrivateKey } = generateRsaKeyPair();

  const rawBody = JSON.stringify({ id: 'evt-2', transactionStatus: 'Declined' });
  const forgedSignature = signBody(attackerPrivateKey, rawBody);

  const { fetchImpl } = createMockFetch(() => ({ status: 200, body: { value: publicKey } }));
  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  const ok = await client.webhooks.verify(rawBody, forgedSignature);
  assert.equal(ok, false);
});

test('verify() возвращает false при подмене тела после подписания', async () => {
  const { publicKey, privateKey } = generateRsaKeyPair();
  const rawBody = JSON.stringify({ id: 'evt-3', amount: 100 });
  const signature = signBody(privateKey, rawBody);
  const tamperedBody = JSON.stringify({ id: 'evt-3', amount: 999 });

  const { fetchImpl } = createMockFetch(() => ({ status: 200, body: { value: publicKey } }));
  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  const ok = await client.webhooks.verify(tamperedBody, signature);
  assert.equal(ok, false);
});

test('parse() разбирает сырое тело в типизированное событие и не падает на неизвестном enum', () => {
  const { fetchImpl } = createMockFetch(() => ({ status: 200, body: {} }));
  const client = new WataClient({ acquiring: 'acquiring-token', fetch: fetchImpl });

  const rawBody = JSON.stringify({
    id: 'evt-4',
    transactionStatus: 'SomeBrandNewStatusFromTheFuture',
    kind: 'Payment',
    amount: 150,
    currency: 'RUB',
  });

  const event = client.webhooks.parse(rawBody);
  assert.equal(event.id, 'evt-4');
  assert.equal(event.transactionStatus, 'SomeBrandNewStatusFromTheFuture');
  assert.equal(event.amount, 150);
});
