using Wata.Sdk.Errors;
using Wata.Sdk.Http;

namespace Wata.Sdk.DigitalGoods.Vouchers;

/// <summary>Ваучеры. Требует WataOptions.VouchersToken.</summary>
public sealed class VouchersClient
{
    private readonly WataHttpTransport? _transport;

    internal VouchersClient(WataHttpTransport? transport) => _transport = transport;

    private WataHttpTransport Transport => _transport ?? throw new WataConfigurationException(
        "Для работы с ваучерами не задан токен терминала. Передайте WataOptions.VouchersToken при создании WataClient.");

    /// <summary>GET /v3/vouchers/all — каталог доступных ваучеров.</summary>
    public Task<IReadOnlyList<VoucherProduct>> GetAllProductsAsync(CancellationToken cancellationToken = default) =>
        Transport.SendAsync<IReadOnlyList<VoucherProduct>>(HttpMethod.Get, "/api/v3/vouchers/all", null, null, requiresAuth: true, allowRetry: true, cancellationToken);

    /// <summary>POST /v3/vouchers — создать заказ (оплата покупателем). Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<VoucherOrder> CreateOrderAsync(CreateVoucherOrderRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<VoucherOrder>(HttpMethod.Post, "/api/v3/vouchers", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>GET /v3/vouchers/order/{id} — статус заказа acquiring-сценария (коды придут в этом же ответе).</summary>
    public Task<VoucherOrder> GetOrderAsync(string id, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<VoucherOrder>(HttpMethod.Get, $"/api/v3/vouchers/order/{Uri.EscapeDataString(id)}", null, null, requiresAuth: true, allowRetry: true, cancellationToken);

    /// <summary>POST /v1/deposit/vouchers — создать заказ с оплатой с депозита мерчанта. Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<VoucherOrder> CreateDepositOrderAsync(CreateVoucherDepositOrderRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<VoucherOrder>(HttpMethod.Post, "/api/v1/deposit/vouchers", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>GET /v1/deposit/vouchers — каталог депозитных ваучеров.</summary>
    public Task<IReadOnlyList<VoucherProduct>> GetDepositProductsAsync(CancellationToken cancellationToken = default) =>
        Transport.SendAsync<IReadOnlyList<VoucherProduct>>(HttpMethod.Get, "/api/v1/deposit/vouchers", null, null, requiresAuth: true, allowRetry: true, cancellationToken);

    /// <summary>GET /v1/deposit/order/{orderId} — общий статус депозитного заказа, с токеном ваучеров.</summary>
    public Task<Deposit.DepositOrder> GetDepositOrderAsync(string orderId, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<Deposit.DepositOrder>(HttpMethod.Get, $"/api/v1/deposit/order/{Uri.EscapeDataString(orderId)}", null, null, requiresAuth: true, allowRetry: true, cancellationToken);
}
