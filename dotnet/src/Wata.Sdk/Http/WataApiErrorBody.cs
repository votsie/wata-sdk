using System.Text.Json;
using System.Text.Json.Serialization;

namespace Wata.Sdk.Http;

/// <summary>Форма тела ошибки WATA: { "error": { "code", "message", "details", "validationErrors" } }.</summary>
internal sealed record WataApiErrorBody
{
    [JsonPropertyName("error")]
    public WataApiErrorDetails? Error { get; init; }
}

internal sealed record WataApiErrorDetails
{
    public string? Code { get; init; }

    public string? Message { get; init; }

    public string? Details { get; init; }

    public JsonElement? ValidationErrors { get; init; }
}
