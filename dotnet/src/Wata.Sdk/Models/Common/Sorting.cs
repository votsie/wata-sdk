namespace Wata.Sdk.Models;

/// <summary>Поле сортировки для поиска ссылок и транзакций.</summary>
public enum SortField
{
    OrderId,
    CreationTime,
    Amount,
}

public enum SortDirection
{
    Ascending,
    Descending,
}

/// <summary>
/// Значение параметра Sorting. Сериализуется в вид "orderId" или "orderId desc",
/// как того требует API.
/// </summary>
public readonly record struct Sorting(SortField Field, SortDirection Direction = SortDirection.Ascending)
{
    public override string ToString()
    {
        var field = Field switch
        {
            SortField.OrderId => "orderId",
            SortField.CreationTime => "creationTime",
            SortField.Amount => "amount",
            _ => throw new ArgumentOutOfRangeException(nameof(Field), Field, null),
        };

        return Direction == SortDirection.Descending ? field + " desc" : field;
    }
}
