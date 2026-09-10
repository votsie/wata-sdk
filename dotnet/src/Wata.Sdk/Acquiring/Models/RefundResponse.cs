using Wata.Sdk.Models;

namespace Wata.Sdk.Acquiring.Models;

public sealed record RefundResponse
{
    public Guid TransactionId { get; init; }

    public Guid OriginalTransactionId { get; init; }

    public TransactionStatus TransactionStatus { get; init; }

    /// <summary>Всегда TransactionKind.Refund.</summary>
    public TransactionKind Kind { get; init; }

    public string? ErrorCode { get; init; }

    public string? ErrorDescription { get; init; }
}
