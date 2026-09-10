using Wata.Sdk.Models;

namespace Wata.Sdk.DigitalGoods.Steam;

/// <summary>
/// Steam поддерживает два сценария: известна сумма зачисления на аккаунт (NetAmount)
/// или известна сумма платежа (Amount). Комиссия и курс делают эти величины разными —
/// не путайте их друг с другом.
/// </summary>
public sealed record SteamAmountQuoteRequest(string SteamLogin, decimal NetAmount, Currency? Currency = null);

public sealed record SteamByAmountQuoteRequest(string SteamLogin, decimal Amount, Currency? Currency = null);

public sealed record SteamQuote
{
    /// <summary>Сумма, которая зачислится на аккаунт Steam.</summary>
    public decimal NetAmount { get; init; }

    /// <summary>Сумма, которую нужно оплатить (включает комиссию/курсовую разницу).</summary>
    public decimal Amount { get; init; }

    public Currency Currency { get; init; }
}

public sealed record CreateSteamOrderRequest(
    string SteamLogin,
    decimal NetAmount,
    Currency? Currency = null,
    Uri? SuccessRedirectUrl = null,
    Uri? FailRedirectUrl = null,
    string? OrderId = null);

public sealed record CreateSteamOrderByAmountRequest(
    string SteamLogin,
    decimal Amount,
    Currency? Currency = null,
    Uri? SuccessRedirectUrl = null,
    Uri? FailRedirectUrl = null,
    string? OrderId = null);

public sealed record SteamOrder
{
    public string? Id { get; init; }

    public string? OrderId { get; init; }

    public string? SteamLogin { get; init; }

    public decimal NetAmount { get; init; }

    public decimal Amount { get; init; }

    public Currency Currency { get; init; }

    public DigitalGoodsOrderStatus Status { get; init; }

    public Uri? Url { get; init; }

    public DateTimeOffset CreationTime { get; init; }
}

public sealed record SteamDepositPriceRequest(string SteamLogin, decimal Amount);

public sealed record SteamDepositNetAmountRequest(string SteamLogin, decimal NetAmount);

public sealed record CreateSteamDepositOrderRequest(string SteamLogin, decimal NetAmount, string? OrderId = null);

public sealed record CreateSteamDepositOrderByPriceRequest(string SteamLogin, decimal Amount, string? OrderId = null);
