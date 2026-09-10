namespace Wata.Sdk.Acquiring.Models;

/// <summary>
/// Запрос на возврат. Валюта берётся из исходной транзакции, поля "причина" не
/// существует — это осознанное ограничение API, а не упущение SDK.
/// </summary>
public sealed record RefundRequest(Guid OriginalTransactionId, decimal Amount);
