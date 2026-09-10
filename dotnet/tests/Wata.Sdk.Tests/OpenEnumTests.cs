using System.Text.Json;
using Wata.Sdk.Http;
using Wata.Sdk.Models;
using Xunit;

namespace Wata.Sdk.Tests;

public sealed class OpenEnumTests
{
    [Fact]
    public void KnownValue_DeserializesToStaticMember()
    {
        var currency = JsonSerializer.Deserialize<Currency>("\"RUB\"", WataJson.Options);
        Assert.Equal(Currency.Rub, currency);
    }

    [Fact]
    public void UnknownValue_DoesNotThrow_AndPreservesRawString()
    {
        var status = JsonSerializer.Deserialize<TransactionStatus>("\"FutureStatus\"", WataJson.Options);
        Assert.Equal("FutureStatus", status.Value);
        Assert.NotEqual(TransactionStatus.Paid, status);
    }

    [Fact]
    public void RoundTrip_SerializesBackToOriginalString()
    {
        var currency = new Currency("GBP");
        var json = JsonSerializer.Serialize(currency, WataJson.Options);
        Assert.Equal("\"GBP\"", json);
    }
}
