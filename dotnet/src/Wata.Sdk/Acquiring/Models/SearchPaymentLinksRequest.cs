using Wata.Sdk.Models;

namespace Wata.Sdk.Acquiring.Models;

public sealed record SearchPaymentLinksRequest(
    string? OrderId = null,
    DateTimeOffset? CreationTimeFrom = null,
    DateTimeOffset? CreationTimeTo = null,
    decimal? AmountFrom = null,
    decimal? AmountTo = null,
    IReadOnlyList<Currency>? Currencies = null,
    IReadOnlyList<PaymentLinkStatus>? Statuses = null,
    Sorting? Sorting = null,
    int? SkipCount = null,
    int? MaxResultCount = null);
