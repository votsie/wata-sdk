using Wata.Sdk.Errors;
using Wata.Sdk.Http;

namespace Wata.Sdk.DigitalGoods.Deposit;

/// <summary>
/// Общая инфраструктура депозитной оплаты (баланс мерчанта, статус депозитного заказа).
/// Эти эндпоинты общие для Steam/Top-Up/ваучеров, оплачиваемых с депозита, поэтому
/// клиент использует первый сконфигурированный токен из SteamToken, TopUpToken,
/// VouchersToken — какой у вас есть, тот и подойдёт для проверки баланса и статуса.
/// Если ни один из этих токенов не задан, обращение бросает WataConfigurationException.
/// </summary>
public sealed class DepositClient
{
    private readonly WataHttpTransport? _transport;

    internal DepositClient(WataHttpTransport? transport) => _transport = transport;

    private WataHttpTransport Transport => _transport ?? throw new WataConfigurationException(
        "Для работы с депозитом цифровых товаров не задан ни один из токенов: SteamToken, TopUpToken, VouchersToken. " +
        "Передайте хотя бы один из них при создании WataClient.");

    /// <summary>GET /v1/deposit/balance — баланс депозита мерчанта.</summary>
    public Task<DepositBalance> GetBalanceAsync(CancellationToken cancellationToken = default) =>
        Transport.SendAsync<DepositBalance>(HttpMethod.Get, "/api/v1/deposit/balance", null, null, requiresAuth: true, allowRetry: true, cancellationToken);

    /// <summary>GET /v1/deposit/order/{orderId} — общий статус депозитного заказа (Steam/Top-Up/ваучеры).</summary>
    public Task<DepositOrder> GetOrderAsync(string orderId, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<DepositOrder>(HttpMethod.Get, $"/api/v1/deposit/order/{Uri.EscapeDataString(orderId)}", null, null, requiresAuth: true, allowRetry: true, cancellationToken);
}
