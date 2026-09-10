using Wata.Sdk.Models;

namespace Wata.Sdk.Acquiring.Models;

/// <summary>
/// Оплата картой по криптограмме. Криптограмму (CardCrypto) формирует клиентский
/// скрипт чекаута в браузере плательщика — сервер мерчанта её не собирает и не
/// должен пытаться собрать сам.
/// </summary>
public sealed record CardCryptoPaymentRequest(
    decimal Amount,
    Currency Currency,
    string CardCrypto,
    string Ip,
    Uri ReturnUrl,
    DeviceData DeviceData);

public sealed record CardCryptoPaymentResponse
{
    public Guid TransactionId { get; init; }

    public TransactionStatus Status { get; init; }

    public ThreeDsData? ThreeDsData { get; init; }
}

/// <summary>Данные для редиректа или автосабмита формы 3-D Secure.</summary>
public sealed record ThreeDsData
{
    public Uri? Url { get; init; }

    public string? Method { get; init; }

    public IReadOnlyDictionary<string, string>? Parameters { get; init; }
}
