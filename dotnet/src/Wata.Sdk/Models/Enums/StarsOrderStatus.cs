namespace Wata.Sdk.Models;

public readonly struct StarsOrderStatus : IWataOpenEnum, IEquatable<StarsOrderStatus>
{
    public StarsOrderStatus(string value) => Value = value ?? throw new ArgumentNullException(nameof(value));

    public static readonly StarsOrderStatus Pending = new("Pending");
    public static readonly StarsOrderStatus Review = new("Review");
    public static readonly StarsOrderStatus Paid = new("Paid");
    public static readonly StarsOrderStatus Refunded = new("Refunded");
    public static readonly StarsOrderStatus Success = new("Success");
    public static readonly StarsOrderStatus Fail = new("Fail");

    public string Value { get; }

    public override string ToString() => Value;
    public bool Equals(StarsOrderStatus other) => string.Equals(Value, other.Value, StringComparison.Ordinal);
    public override bool Equals(object? obj) => obj is StarsOrderStatus other && Equals(other);
    public override int GetHashCode() => Value.GetHashCode(StringComparison.Ordinal);
    public static bool operator ==(StarsOrderStatus left, StarsOrderStatus right) => left.Equals(right);
    public static bool operator !=(StarsOrderStatus left, StarsOrderStatus right) => !left.Equals(right);
    public static implicit operator string(StarsOrderStatus value) => value.Value;
    public static explicit operator StarsOrderStatus(string value) => new(value);
}
