namespace Wata.Sdk.Models;

public readonly struct DigitalGoodsOrderStatus : IWataOpenEnum, IEquatable<DigitalGoodsOrderStatus>
{
    public DigitalGoodsOrderStatus(string value) => Value = value ?? throw new ArgumentNullException(nameof(value));

    public static readonly DigitalGoodsOrderStatus Pending = new("Pending");
    public static readonly DigitalGoodsOrderStatus Paid = new("Paid");
    public static readonly DigitalGoodsOrderStatus Success = new("Success");
    public static readonly DigitalGoodsOrderStatus Fail = new("Fail");

    public string Value { get; }

    public override string ToString() => Value;
    public bool Equals(DigitalGoodsOrderStatus other) => string.Equals(Value, other.Value, StringComparison.Ordinal);
    public override bool Equals(object? obj) => obj is DigitalGoodsOrderStatus other && Equals(other);
    public override int GetHashCode() => Value.GetHashCode(StringComparison.Ordinal);
    public static bool operator ==(DigitalGoodsOrderStatus left, DigitalGoodsOrderStatus right) => left.Equals(right);
    public static bool operator !=(DigitalGoodsOrderStatus left, DigitalGoodsOrderStatus right) => !left.Equals(right);
    public static implicit operator string(DigitalGoodsOrderStatus value) => value.Value;
    public static explicit operator DigitalGoodsOrderStatus(string value) => new(value);
}
