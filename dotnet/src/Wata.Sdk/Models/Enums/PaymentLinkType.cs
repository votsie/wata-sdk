namespace Wata.Sdk.Models;

public readonly struct PaymentLinkType : IWataOpenEnum, IEquatable<PaymentLinkType>
{
    public PaymentLinkType(string value) => Value = value ?? throw new ArgumentNullException(nameof(value));

    public static readonly PaymentLinkType OneTime = new("OneTime");
    public static readonly PaymentLinkType ManyTime = new("ManyTime");

    public string Value { get; }

    public override string ToString() => Value;
    public bool Equals(PaymentLinkType other) => string.Equals(Value, other.Value, StringComparison.Ordinal);
    public override bool Equals(object? obj) => obj is PaymentLinkType other && Equals(other);
    public override int GetHashCode() => Value.GetHashCode(StringComparison.Ordinal);
    public static bool operator ==(PaymentLinkType left, PaymentLinkType right) => left.Equals(right);
    public static bool operator !=(PaymentLinkType left, PaymentLinkType right) => !left.Equals(right);
    public static implicit operator string(PaymentLinkType value) => value.Value;
    public static explicit operator PaymentLinkType(string value) => new(value);
}
