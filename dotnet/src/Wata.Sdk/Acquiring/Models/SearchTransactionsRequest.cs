using Wata.Sdk.Models;

namespace Wata.Sdk.Acquiring.Models;

/// <summary>
/// Фильтр поиска транзакций. Курсорные поля (CursorId/CursorAmount/CursorDate) сюда
/// не входят — ими управляет сам SDK при обходе страниц, вручную их передавать не нужно.
/// </summary>
public sealed record SearchTransactionsRequest(
    string? OrderId = null,
    DateTimeOffset? CreationTimeFrom = null,
    DateTimeOffset? CreationTimeTo = null,
    decimal? AmountFrom = null,
    decimal? AmountTo = null,
    IReadOnlyList<Currency>? Currencies = null,
    IReadOnlyList<Guid>? PaymentLinkIds = null,
    IReadOnlyList<TransactionStatus>? Statuses = null,
    Sorting? Sorting = null,
    int? MaxResultCount = null);
