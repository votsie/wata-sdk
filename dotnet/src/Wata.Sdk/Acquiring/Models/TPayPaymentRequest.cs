namespace Wata.Sdk.Acquiring.Models;

/// <summary>Оплата T-Pay: те же поля, что у СБП, без firstName/lastName.</summary>
public sealed record TPayPaymentRequest(
    decimal Amount,
    string Ip,
    Uri ReturnUrl,
    DeviceData DeviceData);

public sealed record TPayPaymentResponse
{
    public Guid TransactionId { get; init; }

    public Uri? TPayLink { get; init; }
}
