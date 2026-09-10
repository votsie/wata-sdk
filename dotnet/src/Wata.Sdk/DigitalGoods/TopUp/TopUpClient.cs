using Wata.Sdk.Errors;
using Wata.Sdk.Http;

namespace Wata.Sdk.DigitalGoods.TopUp;

/// <summary>Пополнения (Top-Up). Требует WataOptions.TopUpToken.</summary>
public sealed class TopUpClient
{
    private readonly WataHttpTransport? _transport;

    internal TopUpClient(WataHttpTransport? transport) => _transport = transport;

    private WataHttpTransport Transport => _transport ?? throw new WataConfigurationException(
        "Для работы с Top-Up не задан токен терминала. Передайте WataOptions.TopUpToken при создании WataClient.");

    /// <summary>GET /v3/topup/all — каталог доступных продуктов пополнения.</summary>
    public Task<IReadOnlyList<TopUpProduct>> GetAllProductsAsync(CancellationToken cancellationToken = default) =>
        Transport.SendAsync<IReadOnlyList<TopUpProduct>>(HttpMethod.Get, "/api/v3/topup/all", null, null, requiresAuth: true, allowRetry: true, cancellationToken);

    /// <summary>POST /v3/topup — создать заказ (оплата покупателем). Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<TopUpOrder> CreateOrderAsync(CreateTopUpOrderRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<TopUpOrder>(HttpMethod.Post, "/api/v3/topup", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>GET /v3/topup/orders/{id} — статус заказа acquiring-сценария.</summary>
    public Task<TopUpOrder> GetOrderAsync(string id, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<TopUpOrder>(HttpMethod.Get, $"/api/v3/topup/orders/{Uri.EscapeDataString(id)}", null, null, requiresAuth: true, allowRetry: true, cancellationToken);

    /// <summary>POST /v1/deposit/topups — создать заказ с оплатой с депозита мерчанта. Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<TopUpOrder> CreateDepositOrderAsync(CreateTopUpDepositOrderRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<TopUpOrder>(HttpMethod.Post, "/api/v1/deposit/topups", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>GET /v1/deposit/topups — каталог депозитных продуктов пополнения.</summary>
    public Task<IReadOnlyList<TopUpProduct>> GetDepositProductsAsync(CancellationToken cancellationToken = default) =>
        Transport.SendAsync<IReadOnlyList<TopUpProduct>>(HttpMethod.Get, "/api/v1/deposit/topups", null, null, requiresAuth: true, allowRetry: true, cancellationToken);

    /// <summary>GET /v1/deposit/order/{orderId} — общий статус депозитного заказа, с токеном Top-Up.</summary>
    public Task<Deposit.DepositOrder> GetDepositOrderAsync(string orderId, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<Deposit.DepositOrder>(HttpMethod.Get, $"/api/v1/deposit/order/{Uri.EscapeDataString(orderId)}", null, null, requiresAuth: true, allowRetry: true, cancellationToken);
}
