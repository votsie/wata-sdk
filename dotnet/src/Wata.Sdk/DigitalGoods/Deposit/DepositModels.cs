using Wata.Sdk.Models;

namespace Wata.Sdk.DigitalGoods.Deposit;

/// <summary>Общий статус депозитного заказа — один эндпоинт на все продукты, оплачиваемые с депозита.</summary>
public sealed record DepositOrder
{
    public string? OrderId { get; init; }

    public DepositOrderStatus Status { get; init; }

    public decimal? Amount { get; init; }

    public Currency? Currency { get; init; }

    public DateTimeOffset CreationTime { get; init; }

    public string? ErrorCode { get; init; }

    public string? ErrorDescription { get; init; }
}

public sealed record DepositBalance
{
    public decimal TotalBalance { get; init; }

    public decimal FrozenBalance { get; init; }

    public decimal AvailableBalance { get; init; }
}
