namespace Wata.Sdk.Models;

/// <summary>
/// Валюта. Открытый enum: неизвестное значение с сервера сохраняется как есть
/// (см. IWataOpenEnum).
/// </summary>
public readonly struct Currency : IWataOpenEnum, IEquatable<Currency>
{
    public Currency(string value) => Value = value ?? throw new ArgumentNullException(nameof(value));

    public static readonly Currency Rub = new("RUB");
    public static readonly Currency Usd = new("USD");
    public static readonly Currency Eur = new("EUR");

    public static readonly Currency Gbp = new("GBP");

    public string Value { get; }

    public override string ToString() => Value;
    public bool Equals(Currency other) => string.Equals(Value, other.Value, StringComparison.Ordinal);
    public override bool Equals(object? obj) => obj is Currency other && Equals(other);
    public override int GetHashCode() => Value.GetHashCode(StringComparison.Ordinal);
    public static bool operator ==(Currency left, Currency right) => left.Equals(right);
    public static bool operator !=(Currency left, Currency right) => !left.Equals(right);
    public static implicit operator string(Currency value) => value.Value;
    public static explicit operator Currency(string value) => new(value);
}
