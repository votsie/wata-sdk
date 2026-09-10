namespace Wata.Sdk.Models;

public readonly struct PaymentLinkStatus : IWataOpenEnum, IEquatable<PaymentLinkStatus>
{
    public PaymentLinkStatus(string value) => Value = value ?? throw new ArgumentNullException(nameof(value));

    public static readonly PaymentLinkStatus Opened = new("Opened");
    public static readonly PaymentLinkStatus Closed = new("Closed");

    public string Value { get; }

    public override string ToString() => Value;
    public bool Equals(PaymentLinkStatus other) => string.Equals(Value, other.Value, StringComparison.Ordinal);
    public override bool Equals(object? obj) => obj is PaymentLinkStatus other && Equals(other);
    public override int GetHashCode() => Value.GetHashCode(StringComparison.Ordinal);
    public static bool operator ==(PaymentLinkStatus left, PaymentLinkStatus right) => left.Equals(right);
    public static bool operator !=(PaymentLinkStatus left, PaymentLinkStatus right) => !left.Equals(right);
    public static implicit operator string(PaymentLinkStatus value) => value.Value;
    public static explicit operator PaymentLinkStatus(string value) => new(value);
}
