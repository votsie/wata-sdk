# Спецификация WATA SDK

Единый контракт для всех пяти реализаций: Node, Python, Go, Java, .NET.
Все SDK обязаны следовать этому документу, чтобы поведение совпадало между языками.

Источник данных об API — реальный OpenAPI-контракт WATA и документация
`api.wata.pro` / `dg-api.wata.pro`.

## 1. Ключевое ограничение модели доступа

**Токен WATA выпускается на терминал, а не на аккаунт.** Терминал нигде не
передаётся в запросе — сервер определяет его по токену.

**Stars и Steam всегда живут на отдельных терминалах**, то есть требуют
собственных токенов. Это не рекомендация, а свойство платформы: попытка вызвать
Stars с токеном Steam приведёт к отказу.

Отсюда главное требование к дизайну: SDK принимает **набор токенов по продуктам**
и физически не позволяет обратиться к продукту чужим токеном.

```
WataClient({
  acquiring: "<jwt терминала эквайринга>",
  stars:     "<jwt терминала Telegram Stars>",
  steam:     "<jwt терминала Steam>",
  topup:     "<jwt терминала пополнений>",
  vouchers:  "<jwt терминала ваучеров>",
})
```

Правила:

- Все токены необязательны. Клиент создаётся с любым подмножеством.
- Обращение к продукту, токен которого не задан, немедленно бросает ошибку
  конфигурации **до** сетевого вызова, с текстом, называющим недостающий токен.
- Каждый продукт использует собственный HTTP-клиент со своим токеном.
  Токены между продуктами не переиспользуются никогда.
- Если у мерчанта один терминал совмещает эквайринг и цифровые товары, он просто
  передаёт один и тот же токен в несколько полей. Это его осознанное решение,
  а не догадка SDK.

## 2. Окружения

| Продукт | Боевое | Песочница |
|---|---|---|
| Эквайринг (H2H) | `https://api.wata.pro` | `https://api-sandbox.wata.pro` |
| Цифровые товары | `https://dg-api.wata.pro` | не документирована |

Клиент принимает параметр окружения (`production` по умолчанию). Для песочницы
цифровых товаров отдельного адреса нет — если пользователь запросит `sandbox`
для DG, SDK обязан явно сообщить, что песочница для этого продукта недоступна,
а не молча пойти в боевой контур.

**У боевого контура и песочницы разные публичные ключи вебхуков.** Кэш ключа
обязан быть привязан к окружению.

## 3. Общее поведение HTTP

- Авторизация: заголовок `Authorization: Bearer <token>`.
- Единственное исключение: `GET /api/h2h/public-key` вызывается **без** заголовка
  авторизации.
- Таймаут ответа API — 1 минута. Значение по умолчанию в SDK: 60 секунд, настраивается.
- Повторы: только для сетевых ошибок и ответов `5xx`. Экспоненциальная задержка
  с джиттером, по умолчанию 3 попытки. **Запросы, изменяющие состояние
  (создание заказа, платежа, возврата), по умолчанию не повторяются** — повтор
  может создать второй платёж.
- Rate limit: `429` возвращается для части GET-эндпоинтов (лимит порядка
  1 запроса за 30 секунд на объект). SDK не должен молча ретраить `429` в цикле:
  он бросает типизированную ошибку с указанием, когда можно повторить, если сервер
  это сообщил.
- Пустые и `null` значения не отправляются в теле и query.

## 4. Эквайринг — база `/api/h2h`

| Метод | Путь | Назначение |
|---|---|---|
| POST | `/links` | Создать платёжную ссылку |
| GET | `/links` | Поиск ссылок (постраничный) |
| GET | `/links/{id}` | Ссылка по идентификатору |
| GET | `/v2/transactions` | Поиск транзакций (**курсорный**) |
| GET | `/transactions/{id}` | Транзакция по идентификатору |
| POST | `/transactions/refunds` | Возврат |
| GET | `/finance/balance` | Баланс терминала |
| POST | `/payments/card-crypto` | Оплата картой по криптограмме |
| POST | `/payments/sbp` | Оплата СБП |
| POST | `/payments/tpay` | Оплата T-Pay |
| GET | `/public-key` | Публичный ключ вебхуков (без авторизации) |

### Создание ссылки — поля запроса

Обязательные: `amount` (number), `currency` (enum Currency).

