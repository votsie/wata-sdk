namespace Wata.Sdk.Errors;

/// <summary>
/// 401/403: токен истёк, отозван, принадлежит другому терминалу либо запрос идёт
/// с несогласованного IP.
/// </summary>
public sealed class WataAuthenticationException : WataException
{
    public WataAuthenticationException(string message)
        : base(message)
    {
    }
}
