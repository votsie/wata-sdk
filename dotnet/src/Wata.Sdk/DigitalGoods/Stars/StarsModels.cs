using Wata.Sdk.Models;

namespace Wata.Sdk.DigitalGoods.Stars;

public sealed record StarsPriceRequest(int StarsCount, Currency? Currency = null);

public sealed record StarsPrice
{
    public int StarsCount { get; init; }

    public decimal Amount { get; init; }

    public Currency Currency { get; init; }

    /// <summary>Минимальное количество звёзд, доступное к покупке (по спецификации — 50).</summary>
    public int MinStarsCount { get; init; }

    /// <summary>Максимальное количество звёзд за один заказ (по спецификации — 50000).</summary>
    public int MaxStarsCount { get; init; }
}

/// <summary>Количество звёзд: 50-50000. Заказы дороже порога попадают в статус Review и НЕ выполняются без подтверждения.</summary>
public sealed record CreateStarsOrderRequest(
    int StarsCount,
    string TelegramUsername,
    Currency? Currency = null,
    Uri? SuccessRedirectUrl = null,
    Uri? FailRedirectUrl = null,
    string? OrderId = null);

public sealed record StarsOrder
{
    public string? Id { get; init; }

    public string? OrderId { get; init; }

    public int StarsCount { get; init; }

    public string? TelegramUsername { get; init; }

    public decimal Amount { get; init; }

    public Currency Currency { get; init; }

    /// <summary>
    /// Pending -&gt; Review -&gt; Paid | Refunded -&gt; Success | Fail. Заказ в статусе Review
    /// ожидает явного подтверждения через ConfirmOrderAsync — выдачи не будет, пока это не сделано.
    /// </summary>
    public StarsOrderStatus Status { get; init; }

    public Uri? Url { get; init; }

    public DateTimeOffset CreationTime { get; init; }
}
