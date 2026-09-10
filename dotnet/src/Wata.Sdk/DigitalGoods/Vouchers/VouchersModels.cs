using Wata.Sdk.Models;

namespace Wata.Sdk.DigitalGoods.Vouchers;

public sealed record VoucherProduct
{
    public string? Id { get; init; }

    public string? Name { get; init; }

    public decimal Amount { get; init; }

    public Currency Currency { get; init; }
}

public sealed record CreateVoucherOrderRequest(
    string ProductId,
    int Quantity,
    Currency? Currency = null,
    Uri? SuccessRedirectUrl = null,
    Uri? FailRedirectUrl = null,
    string? OrderId = null);

/// <summary>
/// Заказ ваучеров. Коды возвращаются прямо в статусе заказа (отдельного эндпоинта
/// выдачи нет) и могут появиться с задержкой до 10 минут после оплаты — если
/// Codes ещё null при статусе Success, запросите заказ повторно чуть позже.
/// </summary>
public sealed record VoucherOrder
{
    public string? Id { get; init; }

    public string? OrderId { get; init; }

    public string? ProductId { get; init; }

    public int Quantity { get; init; }

    public decimal Amount { get; init; }

    public Currency Currency { get; init; }

    public DigitalGoodsOrderStatus Status { get; init; }

    public Uri? Url { get; init; }

    public DateTimeOffset CreationTime { get; init; }

    /// <summary>Коды ваучеров. Появляются после оплаты, возможна задержка до 10 минут.</summary>
    public IReadOnlyList<string>? Codes { get; init; }
}

public sealed record CreateVoucherDepositOrderRequest(string ProductId, int Quantity, string? OrderId = null);
