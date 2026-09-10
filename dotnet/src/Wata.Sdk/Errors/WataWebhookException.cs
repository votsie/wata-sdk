namespace Wata.Sdk.Errors;

/// <summary>Подпись вебхука не сошлась, ключ не удалось получить или разобрать.</summary>
public sealed class WataWebhookException : WataException
{
    public WataWebhookException(string message, Exception? innerException = null)
        : base(message, innerException)
    {
    }
}
