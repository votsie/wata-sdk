/**
 * Пример: создание платёжной ссылки эквайринга.
 *
 * Запуск (после `npm run build` в каталоге node/):
 *   WATA_ACQUIRING_TOKEN=<jwt> node --experimental-strip-types examples/create-link.ts
 * либо скомпилируйте пример вместе с пакетом и запустите dist-версию.
 */

import { WataApiError, WataClient, WataConfigError } from '../src/index.js';

async function main() {
  const token = process.env.WATA_ACQUIRING_TOKEN;
  if (!token) {
    throw new Error('Задайте переменную окружения WATA_ACQUIRING_TOKEN.');
  }

  const client = new WataClient({
    acquiring: token,
    environment: 'production',
  });

  try {
    const link = await client.acquiring.links.create({
      amount: 150,
      currency: 'RUB',
      description: 'Пример заказа из документации SDK',
      orderId: `demo-${Date.now()}`,
    });

    console.log('Ссылка создана:', link.url);
    console.log('Статус:', link.status);
  } catch (err) {
    if (err instanceof WataConfigError) {
      console.error('Ошибка конфигурации:', err.message);
    } else if (err instanceof WataApiError) {
      console.error(`Ошибка WATA API [${err.wataCode ?? 'без кода'}]:`, err.message);
    } else {
      throw err;
    }
  }
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
