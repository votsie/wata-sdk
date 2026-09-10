using System.Net;
using Wata.Sdk;
using Wata.Sdk.Errors;
using Xunit;

namespace Wata.Sdk.Tests;

public sealed class ApiErrorParsingTests
{
    [Fact]
    public async Task GetLinkAsync_ApiError_ParsesWataErrorCodeAndMessage()
    {
        const string body = """
        {
          "error": {
            "code": "PL_NOT_FOUND",
            "message": "Ссылка не найдена",
            "details": "no such link",
            "validationErrors": null
          }
        }
        """;

        var handler = FakeHttpMessageHandler.FromSync(_ => FakeHttpMessageHandler.Json(HttpStatusCode.NotFound, body));
        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
        });

        var ex = await Assert.ThrowsAsync<WataApiException>(() => client.Acquiring.GetLinkAsync(Guid.NewGuid()));

        Assert.Equal("PL_NOT_FOUND", ex.Code);
        Assert.Equal("Ссылка не найдена", ex.Message);
        Assert.Equal(404, ex.HttpStatusCode);
    }

    [Fact]
    public async Task GetLinkAsync_401_ThrowsAuthenticationException()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ => FakeHttpMessageHandler.Json(HttpStatusCode.Unauthorized, "{}"));
        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
        });

        await Assert.ThrowsAsync<WataAuthenticationException>(() => client.Acquiring.GetLinkAsync(Guid.NewGuid()));
    }

    [Fact]
    public async Task GetLinkAsync_429_ThrowsRateLimitException_WithRetryAfter()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ =>
        {
            var response = FakeHttpMessageHandler.Json(HttpStatusCode.TooManyRequests, "{}");
            response.Headers.Add("Retry-After", "30");
            return response;
        });

        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
        });

        var ex = await Assert.ThrowsAsync<WataRateLimitException>(() => client.Acquiring.GetLinkAsync(Guid.NewGuid()));

        Assert.Equal(TimeSpan.FromSeconds(30), ex.RetryAfter);
        Assert.Single(handler.Requests); // 429 не должен ретраиться автоматически
    }
}
