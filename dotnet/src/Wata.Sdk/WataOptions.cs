namespace Wata.Sdk;

/// <summary>Окружение WATA.</summary>
public enum WataEnvironment
{
    Production,
    Sandbox,
}

/// <summary>
/// Настройки клиента WATA. Токен выпускается на терминал, а не на аккаунт — Stars
/// и Steam всегда работают на отдельных терминалах и требуют собственных токенов
/// (см. README, раздел "Терминалы и токены"). Все токены необязательны: клиент
/// создаётся с любым подмножеством, а обращение к продукту без токена бросает
/// WataConfigurationException до сетевого вызова.
/// </summary>
public sealed class WataOptions
{
    /// <summary>Токен терминала эквайринга (H2H): ссылки, транзакции, возвраты, баланс, прямые платежи.</summary>
    public string? AcquiringToken { get; set; }

    /// <summary>Токен терминала Telegram Stars. Не может быть тем же терминалом, что и Steam.</summary>
    public string? StarsToken { get; set; }

    /// <summary>Токен терминала Steam. Не может быть тем же терминалом, что и Stars.</summary>
    public string? SteamToken { get; set; }

    /// <summary>Токен терминала пополнений (Top-Up).</summary>
    public string? TopUpToken { get; set; }

    /// <summary>Токен терминала ваучеров.</summary>
    public string? VouchersToken { get; set; }

    /// <summary>Окружение. По умолчанию — боевое (Production).</summary>
    public WataEnvironment Environment { get; set; } = WataEnvironment.Production;

    /// <summary>Таймаут ответа API. По умолчанию 60 секунд, как и лимит самого API.</summary>
    public TimeSpan Timeout { get; set; } = TimeSpan.FromSeconds(60);

    /// <summary>
    /// Максимальное число попыток для сетевых ошибок и 5xx на идемпотентных (обычно GET) запросах.
    /// Изменяющие запросы (создание ссылки/заказа/платежа/возврата) никогда не повторяются
    /// автоматически, независимо от этого значения.
    /// </summary>
    public int MaxRetryAttempts { get; set; } = 3;

    /// <summary>Переопределение базового адреса эквайринга (используется в тестах).</summary>
    public Uri? AcquiringBaseUrlOverride { get; set; }

    /// <summary>Переопределение базового адреса цифровых товаров (используется в тестах).</summary>
    public Uri? DigitalGoodsBaseUrlOverride { get; set; }

    internal static readonly Uri AcquiringProductionUrl = new("https://api.wata.pro");
    internal static readonly Uri AcquiringSandboxUrl = new("https://api-sandbox.wata.pro");
    internal static readonly Uri DigitalGoodsProductionUrl = new("https://dg-api.wata.pro");

    internal Uri ResolveAcquiringBaseUrl()
    {
        if (AcquiringBaseUrlOverride is not null)
            return AcquiringBaseUrlOverride;

        return Environment == WataEnvironment.Sandbox ? AcquiringSandboxUrl : AcquiringProductionUrl;
    }

    internal Uri ResolveDigitalGoodsBaseUrl()
    {
        if (DigitalGoodsBaseUrlOverride is not null)
            return DigitalGoodsBaseUrlOverride;

        if (Environment == WataEnvironment.Sandbox)
        {
            throw new Errors.WataConfigurationException(
                "Песочница для цифровых товаров не документирована и недоступна. " +
                "Используйте WataEnvironment.Production либо явно задайте DigitalGoodsBaseUrlOverride.");
        }

        return DigitalGoodsProductionUrl;
    }
}
