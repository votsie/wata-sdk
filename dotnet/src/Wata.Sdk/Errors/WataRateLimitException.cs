namespace Wata.Sdk.Errors;

/// <summary>429: превышен лимит запросов. SDK не ретраит такие ответы автоматически.</summary>
public sealed class WataRateLimitException : WataException
{
    public WataRateLimitException(string message, TimeSpan? retryAfter)
        : base(message)
    {
        RetryAfter = retryAfter;
    }

    /// <summary>Через сколько можно повторить запрос, если сервер указал это в заголовке Retry-After.</summary>
    public TimeSpan? RetryAfter { get; }
}
