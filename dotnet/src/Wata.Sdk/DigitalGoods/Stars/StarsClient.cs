using Wata.Sdk.Errors;
using Wata.Sdk.Http;

namespace Wata.Sdk.DigitalGoods.Stars;

/// <summary>
/// Telegram Stars. Требует WataOptions.StarsToken — терминал Stars всегда отдельный
/// от терминала Steam. Заказы дороже порога уходят в статус Review и не исполняются
/// автоматически: их нужно явно подтвердить или отклонить.
/// </summary>
public sealed class StarsClient
{
    private readonly WataHttpTransport? _transport;

    internal StarsClient(WataHttpTransport? transport) => _transport = transport;

    private WataHttpTransport Transport => _transport ?? throw new WataConfigurationException(
        "Для работы с Telegram Stars не задан токен терминала. Передайте WataOptions.StarsToken при создании WataClient. " +
        "Обратите внимание: терминал Stars не может совпадать с терминалом Steam.");

    /// <summary>GET /stars/price — стоимость и допустимый диапазон количества звёзд.</summary>
    public Task<StarsPrice> GetPriceAsync(StarsPriceRequest request, CancellationToken cancellationToken = default)
    {
        var query = new QueryStringBuilder()
            .Add("StarsCount", request.StarsCount)
            .Add("Currency", request.Currency)
            .ToString();

        return Transport.SendAsync<StarsPrice>(HttpMethod.Get, "/api/stars/price", query, null, requiresAuth: true, allowRetry: true, cancellationToken);
    }

    /// <summary>POST /stars — создать заказ. Изменяющий запрос — не повторяется при сбое.</summary>
    public Task<StarsOrder> CreateOrderAsync(CreateStarsOrderRequest request, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<StarsOrder>(HttpMethod.Post, "/api/stars", null, request, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>GET /stars/order/{id} — статус заказа.</summary>
    public Task<StarsOrder> GetOrderAsync(string id, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<StarsOrder>(HttpMethod.Get, $"/api/stars/order/{Uri.EscapeDataString(id)}", null, null, requiresAuth: true, allowRetry: true, cancellationToken);

    /// <summary>
    /// POST /stars/order/{id}/confirm — подтвердить заказ в статусе Review. Изменяющий
    /// запрос — не повторяется при сбое.
    /// </summary>
    public Task<StarsOrder> ConfirmOrderAsync(string id, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<StarsOrder>(HttpMethod.Post, $"/api/stars/order/{Uri.EscapeDataString(id)}/confirm", null, null, requiresAuth: true, allowRetry: false, cancellationToken);

    /// <summary>
    /// POST /stars/order/{id}/reject — отклонить заказ в статусе Review. Изменяющий
    /// запрос — не повторяется при сбое.
    /// </summary>
    public Task<StarsOrder> RejectOrderAsync(string id, CancellationToken cancellationToken = default) =>
        Transport.SendAsync<StarsOrder>(HttpMethod.Post, $"/api/stars/order/{Uri.EscapeDataString(id)}/reject", null, null, requiresAuth: true, allowRetry: false, cancellationToken);
}
