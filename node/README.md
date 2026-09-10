# @wata/sdk

Node.js / TypeScript SDK для платёжной системы [WATA](https://wata.pro):
эквайринг (H2H) и цифровые товары (Steam, Telegram Stars, Top-Up, ваучеры).

- TypeScript, ESM, строгий режим, Node.js 20+.
- Без внешних зависимостей в рантайме — используется встроенный `fetch`
  и `node:crypto`.
- `async/await` везде, без колбэков.
- Курсорная пагинация транзакций — через `for await`.

## Установка

```bash
npm install @wata/sdk
```

## Быстрый старт

```ts
import { WataClient } from '@wata/sdk';

const client = new WataClient({
  acquiring: process.env.WATA_ACQUIRING_TOKEN,
});

const link = await client.acquiring.links.create({
  amount: 150,
  currency: 'RUB',
  description: 'Заказ #1',
  orderId: 'order-1',
});

console.log(link.url);
```

## Токен выпускается на терминал, а не на аккаунт

Ключевое ограничение модели доступа WATA: JWT-токен привязан к конкретному
терминалу. Терминал нигде не передаётся в запросе — сервер определяет его
по токену.

**Telegram Stars и Steam всегда живут на отдельных терминалах.** Это не
рекомендация, а свойство платформы: вызов Stars с токеном Steam гарантированно
завершится ошибкой авторизации. Поэтому `WataClient` принимает **набор
токенов по продуктам**, и каждый продукт использует свой собственный,
физически изолированный HTTP-клиент — токены между продуктами никогда не
переиспользуются:

```ts
const client = new WataClient({
  acquiring: '<jwt терминала эквайринга>',
  stars: '<jwt терминала Telegram Stars>',
  steam: '<jwt терминала Steam>',
  topup: '<jwt терминала Top-Up>',
  vouchers: '<jwt терминала ваучеров>',
});
```

| Поле конфигурации | Продукт | Базовый URL |
|---|---|---|
| `acquiring` | Эквайринг (H2H): ссылки, транзакции, возвраты, баланс, прямые платежи | `api.wata.pro` / `api-sandbox.wata.pro` |
| `stars` | Telegram Stars | `dg-api.wata.pro` |
| `steam` | Steam | `dg-api.wata.pro` |
| `topup` | Top-Up | `dg-api.wata.pro` |
| `vouchers` | Ваучеры | `dg-api.wata.pro` |

Все токены необязательны — клиент создаётся с любым подмножеством.
Если у мерчанта один терминал совмещает эквайринг и цифровые товары, просто
передайте один и тот же токен в несколько полей — это осознанное решение
интегратора, а не догадка SDK.

**Перепутать токены невозможно — ни в рантайме, ни на уровне типов.**
Обращение к продукту без токена бросает `WataConfigError` ещё до сетевого
вызова, называя недостающий токен:

```ts
const client = new WataClient({ acquiring: '<jwt>' });

client.stars; // WataConfigError: Токен продукта "stars" (Telegram Stars) не задан...
```

Более того, если в конкретном вызове `new WataClient({...})` токен продукта
не передан, TypeScript типизирует соответствующий геттер (`client.stars`,
`client.steam` и т. д.) как `never` — обращение к любому его методу не
скомпилируется:

```ts
const client = new WataClient({ acquiring: '<jwt>' });

client.stars.price({ count: 100 });
//     ~~~~~ Property 'price' does not exist on type 'never'.
```

## Песочница

Для эквайринга доступно окружение `sandbox` (`environment: 'sandbox'`).
**Для цифровых товаров песочницы не существует** — отдельного адреса нет.
Если передать `environment: 'sandbox'` вместе с токеном Steam/Stars/Top-Up/
Vouchers, `WataClient` явно бросит `WataConfigError`, а не молча пойдёт в
боевой контур.

## Курсорная пагинация транзакций

`GET /v2/transactions` — курсорный эндпоинт: сервер возвращает `CursorId`,
`CursorAmount` и `CursorDate` для следующей страницы. Ручное листание —
источник ошибок (пропуск `CursorDate` ломает вторую страницу), поэтому SDK
предоставляет асинхронный генератор, который сам переносит все три поля:

```ts
for await (const tx of client.acquiring.transactions.iterate({ statuses: ['Paid'] })) {
  console.log(tx.id, tx.amount, tx.status);
}
```

Метод `search()` остаётся доступным напрямую, если нужен явный контроль над
страницами.

## Вебхуки

Подпись передаётся в заголовке `X-Signature` (base64), алгоритм —
**SHA512withRSA** (не SHA-256). Публичный ключ запрашивается через
`GET /api/h2h/public-key` (без авторизации) и кэшируется на время жизни
клиента, отдельно для каждого окружения.

**Проверка идёт по сырому телу запроса, до `JSON.parse`.** Пересборка JSON
меняет порядок ключей и ломает подпись — поэтому `verify()`/`parse()`
принимают только строку или байты, но не разобранный объект (это ограничение
закреплено в типах, а не только в документации).

```ts
import { createServer } from 'node:http';

const server = createServer((req, res) => {
  const chunks: Buffer[] = [];
  req.on('data', (chunk) => chunks.push(chunk));
  req.on('end', () => {
    void (async () => {
      const rawBody = Buffer.concat(chunks); // сырые байты — НЕ JSON.parse
      const signature = req.headers['x-signature'];

      if (typeof signature !== 'string' || !(await client.webhooks.verify(rawBody, signature))) {
        res.writeHead(401).end();
        return;
      }

      const event = client.webhooks.parse(rawBody);
      // Обработчик обязан быть идемпотентным и вернуть 200 —
      // постоплатные и возвратные вебхуки повторяются до 32 часов.
      res.writeHead(200).end('OK');
    })();
  });
});
```

Полный пример — [`examples/webhook-server.ts`](./examples/webhook-server.ts).

## Ошибки

```
WataError
├── WataConfigError    — нет токена продукта, неверное окружение, недопустимая дата баланса (до сетевого вызова)
├── WataAuthError      — 401/403
├── WataRateLimitError — 429, с retryAfterMs/retryAt, если сервер их сообщил
├── WataApiError       — 4xx, содержит wataCode (PL_*, TRA_*, ORD_*, STM_*, STR_*, TPP_*, VCR_*), details, validationErrors
├── WataServerError    — 5xx после исчерпания повторов
├── WataNetworkError   — таймаут, обрыв соединения
└── WataWebhookError   — подпись не сошлась или ключ не получен
```

Каждая ошибка несёт `httpStatus`, `path` и, если применимо, `wataCode`.
Секреты (токен, тело с картой) в текст ошибки не попадают.

Неизвестное значение enum, пришедшее с сервера (например, новый статус
транзакции), не ломает разбор ответа — SDK сохраняет исходную строку и
продолжает работу.

## Повторы запросов

- Повторяются только сетевые ошибки и `5xx`, с экспоненциальной задержкой
  и джиттером (по умолчанию 3 попытки).
- **Изменяющие запросы (создание ссылки, платежа, заказа, возврата) по
  умолчанию НЕ повторяются** — повтор может создать второй платёж.
- `429` никогда не повторяется автоматически: SDK сразу бросает
  `WataRateLimitError`, при возможности — с указанием времени повтора.

## Баланс терминала

`GET /finance/balance` принимает только сегодняшнюю или вчерашнюю дату по
UTC. SDK проверяет это локально и бросает `WataConfigError` до сетевого
вызова:

```ts
await client.acquiring.balance.get(new Date()); // сегодня — ок
await client.acquiring.balance.get('2020-01-01'); // WataConfigError
```

## Тестирование в собственном коде

Конструктор принимает опцию `fetch` — подставьте свою реализацию, чтобы
мокать HTTP без реальной сети (так же устроены тесты самого SDK):

```ts
const client = new WataClient({
  acquiring: 'test-token',
  fetch: async (url, init) => new Response(JSON.stringify({ /* ... */ }), { status: 200 }),
});
```

## Разработка

```bash
npm install
npm run build       # сборка пакета (src -> dist)
npm test            # компиляция тестов в dist-test и запуск node --test
```

Тесты собираются отдельным `tsconfig.test.json` и никогда не попадают в
публикуемый пакет (`dist/`).

## Известные ограничения этой версии

Спецификация (`docs/SPEC.md`) не приводит точный список полей запросов и
ответов для части эндпоинтов цифровых товаров (Steam/Top-Up/Vouchers —
списки товаров, создание заказов сверх полей, названных явно). Эти методы
реализованы по таблице эндпоинтов с разумным набором полей; перед
интеграцией сверьте точные названия полей с актуальной OpenAPI-схемой
`dg-api.wata.pro`.
