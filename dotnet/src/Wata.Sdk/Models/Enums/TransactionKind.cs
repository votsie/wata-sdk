namespace Wata.Sdk.Models;

public readonly struct TransactionKind : IWataOpenEnum, IEquatable<TransactionKind>
{
    public TransactionKind(string value) => Value = value ?? throw new ArgumentNullException(nameof(value));

    public static readonly TransactionKind Payment = new("Payment");
    public static readonly TransactionKind Refund = new("Refund");

    public string Value { get; }

    public override string ToString() => Value;
    public bool Equals(TransactionKind other) => string.Equals(Value, other.Value, StringComparison.Ordinal);
    public override bool Equals(object? obj) => obj is TransactionKind other && Equals(other);
    public override int GetHashCode() => Value.GetHashCode(StringComparison.Ordinal);
    public static bool operator ==(TransactionKind left, TransactionKind right) => left.Equals(right);
    public static bool operator !=(TransactionKind left, TransactionKind right) => !left.Equals(right);
    public static implicit operator string(TransactionKind value) => value.Value;
    public static explicit operator TransactionKind(string value) => new(value);
}
