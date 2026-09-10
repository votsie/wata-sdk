using System.Text.Json.Serialization;
using Wata.Sdk.Models;

namespace Wata.Sdk.Acquiring.Models;

public sealed record Balance
{
    public string? TerminalPublicId { get; init; }

    public DateOnly Date { get; init; }

    /// <summary>Сумма баланса терминала на указанную дату. Имя JSON-поля — "balance".</summary>
    [JsonPropertyName("balance")]
    public decimal Amount { get; init; }

    public Currency Currency { get; init; }
}
