namespace Wata.Sdk.Errors;

/// <summary>Базовое исключение всех ошибок WATA SDK.</summary>
public abstract class WataException : Exception
{
    protected WataException(string message, Exception? innerException = null)
        : base(message, innerException)
    {
    }

    /// <summary>HTTP-статус ответа, если ошибка связана с сетевым вызовом.</summary>
    public int? HttpStatusCode { get; init; }

    /// <summary>Путь запроса, в контексте которого произошла ошибка.</summary>
    public string? RequestPath { get; init; }

    /// <summary>
    /// Код ошибки WATA (PL_*, TRA_*, ORD_*, STM_*, STR_*, TPP_*, VCR_*), если сервер его вернул.
    /// </summary>
    public string? WataErrorCode { get; init; }
}
