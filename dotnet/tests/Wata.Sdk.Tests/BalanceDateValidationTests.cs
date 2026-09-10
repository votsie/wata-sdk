using Wata.Sdk;
using Wata.Sdk.Errors;
using Xunit;

namespace Wata.Sdk.Tests;

public sealed class BalanceDateValidationTests
{
    [Fact]
    public async Task GetBalanceAsync_DateOlderThanYesterday_ThrowsConfigurationError_WithoutNetworkCall()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ => throw new InvalidOperationException("Сеть не должна вызываться."));
        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
        });

        var tooOld = DateOnly.FromDateTime(DateTime.UtcNow).AddDays(-2);

        var ex = await Assert.ThrowsAsync<WataConfigurationException>(() => client.Acquiring.GetBalanceAsync(tooOld));

        Assert.Empty(handler.Requests);
        Assert.Contains("UTC", ex.Message);
    }

    [Fact]
    public async Task GetBalanceAsync_FutureDate_ThrowsConfigurationError()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ => throw new InvalidOperationException("Сеть не должна вызываться."));
        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
        });

        var tomorrow = DateOnly.FromDateTime(DateTime.UtcNow).AddDays(1);

        await Assert.ThrowsAsync<WataConfigurationException>(() => client.Acquiring.GetBalanceAsync(tomorrow));

        Assert.Empty(handler.Requests);
    }

    [Fact]
    public async Task GetBalanceAsync_Today_IsAllowed()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ =>
            FakeHttpMessageHandler.Json(System.Net.HttpStatusCode.OK, """{ "terminalPublicId": "t1", "date": "2026-01-01", "balance": 100, "currency": "RUB" }"""));

        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
        });

        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var balance = await client.Acquiring.GetBalanceAsync(today);

        Assert.Single(handler.Requests);
        Assert.Equal(100m, balance.Amount);
    }

    [Fact]
    public async Task GetBalanceAsync_Yesterday_IsAllowed()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ =>
            FakeHttpMessageHandler.Json(System.Net.HttpStatusCode.OK, """{ "terminalPublicId": "t1", "date": "2026-01-01", "balance": 50, "currency": "RUB" }"""));

        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
        });

        var yesterday = DateOnly.FromDateTime(DateTime.UtcNow).AddDays(-1);
        var balance = await client.Acquiring.GetBalanceAsync(yesterday);

        Assert.Single(handler.Requests);
        Assert.Equal(50m, balance.Amount);
    }
}
