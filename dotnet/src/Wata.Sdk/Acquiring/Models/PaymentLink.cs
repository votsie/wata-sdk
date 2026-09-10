using Wata.Sdk.Models;

namespace Wata.Sdk.Acquiring.Models;

/// <summary>Платёжная ссылка (ответ на создание/поиск/получение по идентификатору).</summary>
public sealed record PaymentLink
{
    public Guid Id { get; init; }

    public decimal Amount { get; init; }

    public Currency Currency { get; init; }

    public PaymentLinkStatus Status { get; init; }

    public Uri? Url { get; init; }

    public string? TerminalName { get; init; }

    public string? TerminalPublicId { get; init; }

    public DateTimeOffset CreationTime { get; init; }

    public PaymentLinkType Type { get; init; }

    public string? OrderId { get; init; }

    public DateTimeOffset? ExpirationDateTime { get; init; }

    public bool IsArbitraryAmountAllowed { get; init; }

    /// <summary>Подсказки произвольной суммы, если IsArbitraryAmountAllowed включён.</summary>
    public IReadOnlyList<decimal>? ArbitraryAmounts { get; init; }
}
