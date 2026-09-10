/**
 * Пример: приём вебхука H2H с проверкой подписи на встроенном `node:http`.
 *
 * Ключевой момент: подпись проверяется по СЫРОМУ телу запроса, поэтому
 * тело читается как есть, до `JSON.parse` — пересборка JSON меняет
 * порядок ключей и ломает подпись.
 *
 * Запуск:
 *   WATA_ACQUIRING_TOKEN=<jwt> node --experimental-strip-types examples/webhook-server.ts
 */

import { createServer } from 'node:http';
import { WataClient } from '../src/index.js';

const token = process.env.WATA_ACQUIRING_TOKEN;
if (!token) {
  throw new Error('Задайте переменную окружения WATA_ACQUIRING_TOKEN.');
}

const client = new WataClient({ acquiring: token, environment: 'production' });

const server = createServer((req, res) => {
  if (req.method !== 'POST') {
    res.writeHead(405).end();
    return;
  }

  const chunks: Buffer[] = [];
  req.on('data', (chunk: Buffer) => chunks.push(chunk));
  req.on('end', () => {
    void (async () => {
      const rawBody = Buffer.concat(chunks); // именно сырые байты, не JSON.parse
      const signature = req.headers['x-signature'];

      if (typeof signature !== 'string') {
        res.writeHead(400).end('Отсутствует заголовок X-Signature');
        return;
      }

      try {
        const isValid = await client.webhooks.verify(rawBody, signature);
        if (!isValid) {
          res.writeHead(401).end('Неверная подпись');
          return;
        }

        const event = client.webhooks.parse(rawBody);
        console.log('Получено событие вебхука:', event.kind, event.transactionStatus, event.orderId);

        // Обработчик обязан быть идемпотентным (возможны повторные доставки
        // до 32 часов для постоплатных/возвратных вебхуков) и вернуть 200.
        res.writeHead(200).end('OK');
      } catch (err) {
        console.error('Не удалось обработать вебхук:', err);
        res.writeHead(500).end();
      }
    })();
  });
});

server.listen(3000, () => {
  console.log('Слушаю вебхуки WATA на http://localhost:3000');
});
