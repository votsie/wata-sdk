using System.Net;
using Wata.Sdk;
using Wata.Sdk.Acquiring.Models;
using Wata.Sdk.Errors;
using Wata.Sdk.Models;
using Xunit;

namespace Wata.Sdk.Tests;

public sealed class RetryBehaviorTests
{
    [Fact]
    public async Task CreateLinkAsync_ServerError_DoesNotRetry_BecauseItIsMutating()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ => FakeHttpMessageHandler.Json(HttpStatusCode.InternalServerError, "{}"));
        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
            MaxRetryAttempts = 3,
        });

        await Assert.ThrowsAsync<WataServerException>(() =>
            client.Acquiring.CreateLinkAsync(new CreatePaymentLinkRequest(100m, Currency.Rub)));

        Assert.Single(handler.Requests); // изменяющий запрос — ровно одна попытка, без ретраев
    }

    [Fact]
    public async Task RefundAsync_ServerError_DoesNotRetry()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ => FakeHttpMessageHandler.Json(HttpStatusCode.ServiceUnavailable, "{}"));
        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
        });

        await Assert.ThrowsAsync<WataServerException>(() =>
            client.Acquiring.RefundAsync(new RefundRequest(Guid.NewGuid(), 10m)));

        Assert.Single(handler.Requests);
    }

    [Fact]
    public async Task GetLinkAsync_ServerError_RetriesUpToConfiguredAttempts()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ => FakeHttpMessageHandler.Json(HttpStatusCode.InternalServerError, "{}"));
        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
            MaxRetryAttempts = 3,
        });

        await Assert.ThrowsAsync<WataServerException>(() => client.Acquiring.GetLinkAsync(Guid.NewGuid()));

        Assert.Equal(3, handler.Requests.Count); // GET идемпотентен — ретраится до MaxRetryAttempts
    }

    [Fact]
    public async Task GetLinkAsync_SucceedsAfterTransientServerError()
    {
        var attempt = 0;
        var handler = FakeHttpMessageHandler.FromSync(_ =>
        {
            attempt++;
            if (attempt < 2)
                return FakeHttpMessageHandler.Json(HttpStatusCode.InternalServerError, "{}");

            return FakeHttpMessageHandler.Json(HttpStatusCode.OK, """{ "id": "11111111-1111-1111-1111-111111111111", "amount": 1, "currency": "RUB" }""");
        });

        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
        });

        var link = await client.Acquiring.GetLinkAsync(Guid.NewGuid());

        Assert.Equal(2, handler.Requests.Count);
        Assert.Equal(Guid.Parse("11111111-1111-1111-1111-111111111111"), link.Id);
    }
}
