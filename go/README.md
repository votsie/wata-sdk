# wata-sdk-go

Неофициальный Go SDK для платёжной платформы WATA: эквайринг (H2H) и
цифровые товары (Telegram Stars, Steam, пополнения, ваучеры).

Версия: `0.1.0`. Зависимости: только стандартная библиотека Go (`net/http`,
`encoding/json`, `crypto/rsa`, `crypto/sha512`, `crypto/x509`,
`encoding/pem` и т.д.) — внешних пакетов нет и не планируется.

Требуется Go 1.21+.

## Установка

```bash
go get github.com/votsie/wata-sdk/go
```

Импортируется под именем пакета `wata`:

```go
import "github.com/votsie/wata-sdk/go"
```

## Ключевая модель: токен на терминал, не на аккаунт

Токен WATA выпускается на **терминал**, а не на аккаунт мерчанта, и сам
терминал нигде не передаётся в запросе — сервер определяет его по токену.

**Stars и Steam всегда живут на отдельных терминалах.** Это свойство самой
платформы, а не рекомендация: вызов Stars с токеном терминала Steam будет
отклонён сервером. Поэтому `wata.Client` принимает **набор токенов по
продуктам**, и обращение к продукту без своего токена возвращает ошибку
конфигурации ещё до сетевого запроса — с этим именем токена в тексте:

```go
client := wata.New(wata.Tokens{
    Acquiring: os.Getenv("WATA_ACQUIRING_TOKEN"),
    Stars:     os.Getenv("WATA_STARS_TOKEN"),
    Steam:     os.Getenv("WATA_STEAM_TOKEN"),
    TopUp:     os.Getenv("WATA_TOPUP_TOKEN"),
    Vouchers:  os.Getenv("WATA_VOUCHERS_TOKEN"),
})
```

Все поля `Tokens` необязательны — создавайте клиент с любым подмножеством.
Каждый продукт использует собственный HTTP-запрос со своим токеном; токены
между продуктами никогда не переиспользуются автоматически. Если у вас один
терминал физически совмещает эквайринг и цифровые товары, вы можете
осознанно передать один и тот же токен в несколько полей — это ваше решение,
SDK его не подразумевает.

### Таблица токенов по продуктам

| Поле `Tokens` | Продукт | Базовый URL (боевой) |
|---|---|---|
| `Acquiring` | Эквайринг / H2H: ссылки, транзакции, возвраты, баланс, прямые платежи | `https://api.wata.pro` |
| `Stars` | Telegram Stars | `https://dg-api.wata.pro` |
| `Steam` | Пополнение Steam | `https://dg-api.wata.pro` |
| `TopUp` | Пополнения (мобильная связь и т.п.) | `https://dg-api.wata.pro` |
| `Vouchers` | Ваучеры | `https://dg-api.wata.pro` |

Песочница (`wata.Sandbox`) документирована только для эквайринга
(`https://api-sandbox.wata.pro`). Для цифровых товаров отдельного адреса
песочницы нет: если клиент создан с `wata.Sandbox` и вы вызываете метод
Stars/Steam/TopUp/Vouchers, SDK вернёт `*wata.ConfigError` вместо того,
чтобы молча уйти в боевой контур.

## Быстрый старт

```go
package main

import (
    "context"
    "fmt"
    "log"

    "github.com/votsie/wata-sdk/go"
)

func main() {
    client := wata.New(wata.Tokens{Acquiring: "<jwt терминала эквайринга>"})

    link, err := client.Acquiring.CreateLink(context.Background(), wata.CreateLinkRequest{
        Amount:      1500,
        Currency:    wata.CurrencyRUB,
        Description: "Заказ №42",
        OrderID:     "order-42",
    })
    if err != nil {
        log.Fatal(err)
    }
    fmt.Println("ссылка на оплату:", link.URL)
}
```

Рабочий сквозной пример (создание ссылки + приём вебхука с проверкой
подписи) лежит в
[`examples/create-link-and-webhook`](examples/create-link-and-webhook/main.go).

## Опции клиента

```go
client := wata.New(tokens,
    wata.WithEnvironment(wata.Sandbox),           // по умолчанию wata.Production
    wata.WithTimeout(30*time.Second),              // по умолчанию 60с, как в API
    wata.WithMaxRetries(5),                        // по умолчанию 3
    wata.WithHTTPClient(customHTTPClient),
)
```

## Ошибки

Иерархия ошибок (все реализуют `error`, у всех можно достать общий `*wata.Error`
через `errors.As`):

- `wata.ConfigError` — проблема, которую SDK обнаружил локально, **до**
  сетевого вызова: не задан токен продукта, запрошена недокументированная
  песочница цифровых товаров, недопустимая дата баланса.
