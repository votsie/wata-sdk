namespace Wata.Sdk.Acquiring.Models;

/// <summary>Оплата СБП. Валюты нет — только рубли.</summary>
public sealed record SbpPaymentRequest(
    decimal Amount,
    string Ip,
    Uri ReturnUrl,
    DeviceData DeviceData);

public sealed record SbpPaymentResponse
{
    public Guid TransactionId { get; init; }

    public Uri? SbpLink { get; init; }
}
