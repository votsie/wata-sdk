using Wata.Sdk.Models;

namespace Wata.Sdk.Webhooks;

/// <summary>
/// Событие вебхука эквайринга. Обратите внимание: комиссия здесь называется
/// Commission, а в ответе GET /transactions/{id} то же значение называется
/// TotalCommission — это не опечатка SDK, а особенность API WATA.
/// </summary>
public sealed record WataWebhookEvent
{
    public TransactionType TransactionType { get; init; }

    public TransactionKind Kind { get; init; }

    public Guid Id { get; init; }

    public Guid TransactionId { get; init; }

    public Guid? OriginalTransactionId { get; init; }

    public string? TerminalPublicId { get; init; }

    public TransactionStatus TransactionStatus { get; init; }

    public string? ErrorCode { get; init; }

    public string? ErrorDescription { get; init; }

    public string? TerminalName { get; init; }

    public decimal Amount { get; init; }

    public Currency Currency { get; init; }

    public string? OrderId { get; init; }

    public string? OrderDescription { get; init; }

    /// <summary>Комиссия. В ответе GET /transactions/{id} то же самое поле называется TotalCommission.</summary>
    public decimal? Commission { get; init; }

    public DateTimeOffset? PaymentTime { get; init; }

    public string? Email { get; init; }

    public Guid? PaymentLinkId { get; init; }

    public PayerData? PayerData { get; init; }
}

public sealed record PayerData
{
    public string? PayerId { get; init; }
}