- `wata.AuthError` — HTTP 401/403: токен истёк, отозван, принадлежит другому
  терминалу, либо запрос идёт с несогласованного IP.
- `wata.RateLimitError` — HTTP 429; поле `RetryAfter` (`*time.Duration`)
  заполняется, если сервер прислал заголовок `Retry-After`. SDK никогда не
  ретраит 429 самостоятельно.
- `wata.APIError` — прочие `4xx`; несёт код ошибки WATA (`PL_*`, `TRA_*`,
  `ORD_*`, `STM_*`, `STR_*`, `TPP_*`, `VCR_*`) в `Err.Code`, а также
  `Details` и `ValidationErrors` из тела ответа.
- `wata.ServerError` — `5xx` после исчерпания повторов.
- `wata.NetworkError` — таймаут, обрыв соединения; оборачивает исходную
  ошибку транспорта (доступна через `errors.Is`/`errors.Unwrap`, включая
  `context.DeadlineExceeded`).
- `wata.WebhookError` — подпись вебхука не сошлась ошибкой (а не просто
  `false`), либо не удалось получить/разобрать публичный ключ, либо тело не
  разобралось как JSON.

```go
_, err := client.Acquiring.Refund(ctx, req)
var apiErr *wata.APIError
if errors.As(err, &apiErr) {
    fmt.Println(apiErr.Err.Code, apiErr.Err.Message)
}
```

Секреты (токены, тело с картой) никогда не попадают в текст ошибки.

## Повторы запросов

- GET-запросы повторяются при сетевых ошибках и ответах `5xx`: до 3 попыток
  по умолчанию (настраивается через `WithMaxRetries`), с экспоненциальной
  задержкой и джиттером.
- **Изменяющие состояние запросы — создание ссылки, платежа, возврата — по
  умолчанию НЕ повторяются никогда**, независимо от `WithMaxRetries`: повтор
  после сетевого сбоя мог бы создать второй платёж.
- `429` никогда не ретраится автоматически — SDK возвращает
  `*wata.RateLimitError` сразу.

## Курсорная пагинация транзакций

`GET /v2/transactions` — курсорная: чтобы получить вторую страницу, нужно
передать серверу `CursorId`, `CursorAmount` и `CursorDate` из ответа
предыдущей страницы. Забыть `CursorDate` — типичная ошибка, которая тихо
ломает вторую страницу.

Вместо ручного листания используйте `AcquiringService.Transactions` — он сам
переносит все три курсорных поля:

```go
it := client.Acquiring.Transactions(ctx, wata.SearchTransactionsParams{
    Statuses: []wata.TransactionStatus{wata.TransactionStatusPaid},
})
for it.Next() {
    tx := it.Transaction()
    fmt.Println(tx.ID, tx.Amount, tx.Status)
}
if err := it.Err(); err != nil {
    log.Fatal(err)
}
```

Это тип с методами `Next()`/`Transaction()`/`Err()`, а не `iter.Seq2` из Go
1.23 — модуль целится в Go 1.21 (минимум, который просит спецификация SDK),
а этот явный тип работает без изменений начиная с 1.21 и является тем же
паттерном, что `bufio.Scanner`/`sql.Rows` в стандартной библиотеке. На Go
1.23+ вокруг `Next`/`Transaction`/`Err` легко пишется двухстрочный адаптер
под `range`; закладывать `iter.Seq2` прямо в публичный API означало бы либо
поднять минимальную версию модуля выше требуемой, либо городить файл под
build-тег ради одной формы цикла.

## Вебхуки

