# wata — Python SDK для WATA

Официальный неофициальный Python SDK для платёжной системы
[WATA](https://wata.pro): эквайринг (H2H) и цифровые товары (Steam, Telegram
Stars, Top-Up, ваучеры). Реализован по единому контракту `docs/SPEC.md`,
общему для всех SDK WATA (Node, Python, Go, Java, .NET).

## Установка

```bash
pip install wata
# для проверки подписи вебхуков (RSA) нужен дополнительный пакет cryptography:
pip install wata[webhooks]
```

Требуется Python 3.10+.

## Ключевое ограничение модели доступа WATA

**Токен WATA выпускается на терминал, а не на аккаунт.** Терминал нигде не
передаётся в запросе — сервер определяет его по токену.

> **Stars и Steam всегда живут на отдельных терминалах**, то есть требуют
> собственных токенов. Это не рекомендация, а свойство платформы: вызов Stars
> с токеном Steam гарантированно завершится ошибкой на стороне WATA.

Поэтому `WataClient` принимает **набор токенов по продуктам** — каждый
продукт получает собственный HTTP-клиент со своим токеном, и токены между
продуктами никогда не переиспользуются:

```python
from wata import WataClient

client = WataClient(
    acquiring="<jwt терминала эквайринга>",
    stars="<jwt терминала Telegram Stars>",
    steam="<jwt терминала Steam>",
    topup="<jwt терминала пополнений>",
    vouchers="<jwt терминала ваучеров>",
)
```

Все токены необязательны — клиент можно создать с любым подмножеством.
Обращение к продукту, токен которого не задан, **немедленно** бросает
`WataConfigError` ещё до сетевого вызова, с текстом, называющим недостающий
токен:

```python
client = WataClient(acquiring="...")
client.stars.price(quantity=100)
# wata.errors.WataConfigError: Токен продукта 'stars' не задан в WataClient —
# обращение к этому продукту невозможно. Напоминание: Stars и Steam всегда
# живут на отдельных терминалах и требуют собственных токенов.
```

Если у мерчанта один терминал совмещает эквайринг и цифровые товары — просто
передайте один и тот же токен в несколько полей. Это осознанное решение
интегратора, а не догадка SDK.

### Таблица токенов по продуктам

| Параметр `WataClient` | Продукт | Хост |
|---|---|---|
| `acquiring` | Эквайринг (H2H): ссылки, транзакции, возвраты, баланс, прямые платежи | `api.wata.pro` (`api-sandbox.wata.pro` для песочницы) |
| `steam` | Steam (пополнение баланса) | `dg-api.wata.pro` |
| `stars` | Telegram Stars | `dg-api.wata.pro` |
| `topup` | Top-Up (пополнения игровых аккаунтов) | `dg-api.wata.pro` |
| `vouchers` | Ваучеры | `dg-api.wata.pro` |

Для цифровых товаров отдельного адреса песочницы не существует — если
`environment="sandbox"` и при этом задан хотя бы один DG-токен, SDK явно
бросит `WataConfigError` вместо того, чтобы молча пойти в боевой контур.

## Быстрый старт

```python
from wata import WataClient

client = WataClient(acquiring="<jwt терминала эквайринга>")

link = client.acquiring.links.create(
    amount=1500,
    currency="RUB",
    description="Заказ №42",
    order_id="order-42",
    success_redirect_url="https://shop.example/success",
)
print(link.url)     # ссылка на оплату
print(link.status)  # PaymentLinkStatus.OPENED
```

### Приём и проверка вебхука (Flask-подобный пример)

```python
from wata import WataClient, WataWebhookError

client = WataClient(acquiring="<jwt терминала эквайринга>")

def handle_webhook(raw_body: bytes, signature_header: str) -> None:
    # raw_body — СЫРОЕ тело запроса (bytes), а не разобранный dict:
    # пересборка JSON меняет порядок ключей и ломает подпись.
    if not client.verify_webhook(raw_body, signature_header):
        raise WataWebhookError("Подпись вебхука не совпала")

    event = client.parse_webhook(raw_body)
    print(event.transaction_status, event.order_id, event.amount, event.currency)
    # Обработчик обязан вернуть HTTP 200 и быть идемпотентным — это
    # ответственность приложения, SDK этого не делает за вас.
```

Полный рабочий пример — в `examples/`.

### Курсорная пагинация транзакций

```python
for tx in client.acquiring.transactions.iter_all(statuses=["Paid"]):
    print(tx.id, tx.amount, tx.status)
```

Генератор сам переносит `CursorId`/`CursorAmount`/`CursorDate` между
запросами — ручное листание является частым источником ошибок (пропуск
`CursorDate` ломает вторую страницу).

### Асинхронный клиент

```python
import asyncio
from wata import AsyncWataClient

async def main() -> None:
    async with AsyncWataClient(acquiring="<jwt>") as client:
        link = await client.acquiring.links.create(amount=1000, currency="RUB")
        print(link.url)

asyncio.run(main())
```

## Поведение, о котором важно знать

- **Изменяющие запросы** (создание ссылки/заказа/платежа/возврата) **не
  повторяются** автоматически при сбое — повтор мог бы создать второй платёж.
  Повторяются только сетевые ошибки и `5xx` для запросов, не меняющих
  состояние, с экспоненциальной задержкой и джиттером (по умолчанию 3
  попытки).
- `429 Too Many Requests` не ретраится в цикле — бросается
  `WataRateLimitError` с `retry_after`, если сервер его указал.
- Баланс терминала (`client.acquiring.balance.get(date)`) доступен только за
  сегодняшнюю или вчерашнюю дату по UTC — SDK проверяет это локально и
  бросает `WataConfigError` до сетевого вызова.
- Неизвестное значение enum, пришедшее от сервера (WATA API живой и может
  добавить новое значение), не ломает разбор ответа: исходная строка
  сохраняется в объекте enum вместо падения с исключением.
- Секреты (токен, тело запроса с картой) никогда не попадают в текст ошибки.

## Иерархия ошибок

```
WataError
├── WataConfigError     # нет токена продукта, неверное окружение, недопустимая дата баланса — до сетевого вызова
├── WataAuthError        # 401/403
├── WataRateLimitError   # 429, есть .retry_after
├── WataApiError         # 4xx с кодом WATA (PL_*, TRA_*, ORD_*, STM_*, STR_*, TPP_*, VCR_*)
├── WataServerError      # 5xx после исчерпания повторов
├── WataNetworkError     # таймаут, обрыв соединения
└── WataWebhookError     # подпись не сошлась / ключ не получен
```

## Модели цифровых товаров: `DgObject`

`docs/SPEC.md` не фиксирует исчерпывающий список полей JSON-ответа для
Steam/Stars/Top-Up/ваучеров (в отличие от эквайринга, где формат полностью
описан). Чтобы не терять данные и не изобретать несуществующие имена полей,
такие ответы возвращаются как гибкий `DgObject`:

```python
order = client.steam.create_by_net_amount(net_amount=500, login="steam_login")
order.status       # типизированный DgOrderStatus, если поле есть в ответе
order.order_id     # атрибут по snake_case имени -> ищет "orderId" в сыром JSON
order.raw          # исходный словарь целиком, ничего не потеряно
```

## Разработка и тесты

```bash
pip install -e ".[dev]"
pytest
```

Тесты работают полностью без сети — HTTP замокан через `respx`.

## Версия

`0.1.0` — синхронна с версиями SDK для Node, Go, Java и .NET.
