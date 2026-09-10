namespace Wata.Sdk.Models;

public readonly struct TransactionType : IWataOpenEnum, IEquatable<TransactionType>
{
    public TransactionType(string value) => Value = value ?? throw new ArgumentNullException(nameof(value));

    public static readonly TransactionType CardCrypto = new("CardCrypto");
    public static readonly TransactionType Sbp = new("SBP");
    public static readonly TransactionType TPay = new("TPay");

    public string Value { get; }

    public override string ToString() => Value;
    public bool Equals(TransactionType other) => string.Equals(Value, other.Value, StringComparison.Ordinal);
    public override bool Equals(object? obj) => obj is TransactionType other && Equals(other);
    public override int GetHashCode() => Value.GetHashCode(StringComparison.Ordinal);
    public static bool operator ==(TransactionType left, TransactionType right) => left.Equals(right);
    public static bool operator !=(TransactionType left, TransactionType right) => !left.Equals(right);
    public static implicit operator string(TransactionType value) => value.Value;
    public static explicit operator TransactionType(string value) => new(value);
}
