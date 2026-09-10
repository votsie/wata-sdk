namespace Wata.Sdk.Models;

public readonly struct TransactionStatus : IWataOpenEnum, IEquatable<TransactionStatus>
{
    public TransactionStatus(string value) => Value = value ?? throw new ArgumentNullException(nameof(value));

    public static readonly TransactionStatus Created = new("Created");
    public static readonly TransactionStatus Pending = new("Pending");
    public static readonly TransactionStatus Paid = new("Paid");
    public static readonly TransactionStatus Declined = new("Declined");

    public string Value { get; }

    public override string ToString() => Value;
    public bool Equals(TransactionStatus other) => string.Equals(Value, other.Value, StringComparison.Ordinal);
    public override bool Equals(object? obj) => obj is TransactionStatus other && Equals(other);
    public override int GetHashCode() => Value.GetHashCode(StringComparison.Ordinal);
    public static bool operator ==(TransactionStatus left, TransactionStatus right) => left.Equals(right);
    public static bool operator !=(TransactionStatus left, TransactionStatus right) => !left.Equals(right);
    public static implicit operator string(TransactionStatus value) => value.Value;
    public static explicit operator TransactionStatus(string value) => new(value);
}
