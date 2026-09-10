using System.Globalization;
using Wata.Sdk.Models;

namespace Wata.Sdk.Http;

/// <summary>
/// Небольшой построитель query-строки: пустые и null-значения не добавляются
/// (требование раздела 3 спецификации), массивы сериализуются как повторяющиеся
/// параметры (Currencies=RUB&amp;Currencies=USD).
/// </summary>
internal sealed class QueryStringBuilder
{
    private readonly List<string> _parts = new();

    public QueryStringBuilder Add(string key, string? value)
    {
        if (!string.IsNullOrEmpty(value))
            _parts.Add(Uri.EscapeDataString(key) + "=" + Uri.EscapeDataString(value));

        return this;
    }

    public QueryStringBuilder Add(string key, bool? value)
    {
        if (value is { } b)
            Add(key, b ? "true" : "false");

        return this;
    }

    public QueryStringBuilder Add(string key, int? value)
    {
        if (value is { } i)
            Add(key, i.ToString(CultureInfo.InvariantCulture));

        return this;
    }

    public QueryStringBuilder Add(string key, decimal? value)
    {
        if (value is { } d)
            Add(key, d.ToString(CultureInfo.InvariantCulture));

        return this;
    }

    public QueryStringBuilder Add(string key, DateTimeOffset? value)
    {
        if (value is { } dto)
            Add(key, dto.ToString("O", CultureInfo.InvariantCulture));

        return this;
    }

    public QueryStringBuilder Add(string key, DateOnly? value)
    {
        if (value is { } d)
            Add(key, d.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture));

        return this;
    }

    public QueryStringBuilder Add<TEnum>(string key, TEnum? value)
        where TEnum : struct, IWataOpenEnum
    {
        if (value is { } e)
            Add(key, e.Value);

        return this;
    }

    public QueryStringBuilder Add(string key, Sorting? value)
    {
        if (value is { } s)
            Add(key, s.ToString());

        return this;
    }

    public QueryStringBuilder AddMany(string key, IEnumerable<string>? values)
    {
        if (values is null)
            return this;

        foreach (var v in values)
            Add(key, v);

        return this;
    }

    public QueryStringBuilder AddMany<TEnum>(string key, IEnumerable<TEnum>? values)
        where TEnum : struct, IWataOpenEnum
    {
        if (values is null)
            return this;

        foreach (var v in values)
            Add(key, v.Value);

        return this;
    }

    public QueryStringBuilder AddMany(string key, IEnumerable<Guid>? values)
    {
        if (values is null)
            return this;

        foreach (var v in values)
            Add(key, v.ToString());

        return this;
    }

    public bool IsEmpty => _parts.Count == 0;

    public override string ToString() => string.Join("&", _parts);
}
