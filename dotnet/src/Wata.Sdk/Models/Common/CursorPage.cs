namespace Wata.Sdk.Models;

/// <summary>
/// Одна страница курсорной пагинации транзакций. Для сквозного обхода всех страниц
/// используйте IAsyncEnumerable, возвращаемый методами поиска транзакций — он сам
/// переносит CursorId, CursorAmount и CursorDate между запросами.
/// </summary>
public sealed record CursorPage<T>(
    IReadOnlyList<T> Items,
    bool HasNextPage,
    string? NextCursorId,
    DateTimeOffset? NextCursorDate,
    decimal? NextCursorAmount);
