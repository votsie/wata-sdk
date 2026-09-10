using System.Runtime.CompilerServices;
using Wata.Sdk.Acquiring.Models;
using Wata.Sdk.Errors;
using Wata.Sdk.Http;
using Wata.Sdk.Models;

namespace Wata.Sdk.Acquiring;

/// <summary>
/// Эквайринг (H2H): платёжные ссылки, транзакции, возвраты, баланс и прямые платежи.
/// Все методы требуют WataOptions.AcquiringToken — при его отсутствии сразу бросают
/// WataConfigurationException, не выполняя сетевой вызов.
/// </summary>
public sealed class AcquiringClient
{
    private readonly WataHttpTransport? _transport;

    internal AcquiringClient(WataHttpTransport? transport) => _transport = transport;

    private WataHttpTransport Transport => _transport ?? throw new WataConfigurationException(
        "Для работы с эквайрингом не задан токен терминала. Передайте WataOptions.AcquiringToken при создании WataClient.");

    /// <summary>Создать платёжную ссылку. Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<PaymentLink> CreateLinkAsync(CreatePaymentLinkRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<PaymentLink>(HttpMethod.Post, "/api/h2h/links", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>Поиск ссылок (постраничный, через SkipCount/MaxResultCount).</summary>
    public Task<PagedResult<PaymentLink>> SearchLinksAsync(SearchPaymentLinksRequest request, CancellationToken cancellationToken = default)
    {
        var query = new QueryStringBuilder()
            .Add("OrderId", request.OrderId)
            .Add("CreationTimeFrom", request.CreationTimeFrom)
            .Add("CreationTimeTo", request.CreationTimeTo)
            .Add("AmountFrom", request.AmountFrom)
            .Add("AmountTo", request.AmountTo)
            .AddMany("Currencies", request.Currencies)
            .AddMany("Statuses", request.Statuses)
            .Add("Sorting", request.Sorting)
            .Add("SkipCount", request.SkipCount)
            .Add("MaxResultCount", request.MaxResultCount)
            .ToString();

        return Transport.SendAsync<PagedResult<PaymentLink>>(HttpMethod.Get, "/api/h2h/links", query, null, requiresAuth: true, allowRetry: true, cancellationToken);
    }

    /// <summary>Ссылка по идентификатору.</summary>
    public Task<PaymentLink> GetLinkAsync(Guid id, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<PaymentLink>(HttpMethod.Get, $"/api/h2h/links/{id}", null, null, requiresAuth: true, allowRetry: true, cancellationToken);

    /// <summary>
    /// Поиск транзакций с автоматическим обходом всех страниц курсорной пагинации:
    /// CursorId, CursorAmount и CursorDate переносятся между запросами сами — вручную
    /// листать не нужно (ручное листание легко сломать, пропустив CursorDate).
    /// </summary>
    public async IAsyncEnumerable<Transaction> SearchTransactionsAsync(
        SearchTransactionsRequest request,
        [EnumeratorCancellation] CancellationToken cancellationToken = default)
    {
        string? cursorId = null;
        DateTimeOffset? cursorDate = null;
        decimal? cursorAmount = null;
        bool hasNext;

        do
        {
            var page = await SearchTransactionsPageAsync(request, cursorId, cursorAmount, cursorDate, cancellationToken)
                .ConfigureAwait(false);

            foreach (var item in page.Items)
                yield return item;

            hasNext = page.HasNextPage;
            cursorId = page.NextCursorId;
            cursorDate = page.NextCursorDate;
            cursorAmount = page.NextCursorAmount;
        }
        while (hasNext);
    }

    /// <summary>
    /// Одна страница поиска транзакций. Используйте, если вам нужен ручной контроль над
    /// страницами; для сквозного обхода предпочитайте SearchTransactionsAsync.
    /// </summary>
    public Task<CursorPage<Transaction>> SearchTransactionsPageAsync(
        SearchTransactionsRequest request,
        string? cursorId = null,
        decimal? cursorAmount = null,
        DateTimeOffset? cursorDate = null,
        CancellationToken cancellationToken = default)
    {
        var query = new QueryStringBuilder()
            .Add("OrderId", request.OrderId)
            .Add("CreationTimeFrom", request.CreationTimeFrom)
            .Add("CreationTimeTo", request.CreationTimeTo)
            .Add("AmountFrom", request.AmountFrom)
            .Add("AmountTo", request.AmountTo)
            .AddMany("Currencies", request.Currencies)
            .AddMany("PaymentLinkIds", request.PaymentLinkIds)
            .AddMany("Statuses", request.Statuses)
            .Add("Sorting", request.Sorting)
            .Add("MaxResultCount", request.MaxResultCount)
            .Add("CursorId", cursorId)
            .Add("CursorAmount", cursorAmount)
            .Add("CursorDate", cursorDate)
            .ToString();

        return Transport.SendAsync<CursorPage<Transaction>>(HttpMethod.Get, "/api/h2h/v2/transactions", query, null, requiresAuth: true, allowRetry: true, cancellationToken);
    }

    /// <summary>Транзакция по идентификатору.</summary>
    public Task<Transaction> GetTransactionAsync(Guid id, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<Transaction>(HttpMethod.Get, $"/api/h2h/transactions/{id}", null, null, requiresAuth: true, allowRetry: true, cancellationToken);

    /// <summary>Возврат. Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<RefundResponse> RefundAsync(RefundRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<RefundResponse>(HttpMethod.Post, "/api/h2h/transactions/refunds", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>
    /// Баланс терминала на дату. Допустимы только сегодняшняя и вчерашняя дата по UTC —
    /// это проверяется локально, до сетевого вызова, и при нарушении бросает
    /// WataConfigurationException.
    /// </summary>
    public Task<Balance> GetBalanceAsync(DateOnly date, CancellationToken cancellationToken = default)
    {
        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var yesterday = today.AddDays(-1);

        if (date != today && date != yesterday)
        {
            throw new WataConfigurationException(
                $"Баланс доступен только за сегодня ({today:yyyy-MM-dd}) или вчера ({yesterday:yyyy-MM-dd}) по UTC (запрошено {date:yyyy-MM-dd}).");
        }

        var query = new QueryStringBuilder().Add("Date", date).ToString();
        return Transport.SendAsync<Balance>(HttpMethod.Get, "/api/h2h/finance/balance", query, null, requiresAuth: true, allowRetry: true, cancellationToken);
    }

    /// <summary>Оплата картой по криптограмме. Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<CardCryptoPaymentResponse> PayCardCryptoAsync(CardCryptoPaymentRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<CardCryptoPaymentResponse>(HttpMethod.Post, "/api/h2h/payments/card-crypto", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>Оплата СБП. Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<SbpPaymentResponse> PaySbpAsync(SbpPaymentRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<SbpPaymentResponse>(HttpMethod.Post, "/api/h2h/payments/sbp", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>Оплата T-Pay. Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<TPayPaymentResponse> PayTPayAsync(TPayPaymentRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<TPayPaymentResponse>(HttpMethod.Post, "/api/h2h/payments/tpay", null, request, requiresAuth: true, allowRetry: false, cancellationToken);
}
