using System.Text.Json;
using System.Text.Json.Serialization;

namespace Wata.Sdk.Http;

/// <summary>Единая конфигурация System.Text.Json, используемая во всём SDK.</summary>
internal static class WataJson
{
    public static readonly JsonSerializerOptions Options = Create();

    private static JsonSerializerOptions Create()
    {
        var options = new JsonSerializerOptions
        {
            PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
            NumberHandling = JsonNumberHandling.AllowReadingFromString,
            PropertyNameCaseInsensitive = true,
        };

        options.Converters.Add(new OpenEnumJsonConverterFactory());
        return options;
    }
}
