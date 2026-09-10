using System.Text.Json;

namespace Wata.Sdk.Errors;

/// <summary>
/// 4xx с телом { error: { code, message, details, validationErrors } }.
/// Содержит код ошибки WATA, сообщение и HTTP-статус.
/// </summary>
public sealed class WataApiException : WataException
{
    public WataApiException(string message, string? code, string? details, JsonElement? validationErrors)
        : base(message)
    {
        Code = code;
        Details = details;
        ValidationErrors = validationErrors;
    }

    /// <summary>Код ошибки WATA, например PL_NOT_FOUND, TRA_ALREADY_REFUNDED и т.п.</summary>
    public string? Code { get; }

    /// <summary>Дополнительные детали ошибки от сервера, если есть.</summary>
    public string? Details { get; }

    /// <summary>
    /// Сырое содержимое поля validationErrors — формат не зафиксирован спецификацией,
    /// поэтому оно сохраняется как JsonElement, чтобы не терять данные при неожиданной форме.
    /// </summary>
    public JsonElement? ValidationErrors { get; }
}
