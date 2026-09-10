using Wata.Sdk.Models;

namespace Wata.Sdk.Acquiring.Models;

public sealed record Transaction
{
    public Guid Id { get; init; }

    public TransactionType Type { get; init; }

    public TransactionKind Kind { get; init; }

    public TransactionStatus Status { get; init; }

    public decimal Amount { get; init; }

    public Currency Currency { get; init; }

    public string? OrderId { get; init; }

    public string? OrderDescription { get; init; }

    public Guid? PaymentLinkId { get; init; }

    public string? TerminalPublicId { get; init; }

    public string? TerminalName { get; init; }

    /// <summary>Комиссия. В вебхуке то же значение называется commission (см. README).</summary>
    public decimal? TotalCommission { get; init; }

    public DateTimeOffset CreationTime { get; init; }

    public DateTimeOffset? PaymentTime { get; init; }

    public string? ErrorCode { get; init; }

    public string? ErrorDescription { get; init; }
}
