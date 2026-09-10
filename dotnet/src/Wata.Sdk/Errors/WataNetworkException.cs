namespace Wata.Sdk.Errors;

/// <summary>Таймаут запроса или обрыв соединения.</summary>
public sealed class WataNetworkException : WataException
{
    public WataNetworkException(string message, Exception? innerException)
        : base(message, innerException)
    {
    }
}
