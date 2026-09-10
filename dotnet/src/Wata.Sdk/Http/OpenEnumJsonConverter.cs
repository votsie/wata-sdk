using System.Reflection;
using System.Text.Json;
using System.Text.Json.Serialization;
using Wata.Sdk.Models;

namespace Wata.Sdk.Http;

/// <summary>
/// Фабрика JSON-конвертеров для всех "открытых" enum'ов (см. <see cref="IWataOpenEnum"/>).
/// Регистрируется один раз в <see cref="WataJson"/> и обслуживает все типы вида
/// <c>Currency</c>, <c>TransactionStatus</c> и т.д. без дублирования кода конвертера.
/// </summary>
internal sealed class OpenEnumJsonConverterFactory : JsonConverterFactory
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.IsValueType && typeof(IWataOpenEnum).IsAssignableFrom(typeToConvert);

    public override JsonConverter CreateConverter(Type typeToConvert, JsonSerializerOptions options)
    {
        var converterType = typeof(OpenEnumJsonConverter<>).MakeGenericType(typeToConvert);
        return (JsonConverter)Activator.CreateInstance(converterType)!;
    }
}

internal sealed class OpenEnumJsonConverter<T> : JsonConverter<T>
    where T : struct, IWataOpenEnum
{
    private static readonly ConstructorInfo Constructor =
        typeof(T).GetConstructor(new[] { typeof(string) })
        ?? throw new InvalidOperationException(
            $"Тип {typeof(T)} должен иметь публичный конструктор, принимающий string, чтобы поддерживать неизвестные значения enum.");

    public override T Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        var raw = reader.TokenType switch
        {
            JsonTokenType.String => reader.GetString() ?? string.Empty,
            JsonTokenType.Null => string.Empty,
            _ => throw new JsonException($"Ожидалась строка для {typeof(T).Name}, получено {reader.TokenType}."),
        };

        return (T)Constructor.Invoke(new object[] { raw });
    }

    public override void Write(Utf8JsonWriter writer, T value, JsonSerializerOptions options) =>
        writer.WriteStringValue(value.Value);

    public override T ReadAsPropertyName(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options) =>
        (T)Constructor.Invoke(new object[] { reader.GetString() ?? string.Empty });

    public override void WriteAsPropertyName(Utf8JsonWriter writer, T value, JsonSerializerOptions options) =>
        writer.WritePropertyName(value.Value);
}
