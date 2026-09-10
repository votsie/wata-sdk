using Wata.Sdk;
using Wata.Sdk.Acquiring.Models;
using Wata.Sdk.DigitalGoods.Stars;
using Wata.Sdk.Errors;
using Wata.Sdk.Models;
using Xunit;

namespace Wata.Sdk.Tests;

public sealed class ConfigurationTests
{
    [Fact]
    public async Task AcquiringWithoutToken_ThrowsConfigurationError_WithoutNetworkCall()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ => throw new InvalidOperationException("Сеть не должна вызываться."));
        var client = new WataClient(handler.ToHttpClient(), new WataOptions());

        var ex = await Assert.ThrowsAsync<WataConfigurationException>(() =>
            client.Acquiring.CreateLinkAsync(new CreatePaymentLinkRequest(100m, Currency.Rub)));

        Assert.Contains("AcquiringToken", ex.Message);
        Assert.Empty(handler.Requests);
    }

    [Fact]
    public async Task StarsWithSteamTokenOnly_ThrowsConfigurationError_NamingMissingToken()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ => throw new InvalidOperationException("Сеть не должна вызываться."));
        var client = new WataClient(handler.ToHttpClient(), new WataOptions { SteamToken = "steam-token" });

        var ex = await Assert.ThrowsAsync<WataConfigurationException>(() =>
            client.Stars.GetPriceAsync(new StarsPriceRequest(100)));

        Assert.Contains("StarsToken", ex.Message);
        Assert.Empty(handler.Requests);
    }

    [Fact]
    public void SandboxForDigitalGoods_ThrowsConfigurationError()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ => throw new InvalidOperationException("Сеть не должна вызываться."));

        var options = new WataOptions
        {
            Environment = WataEnvironment.Sandbox,
            SteamToken = "steam-token",
        };

        var ex = Record.Exception(() => new WataClient(handler.ToHttpClient(), options));

        Assert.IsType<WataConfigurationException>(ex);
        Assert.Empty(handler.Requests);
    }
}
