namespace Wata.Sdk.Models;

public readonly struct SubscriptionInterval : IWataOpenEnum, IEquatable<SubscriptionInterval>
{
    public SubscriptionInterval(string value) => Value = value ?? throw new ArgumentNullException(nameof(value));

    public static readonly SubscriptionInterval Test = new("Test");
    public static readonly SubscriptionInterval Week = new("Week");
    public static readonly SubscriptionInterval Month = new("Month");

    public string Value { get; }

    public override string ToString() => Value;
    public bool Equals(SubscriptionInterval other) => string.Equals(Value, other.Value, StringComparison.Ordinal);
    public override bool Equals(object? obj) => obj is SubscriptionInterval other && Equals(other);
    public override int GetHashCode() => Value.GetHashCode(StringComparison.Ordinal);
    public static bool operator ==(SubscriptionInterval left, SubscriptionInterval right) => left.Equals(right);
    public static bool operator !=(SubscriptionInterval left, SubscriptionInterval right) => !left.Equals(right);
    public static implicit operator string(SubscriptionInterval value) => value.Value;
    public static explicit operator SubscriptionInterval(string value) => new(value);
}