- Заголовок подписи: `X-Signature`, значение в base64.
- Алгоритм: **SHA512withRSA** (RSA PKCS#1 v1.5 + SHA-512) — не SHA-256.
- Ключ: `GET /api/h2h/public-key`, поле `value`, PEM. Вызывается без
  авторизации — единственный такой эндпоинт во всём API.
- Проверяется **сырое тело запроса**, до разбора JSON.

```go
func handler(w http.ResponseWriter, r *http.Request) {
    rawBody, _ := io.ReadAll(r.Body)
    signature := r.Header.Get("X-Signature")

    ok, err := client.VerifyWebhook(r.Context(), rawBody, signature)
    if err != nil {
        http.Error(w, "verification failed", http.StatusInternalServerError)
        return
    }
    if !ok {
        http.Error(w, "invalid signature", http.StatusBadRequest)
        return
    }

    event, err := wata.ParseWebhook(rawBody)
    if err != nil {
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }
    // ... идемпотентно обработать event ...
    w.WriteHeader(http.StatusOK)
}
```

`VerifyWebhook` подтягивает и кэширует ключ автоматически, отдельно на
каждый `Client` (а значит — на каждое окружение: у боевого контура и
песочницы разные ключи, и они никогда не путаются, потому что не хранятся
в общем кэше между разными `Client`). Есть варианты:

- `client.VerifyWebhook(ctx, rawBody []byte, signature string)` — ключ
  подтягивается и кэшируется.
- `client.VerifyWebhookString(ctx, rawBody string, signature string)` —
  то же самое для тела в виде строки.
- `wata.VerifyWebhookWithKey(rawBody []byte, signature string, keyPEM string)`
  — ключ передан явно, без сети и без кэша клиента.

Обратите внимание: во всех вариантах `rawBody` — это `[]byte`/`string`,
**никогда** разобранная структура. Пересборка JSON из объекта меняет порядок
ключей и ломает подпись — сигнатуры методов физически не принимают структуру
именно поэтому.

`wata.ParseWebhook(rawBody []byte) (*wata.WebhookEvent, error)` разбирает
тело в типизированное событие, но **не** проверяет подпись — вызывайте оба
метода на одних и тех же байтах.

Обратите внимание на несогласованность самого API: в вебхуке комиссия
называется `commission`, а в ответе `GET /transactions/{id}` то же значение
называется `totalCommission`. Это не опечатка в SDK, а особенность WATA,
которую стоит иметь в виду при сверке сумм.

## Баланс: только сегодня или вчера по UTC

`Acquiring.Balance` проверяет дату локально и возвращает `*wata.ConfigError`
до сетевого вызова, если дата не сегодняшняя и не вчерашняя по UTC:

```go
bal, err := client.Acquiring.Balance(ctx, time.Now())
```

## Цифровые товары: что документировано, а что — нет

SPEC.md, общий контракт для всех пяти SDK, подробно описывает пути и
семантику эндпоинтов цифровых товаров, но не всегда называет точные имена
JSON-полей тела запроса (например, поле для количества звёзд в заказе
Stars). Там, где имя поля прямо названо в спецификации (Steam: `netAmount` /
`amount`; баланс депозита: `totalBalance`/`frozenBalance`/`availableBalance`),
оно есть в виде типизированного поля Go-структуры. Там, где спецификация
не называет поле, метод принимает `map[string]any`/`extra map[string]any`,
а каждый ответ встраивает `RawBody` с полем `Raw json.RawMessage` — сырым
JSON ответа, — чтобы вы могли прочитать любое поле, даже не описанное в этом
SDK, не дожидаясь новой версии:

```go
order, err := client.Steam.CreateOrderByNetAmount(ctx, 1000, map[string]any{
    "accountLogin": "somebody", // поле, которое не называет SPEC.md
})
var extra struct{ SomeField string `json:"someField"` }
json.Unmarshal(order.Raw, &extra)
```

### Telegram Stars: заказы дороже порога уходят в ревью

Количество звёзд: 50–50000. Заказ дороже определённого порога получает
статус `wata.StarsOrderStatusReview` и **не выполняется**, пока его явно не
подтвердят:

```go
order, _ := client.Stars.CreateOrder(ctx, map[string]any{"count": 20000, /* ... */})
if order.Status == wata.StarsOrderStatusReview {
    // потребуется client.Stars.ConfirmOrder(ctx, order.ID)
    // или client.Stars.RejectOrder(ctx, order.ID)
}
```

### Steam: сумма зачисления vs сумма платежа

У Steam два разных сценария в зависимости от того, что задано: сумма,
которая должна зачислиться на аккаунт (`netAmount`), или сумма, которую
платит покупатель (`amount`/`price`) — комиссия и курс делают эти величины
разными. В SDK это два явных набора методов:
`AmountForNetAmount`/`NetAmountForAmount` и
`CreateOrderByNetAmount`/`CreateOrderByAmount` (и их аналоги
`.../Deposit...` для оплаты с депозита мерчанта).

## Идемпотентность enum'ов

Все перечисления (`Currency`, `TransactionStatus`, `PaymentLinkStatus` и
т.д.) — это строковые типы Go, а не закрытые целочисленные enum'ы. Если
живой API вернёт значение, которого эта версия SDK ещё не знает, разбор
ответа не упадёт — вы получите исходную строку в соответствующем поле.

## Тесты

```bash
go test ./...
```

Тесты используют только `net/http/httptest` — реальная сеть не требуется.
Покрытие включает: отсутствие токена продукта (и что токен одного продукта
не открывает доступ к другому), проверку подписи вебхука (валидная и
подделанная), курсорную пагинацию через две страницы, разбор `APIError` с
кодом WATA, `AuthError`/`RateLimitError`, отказ от повтора изменяющего
запроса на фоне повтора GET, проверку даты баланса, отказ от песочницы для
цифровых товаров и устойчивость к неизвестным значениям enum.

## Лицензия

См. корень репозитория `wata-sdk`.