Опциональные: `description`, `orderId`, `successRedirectUrl`, `failRedirectUrl`,
`expirationDateTime` (ISO 8601), `type` (`OneTime` | `ManyTime`, по умолчанию
`OneTime`), `isArbitraryAmountAllowed` (bool), `arbitraryAmountPrompts`
(массив чисел), `email`, `phone`, `username`, `userId`,
`subscription` (`period` int, `interval` `Test|Week|Month`, `maxPeriods` int,
`amount` number, `startDate` опционально).

Ответ: `id`, `amount`, `currency`, `status`, `url`, `terminalName`,
`terminalPublicId`, `creationTime`, `type`, `orderId`, `expirationDateTime`,
`isArbitraryAmountAllowed`, `arbitraryAmounts`.

Ограничения по документации (SDK их **не** валидирует жёстко, но упоминает в
документации типов): сумма от 10 RUB / 1 USD / 1 EUR до 999999.99; срок жизни
от 10 минут до 30 дней, по умолчанию 3 дня; подсказок произвольной суммы не
более 6, каждая не меньше `amount`.

### Поиск ссылок — query

`OrderId`, `CreationTimeFrom`, `CreationTimeTo`, `AmountFrom`, `AmountTo`,
`Currencies[]`, `Statuses[]`, `Sorting` (`orderId|creationTime|amount`, суффикс
` desc`), `SkipCount`, `MaxResultCount` (по умолчанию 10, максимум 1000).

Ответ: `{ items, totalCount }`.

### Поиск транзакций — курсорная пагинация

Query: `OrderId`, `CreationTimeFrom`, `CreationTimeTo`, `AmountFrom`, `AmountTo`,
`Currencies[]`, `PaymentLinkIds[]`, `Statuses[]`, `Sorting`, `MaxResultCount`,
`CursorId`, `CursorAmount`, `CursorDate`.

Ответ: `{ items, hasNextPage, nextCursorId, nextCursorDate, nextCursorAmount }`.

**Все SDK обязаны предоставить итератор/генератор по страницам**, который сам
переносит три курсорных поля. Ручное листание — источник ошибок: пропуск
`CursorDate` ломает вторую страницу.

### Возврат

Запрос: `originalTransactionId` (uuid), `amount` (number). Больше полей нет —
валюта берётся из исходной транзакции, поля «причина» не существует.

Ответ: `transactionId`, `originalTransactionId`, `transactionStatus`, `kind`
(всегда `Refund`), `errorCode`, `errorDescription`.

Ограничения: сумма больше нуля, не больше доступного остатка, не более двух
знаков после запятой; исходная транзакция должна быть `Paid`; возврат недоступен
на терминалах с продуктом «Цифровые товары + Эквайринг».

### Баланс

Query: `Date` (обязательный, формат даты). **Допустимы только сегодняшняя и
вчерашняя дата по UTC.** SDK обязан проверить это локально и бросить понятную
ошибку до сетевого вызова.

Ответ: `terminalPublicId`, `date`, `balance`, `currency`.

### Прямые платежи

- `card-crypto`: обязательны `amount`, `currency`, `cardCrypto`, `ip`,
  `returnUrl`, `deviceData`. В ответе возможен `threeDsData` (`url`, `method`,
  `parameters`) — редирект или автосабмит формы 3DS.
  Криптограмму формирует клиентский скрипт чекаута в браузере плательщика;
  сервер мерчанта её не собирает. Документация типа обязана это оговаривать.
- `sbp`: обязательны `amount`, `ip`, `returnUrl`, `deviceData`. Валюты нет —
  только рубли. В ответе `sbpLink`.
- `tpay`: то же, что `sbp`, без `firstName`/`lastName`. В ответе `tPayLink`.

## 5. Цифровые товары — база `/api`

Два способа оплаты, и от него зависит путь:
**acquiring** — платит покупатель, **deposit** — списывается с депозита мерчанта.

### Steam (токен терминала Steam)

| Оплата | Метод | Путь |
|---|---|---|
| acquiring | GET | `/v3/steam/amount` (по сумме зачисления) |
| acquiring | GET | `/v3/steam/by-amount` (по сумме платежа) |
| acquiring | POST | `/v3/steam`, `/v3/steam/by-amount` |
| acquiring | GET | `/v3/steam/order/{id}` |
| deposit | GET | `/v1/steam/deposit/price`, `/v1/steam/deposit/netamount` |
| deposit | POST | `/v1/steam/deposit`, `/v1/steam/deposit/by-price` |
| deposit | GET | `/v1/deposit/order/{orderId}` (общий статус) |

