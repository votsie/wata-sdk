using Wata.Sdk.Acquiring;
using Wata.Sdk.DigitalGoods.Deposit;
using Wata.Sdk.DigitalGoods.Stars;
using Wata.Sdk.DigitalGoods.Steam;
using Wata.Sdk.DigitalGoods.TopUp;
using Wata.Sdk.DigitalGoods.Vouchers;
using Wata.Sdk.Http;
using Wata.Sdk.Webhooks;

namespace Wata.Sdk;

/// <summary>
/// Точка входа в SDK. Конструктор принимает готовый <see cref="HttpClient"/> (например,
/// из <c>IHttpClientFactory</c>), чтобы SDK дружил с DI и не плодил сокеты — внутри
/// SDK никогда не создаёт <c>new HttpClient()</c> на каждый вызов.
/// <para>
/// Токен выпускается на терминал: каждый продукт (эквайринг, Stars, Steam, Top-Up,
/// ваучеры) обращается к API только своим токеном — токены между продуктами не
/// переиспользуются. Все токены в <see cref="WataOptions"/> необязательны; обращение
/// к продукту без токена бросает <see cref="Errors.WataConfigurationException"/> ещё
/// до сетевого вызова, называя недостающий токен.
/// </para>
/// </summary>
public sealed class WataClient
{
    /// <summary>Эквайринг (H2H): ссылки, транзакции, возвраты, баланс, прямые платежи.</summary>
    public AcquiringClient Acquiring { get; }

    /// <summary>Telegram Stars — терминал, всегда отдельный от Steam.</summary>
    public StarsClient Stars { get; }

    /// <summary>Steam — терминал, всегда отдельный от Stars.</summary>
    public SteamClient Steam { get; }

    /// <summary>Пополнения (Top-Up).</summary>
    public TopUpClient TopUp { get; }

    /// <summary>Ваучеры.</summary>
    public VouchersClient Vouchers { get; }

    /// <summary>
    /// Общая инфраструктура депозитной оплаты (баланс мерчанта, статус депозитного
    /// заказа), доступная через первый сконфигурированный DG-токен.
    /// </summary>
    public DepositClient Deposit { get; }

    /// <summary>Проверка подписи и разбор вебхуков.</summary>
    public WebhookVerifier Webhooks { get; }

    public WataClient(HttpClient httpClient, WataOptions options)
    {
        ArgumentNullException.ThrowIfNull(httpClient);
        ArgumentNullException.ThrowIfNull(options);

        var acquiringBaseUrl = options.ResolveAcquiringBaseUrl();

        Acquiring = new AcquiringClient(CreateTransport(httpClient, acquiringBaseUrl, options.AcquiringToken, options));
        Webhooks = new WebhookVerifier(httpClient, acquiringBaseUrl, options.Environment);

        // Digital Goods — резолвим базовый адрес лениво: если ни один DG-токен не задан,
        // клиенту незачем падать на "sandbox для DG недоступен" раньше времени.
        var starsToken = options.StarsToken;
        var steamToken = options.SteamToken;
        var topUpToken = options.TopUpToken;
        var vouchersToken = options.VouchersToken;
        var depositToken = steamToken ?? topUpToken ?? vouchersToken;

        Uri? digitalGoodsBaseUrl = null;
        if (starsToken is not null || steamToken is not null || topUpToken is not null || vouchersToken is not null)
            digitalGoodsBaseUrl = options.ResolveDigitalGoodsBaseUrl();

        Stars = new StarsClient(CreateTransport(httpClient, digitalGoodsBaseUrl, starsToken, options));
        Steam = new SteamClient(CreateTransport(httpClient, digitalGoodsBaseUrl, steamToken, options));
        TopUp = new TopUpClient(CreateTransport(httpClient, digitalGoodsBaseUrl, topUpToken, options));
        Vouchers = new VouchersClient(CreateTransport(httpClient, digitalGoodsBaseUrl, vouchersToken, options));
        Deposit = new DepositClient(CreateTransport(httpClient, digitalGoodsBaseUrl, depositToken, options));
    }

    private static WataHttpTransport? CreateTransport(HttpClient httpClient, Uri? baseUrl, string? token, WataOptions options)
    {
        if (token is null || baseUrl is null)
            return null;

        return new WataHttpTransport(httpClient, baseUrl, token, options.Timeout, options.MaxRetryAttempts);
    }
}
