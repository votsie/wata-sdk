namespace Wata.Sdk.Models;

public readonly struct DepositOrderStatus : IWataOpenEnum, IEquatable<DepositOrderStatus>
{
    public DepositOrderStatus(string value) => Value = value ?? throw new ArgumentNullException(nameof(value));

    public static readonly DepositOrderStatus Pending = new("Pending");
    public static readonly DepositOrderStatus Success = new("Success");
    public static readonly DepositOrderStatus Fail = new("Fail");

    public string Value { get; }

    public override string ToString() => Value;
    public bool Equals(DepositOrderStatus other) => string.Equals(Value, other.Value, StringComparison.Ordinal);
    public override bool Equals(object? obj) => obj is DepositOrderStatus other && Equals(other);
    public override int GetHashCode() => Value.GetHashCode(StringComparison.Ordinal);
    public static bool operator ==(DepositOrderStatus left, DepositOrderStatus right) => left.Equals(right);
    public static bool operator !=(DepositOrderStatus left, DepositOrderStatus right) => !left.Equals(right);
    public static implicit operator string(DepositOrderStatus value) => value.Value;
    public static explicit operator DepositOrderStatus(string value) => new(value);
}
