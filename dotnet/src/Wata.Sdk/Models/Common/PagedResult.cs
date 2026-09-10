namespace Wata.Sdk.Models;

/// <summary>Постраничный результат поиска (используется для ссылок).</summary>
public sealed record PagedResult<T>(IReadOnlyList<T> Items, int TotalCount);
