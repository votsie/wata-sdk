using Wata.Sdk.Errors;
using Wata.Sdk.Http;
using Wata.Sdk.Models;

namespace Wata.Sdk.DigitalGoods.Steam;

/// <summary>
/// Пополнение Steam. Требует WataOptions.SteamToken — терминал Steam всегда отдельный
/// от терминала Telegram Stars, попытка использовать чужой токен приведёт к отказу
/// на стороне сервера, а без токена SDK не даст выполнить сетевой вызов вовсе.
/// </summary>
public sealed class SteamClient
{
    private readonly WataHttpTransport? _transport;

    internal SteamClient(WataHttpTransport? transport) => _transport = transport;

    private WataHttpTransport Transport => _transport ?? throw new WataConfigurationException(
        "Для работы со Steam не задан токен терминала. Передайте WataOptions.SteamToken при создании WataClient. " +
        "Обратите внимание: терминал Steam не может совпадать с терминалом Stars.");

    // --- Оплата покупателем (acquiring) ---

    /// <summary>GET /v3/steam/amount — котировка по сумме зачисления на аккаунт.</summary>
    public Task<SteamQuote> GetAmountQuoteAsync(SteamAmountQuoteRequest request, CancellationToken cancellationToken = default)
    {
        var query = new QueryStringBuilder()
            .Add("SteamLogin", request.SteamLogin)
            .Add("NetAmount", request.NetAmount)
            .Add("Currency", request.Currency)
            .ToString();

        return Transport.SendAsync<SteamQuote>(HttpMethod.Get, "/api/v3/steam/amount", query, null, requiresAuth: true, allowRetry: true, cancellationToken);
    }

    /// <summary>GET /v3/steam/by-amount — котировка по сумме платежа.</summary>
    public Task<SteamQuote> GetQuoteByAmountAsync(SteamByAmountQuoteRequest request, CancellationToken cancellationToken = default)
    {
        var query = new QueryStringBuilder()
            .Add("SteamLogin", request.SteamLogin)
            .Add("Amount", request.Amount)
            .Add("Currency", request.Currency)
            .ToString();

        return Transport.SendAsync<SteamQuote>(HttpMethod.Get, "/api/v3/steam/by-amount", query, null, requiresAuth: true, allowRetry: true, cancellationToken);
    }

    /// <summary>POST /v3/steam — создать заказ по сумме зачисления. Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<SteamOrder> CreateOrderAsync(CreateSteamOrderRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<SteamOrder>(HttpMethod.Post, "/api/v3/steam", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>POST /v3/steam/by-amount — создать заказ по сумме платежа. Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<SteamOrder> CreateOrderByAmountAsync(CreateSteamOrderByAmountRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<SteamOrder>(HttpMethod.Post, "/api/v3/steam/by-amount", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>GET /v3/steam/order/{id} — статус заказа acquiring-сценария.</summary>
    public Task<SteamOrder> GetOrderAsync(string id, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<SteamOrder>(HttpMethod.Get, $"/api/v3/steam/order/{Uri.EscapeDataString(id)}", null, null, requiresAuth: true, allowRetry: true, cancellationToken);

    // --- Оплата с депозита мерчанта (deposit) ---

    /// <summary>GET /v1/steam/deposit/price — сколько спишется с депозита за заданную сумму зачисления.</summary>
    public Task<SteamQuote> GetDepositPriceAsync(SteamDepositPriceRequest request, CancellationToken cancellationToken = default)
    {
        var query = new QueryStringBuilder()
            .Add("SteamLogin", request.SteamLogin)
            .Add("Amount", request.Amount)
            .ToString();

        return Transport.SendAsync<SteamQuote>(HttpMethod.Get, "/api/v1/steam/deposit/price", query, null, requiresAuth: true, allowRetry: true, cancellationToken);
    }

    /// <summary>GET /v1/steam/deposit/netamount — сколько зачислится на аккаунт при списании заданной суммы с депозита.</summary>
    public Task<SteamQuote> GetDepositNetAmountAsync(SteamDepositNetAmountRequest request, CancellationToken cancellationToken = default)
    {
        var query = new QueryStringBuilder()
            .Add("SteamLogin", request.SteamLogin)
            .Add("NetAmount", request.NetAmount)
            .ToString();

        return Transport.SendAsync<SteamQuote>(HttpMethod.Get, "/api/v1/steam/deposit/netamount", query, null, requiresAuth: true, allowRetry: true, cancellationToken);
    }

    /// <summary>POST /v1/steam/deposit — создать депозитный заказ по сумме зачисления. Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<SteamOrder> CreateDepositOrderAsync(CreateSteamDepositOrderRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<SteamOrder>(HttpMethod.Post, "/api/v1/steam/deposit", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>POST /v1/steam/deposit/by-price — создать депозитный заказ по сумме списания. Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<SteamOrder> CreateDepositOrderByPriceAsync(CreateSteamDepositOrderByPriceRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<SteamOrder>(HttpMethod.Post, "/api/v1/steam/deposit/by-price", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>
    /// GET /v1/deposit/order/{orderId} — общий статус депозитного заказа. Эквивалентно
    /// WataClient.Deposit.GetOrderAsync, но использует именно токен Steam.
    /// </summary>
    public Task<Deposit.DepositOrder> GetDepositOrderAsync(string orderId, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<Deposit.DepositOrder>(HttpMethod.Get, $"/api/v1/deposit/order/{Uri.EscapeDataString(orderId)}", null, null, requiresAuth: true, allowRetry: true, cancellationToken);
}
