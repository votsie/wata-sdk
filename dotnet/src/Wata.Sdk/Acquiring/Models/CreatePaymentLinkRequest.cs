using Wata.Sdk.Models;

namespace Wata.Sdk.Acquiring.Models;

/// <summary>
/// Запрос на создание платёжной ссылки. Обязательны Amount и Currency, остальное
/// опционально. Ограничения из документации (сумма, срок жизни, число подсказок)
/// SDK не проверяет жёстко на клиенте — их проверяет сервер.
/// </summary>
public sealed record CreatePaymentLinkRequest(
    decimal Amount,
    Currency Currency,
    string? Description = null,
    string? OrderId = null,
    Uri? SuccessRedirectUrl = null,
    Uri? FailRedirectUrl = null,
    DateTimeOffset? ExpirationDateTime = null,
    PaymentLinkType? Type = null,
    bool? IsArbitraryAmountAllowed = null,
    IReadOnlyList<decimal>? ArbitraryAmountPrompts = null,
    string? Email = null,
    string? Phone = null,
    string? Username = null,
    string? UserId = null,
    SubscriptionRequest? Subscription = null);

/// <summary>Параметры подписки для ManyTime-ссылки с периодическим списанием.</summary>
public sealed record SubscriptionRequest(
    int Period,
    SubscriptionInterval Interval,
    int MaxPeriods,
    decimal Amount,
    DateTimeOffset? StartDate = null);
