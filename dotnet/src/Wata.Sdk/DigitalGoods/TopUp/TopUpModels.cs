using Wata.Sdk.Models;

namespace Wata.Sdk.DigitalGoods.TopUp;

public sealed record TopUpProduct
{
    public string? Id { get; init; }

    public string? Name { get; init; }

    public decimal Amount { get; init; }

    public Currency Currency { get; init; }
}

public sealed record CreateTopUpOrderRequest(
    string ProductId,
    string AccountId,
    Currency? Currency = null,
    Uri? SuccessRedirectUrl = null,
    Uri? FailRedirectUrl = null,
    string? OrderId = null);

public sealed record TopUpOrder
{
    public string? Id { get; init; }

    public string? OrderId { get; init; }

    public string? ProductId { get; init; }

    public decimal Amount { get; init; }

    public Currency Currency { get; init; }

    public DigitalGoodsOrderStatus Status { get; init; }

    public Uri? Url { get; init; }

    public DateTimeOffset CreationTime { get; init; }
}

public sealed record CreateTopUpDepositOrderRequest(string ProductId, string AccountId, string? OrderId = null);