Два сценария различаются тем, что задано: сумма зачисления на аккаунт
(`netAmount`) или сумма платежа (`amount`/`price`). Комиссия и курс делают эти
величины разными — API SDK обязан называть их явно, а не «сумма».

### Telegram Stars (токен терминала Stars)

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/stars/price` | Стоимость и минимум |
| POST | `/stars` | Создать заказ |
| GET | `/stars/order/{id}` | Статус |
| POST | `/stars/order/{id}/confirm` | Подтвердить заказ в статусе `Review` |
| POST | `/stars/order/{id}/reject` | Отклонить заказ в статусе `Review` |

Количество звёзд: 50–50000. Заказы дороже порога попадают в статус `Review` и
**не выполняются**, пока их явно не подтвердят. Документация типов обязана это
называть — иначе интегратор будет ждать выдачи, которой не будет.

### Top-Up и ваучеры

| Оплата | Метод | Путь |
|---|---|---|
| acquiring | GET | `/v3/topup/all`, `/v3/vouchers/all` |
| acquiring | POST | `/v3/topup`, `/v3/vouchers` |
| acquiring | GET | `/v3/topup/orders/{id}`, `/v3/vouchers/order/{id}` |
| deposit | GET/POST | `/v1/deposit/topups`, `/v1/deposit/vouchers` |
| deposit | GET | `/v1/deposit/order/{orderId}` |

Коды ваучеров возвращаются в статусе заказа (отдельного эндпоинта выдачи нет) и
могут появиться с задержкой до 10 минут.

### Депозит

`GET /v1/deposit/balance` → `totalBalance`, `frozenBalance`, `availableBalance`.

## 6. Enum'ы

- `Currency`: `RUB`, `USD`, `EUR` (в схеме присутствует `GBP`, документацией не
  подтверждён — включить со значением и пометкой «не документирован»)
- `TransactionStatus`: `Created`, `Pending`, `Paid`, `Declined`
- `TransactionKind`: `Payment`, `Refund`
- `TransactionType`: `CardCrypto`, `SBP`, `TPay`
- `PaymentLinkStatus`: `Opened`, `Closed`
- `PaymentLinkType`: `OneTime`, `ManyTime`
- `SubscriptionInterval`: `Test`, `Week`, `Month`
- Статус заказа DG: `Pending` → `Paid` → `Success` | `Fail`
- Статус заказа Stars: `Pending` → `Review` → `Paid` | `Refunded` → `Success` | `Fail`
- Статус депозитного заказа: `Pending`, `Success`, `Fail`

Неизвестное значение enum, пришедшее с сервера, **не должно ронять разбор
ответа**. API живой и может добавить значение — SDK обязан сохранить исходную
строку и продолжить работу.

## 7. Ошибки

Иерархия (имена адаптируются под идиомы языка):

- `WataError` — базовая
  - `WataConfigError` — нет токена нужного продукта, неверное окружение,
    недопустимая дата баланса. Возникает **до** сетевого вызова
  - `WataAuthError` — `401`/`403`: токен истёк, отозван, принадлежит другому
    терминалу либо запрос идёт с несогласованного IP
  - `WataRateLimitError` — `429`, с указанием времени повтора, если сервер его дал
  - `WataApiError` — `4xx` с телом `{error: {code, message, details, validationErrors}}`;
    обязана содержать код ошибки WATA (`PL_*`, `TRA_*`, `ORD_*`, `STM_*`, `STR_*`,
    `TPP_*`, `VCR_*`), сообщение и HTTP-статус
  - `WataServerError` — `5xx` после исчерпания повторов
  - `WataNetworkError` — таймаут, обрыв соединения
  - `WataWebhookError` — подпись не сошлась или ключ не получен

Каждая ошибка несёт HTTP-статус, путь запроса и, если он есть, код WATA.
Секреты (токен, тело с картой) в текст ошибки не попадают никогда.

## 8. Вебхуки

- Заголовок подписи: `X-Signature`, значение в base64
- Алгоритм: **SHA512withRSA** (RSA PKCS#1 v1.5 + SHA-512). Не SHA-256
- Ключ: `GET /api/h2h/public-key`, поле `value`, PEM
- Проверяется **сырое тело запроса**, до разбора JSON

API проверки во всех SDK:

```
verifyWebhook(rawBody, signature)          -> bool   (ключ подтягивается и кэшируется)
verifyWebhook(rawBody, signature, keyPem)  -> bool   (ключ задан явно)
parseWebhook(rawBody)                      -> типизированное событие
```

Сигнатура обязана принимать тело как **байты или строку**, но не как разобранный
объект: пересборка JSON меняет порядок ключей и ломает подпись. В документации
метода это нужно сказать прямо, а типом по возможности запретить приём объекта.

Поля события: `transactionType`, `kind`, `id`, `transactionId`,
`originalTransactionId`, `terminalPublicId`, `transactionStatus`, `errorCode`,
`errorDescription`, `terminalName`, `amount`, `currency`, `orderId`,
`orderDescription`, `commission`, `paymentTime`, `email`, `paymentLinkId`,
`payerData.payerId`.

Обратите внимание: в вебхуке комиссия называется `commission`, а в ответе
`GET /transactions/{id}` то же значение — `totalCommission`.

Типы вебхуков и поведение при сбое:

| Тип | Когда | Таймаут ответа | При ошибке |
|---|---|---|---|
| Предоплатный | до обращения в банк | 10 секунд | транзакция отклоняется |
| Постоплатный | после результата оплаты | 1 минута | повторы до 32 часов |
| Возвратный | после результата возврата | 1 минута | повторы до 32 часов |

Обработчик обязан вернуть HTTP 200 и быть идемпотентным.

## 9. Требования к каждой реализации

1. **Идиоматичность.** Код должен выглядеть как написанный носителем языка:
   `async/await` в Node, type hints и `dataclass`/`pydantic` в Python,
   `context.Context` и явные ошибки в Go, builder-паттерн и `Optional` в Java,
   `async`/`CancellationToken` и nullable-типы в .NET.
2. **Никаких зависимостей сверх необходимого.** Предпочтительна стандартная
   библиотека языка.
3. **Тесты без сети** — на моках HTTP. Обязательное покрытие:
   отсутствие токена продукта, проверка подписи вебхука (успех и подделка),
   курсорная пагинация через две страницы, разбор ошибки с кодом WATA,
   отказ от повтора изменяющего запроса, проверка даты баланса.
4. **Рабочий пример**: создание платёжной ссылки и приём вебхука с проверкой подписи.
5. **README** на русском: установка, быстрый старт, таблица токенов по продуктам,
   и обязательный раздел про то, что Stars и Steam — отдельные терминалы.
6. Версия SDK — `0.1.0` во всех языках.

---

## 10. Точные поля Digital Goods

Раздел добавлен после первой итерации: без него реализации вынужденно
использовали нетипизированные словари. Данные взяты из документации
`dg-api.wata.pro`.

Обозначения: **req** — обязательное поле.

### Steam + эквайринг

| Метод | Путь | Запрос | Ответ |
|---|---|---|---|
| GET | `/v3/steam/amount` | `netAmount` req, `account` req | `price`, `minPrice`, `steamRate` |
| POST | `/v3/steam` | `account` req, `amount` req, `netAmount` req, `description` req, `orderId` req; опционально redirect-адреса | `orderId`, `amount`, `price`, `minPrice`, `commission`, `steamRate`, `paymentLink` |
| GET | `/v3/steam/by-amount` | `amount` req, `margin` req, `account` req | `netAmount`, `price`, `steamRate` |
| POST | `/v3/steam/by-amount` | `account` req, `amount` req, `margin` req, `description` req, `orderId` req | `orderId`, `amount`, `netAmount`, `price`, `commission`, `margin`, `steamRate`, `paymentLink` |
| GET | `/v3/steam/order/{id}` | — | `orderId`, `amount`, `status`, redirect-адреса |

### Steam + депозит

| Метод | Путь | Запрос | Ответ |
|---|---|---|---|
| GET | `/v1/steam/deposit/price` | `account` req, `netAmount` req | `price` (в долларах), `netAmount`, `steamRate` |
| POST | `/v1/steam/deposit` | `account` req, `netAmount` req, `description` req, `orderId` req | `orderId`, `account`, `price`, `netAmount`, `steamRate` |
| GET | `/v1/steam/deposit/netamount` | `account` req, `price` req | `price`, `netAmount`, `steamRate` |
| POST | `/v1/steam/deposit/by-price` | `account` req, `price` req, `description` req, `orderId` req | `orderId`, `account`, `price`, `netAmount`, `steamRate` |

Статус депозитного заказа — общий эндпоинт `/v1/deposit/order/{orderId}`.

### Telegram Stars

| Метод | Путь | Запрос | Ответ |
|---|---|---|---|
| GET | `/stars/price` | `username` req | `starPrice`, `minPrice` |
| POST | `/stars` | `username` req, `count` req (50–50000), `amount` req, `description` req, `orderId` req | `orderId`, `username`, `count`, `amount`, `price`, `commission`, `description`, `paymentLink` |
| GET | `/stars/order/{id}` | — | `status`, `username`, `count`, `amount`, `description`, `creationTime` |
| POST | `/stars/order/{id}/confirm` | — | перевод `Review` → `Paid` |
| POST | `/stars/order/{id}/reject` | — | перевод `Review` → `Refunded` |

Заказы дороже порядка 2000 ₽ попадают в статус `Review` и требуют подтверждения.

### Top-Up + эквайринг

| Метод | Путь | Запрос | Ответ |
|---|---|---|---|
| GET | `/v3/topup/all` | — | `categoryId`, `categoryName`, `type`, `fields[]`, `products[]` c `id`, `name`, `price`, `minPrice`, `isAvailable` |
| POST | `/v3/topup` | `topupId` req, `amount` req, `orderId` req, `fields` req, `email` req, `description` req | `orderId`, `amount`, `commission`, `orderPrice`, `topupId`, `email`, `paymentLink` |
| GET | `/v3/topup/orders/{id}` | — | `orderId`, `status`, `amount`, `orderPrice`, `topupId`, `email`, `paymentLink` |

`fields` — набор значений, требуемых конкретной позицией каталога (например
идентификатор игрового аккаунта). Их состав описан в `fields[]` каталога.

### Ваучеры + эквайринг

| Метод | Путь | Запрос | Ответ |
|---|---|---|---|
| GET | `/v3/vouchers/all` | — | `categoryId`, `categoryName`, `type`, `fields[]`, `vouchers[]` c `id`, `name`, `price`, `minPrice`, `isAvailable`, `stock` |
| POST | `/v3/vouchers` | `voucherId` req, `amount` req, `count` req, `orderId` req, `email` req, `description` req | `orderId`, `amount`, `commission`, `orderPrice`, `voucherId`, `count`, `email`, `paymentLink` |
| GET | `/v3/vouchers/order/{id}` | — | `orderId`, `status`, `vouchers[]` (коды), `amount`, `orderPrice`, `voucherId`, `count`, `email` |

Коды ваучеров приходят в статусе заказа и могут появиться с задержкой до 10 минут.

### Top-Up и ваучеры + депозит

| Метод | Путь | Запрос | Ответ |
|---|---|---|---|
| GET | `/v1/deposit/topups` | — | `categories[]` c `id`, `name`, `fields[]`, `products[]` (`id`, `name`, `price`, `isAvailable`) |
| POST | `/v1/deposit/topups` | `topupId` req, `categoryId` req, `orderId` req, `email` req; `fields` опционально | `orderId`, `topupId`, `categoryId`, `price`, `status`, `fields`, `email` |
| GET | `/v1/deposit/vouchers` | — | `categories[]` c `id`, `name`, `products[]` (`id`, `name`, `price`, `stock`, `isAvailable`) |
| POST | `/v1/deposit/vouchers` | `voucherId` req, `categoryId` req, `count` req, `orderId` req, `email` req | `orderId`, `voucherId`, `categoryId`, `count`, `price`, `status`, `codes[]`, `email` |

### Депозитные операции

| Метод | Путь | Ответ |
|---|---|---|
| GET | `/v1/deposit/balance` | `totalBalance`, `frozenBalance`, `availableBalance` |
| GET | `/v1/deposit/order/{orderId}` | `orderId`, `price`, `status`, `type` (`Steam`\|`TopUp`\|`Vouchers`), `details` (может быть пустым) |

### Коды ошибок Digital Goods

`ORD_1001`–`ORD_1007` (заказы), `PL_1001`–`PL_1003` (ссылки),
`STM_1001`–`STM_1004` (Steam), `STR_1001`–`STR_1004` (Stars),
`TPP_1001`–`TPP_1004` (Top-Up), `VCR_1001`–`VCR_1003` (ваучеры).

### Требование к моделям

Поля выше должны быть типизированы. При этом каждая модель ответа обязана
сохранять **исходное тело** (сырой JSON) в отдельном поле: документация WATA
покрывает не все возвращаемые поля, и без этого интегратор не сможет добраться
до недокументированного значения, не дожидаясь новой версии SDK.
