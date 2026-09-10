namespace Wata.Sdk.Errors;

/// <summary>5xx, оставшийся после исчерпания повторов.</summary>
public sealed class WataServerException : WataException
{
    public WataServerException(string message)
        : base(message)
    {
    }
}
