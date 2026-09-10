using System.Net;
using System.Security.Cryptography;
using System.Text;
using Wata.Sdk;
using Wata.Sdk.Webhooks;
using Xunit;

namespace Wata.Sdk.Tests;

public sealed class WebhookVerificationTests
{
    private const string RawBody = "{\"transactionId\":\"11111111-1111-1111-1111-111111111111\",\"transactionStatus\":\"Paid\"}";

    [Fact]
    public void Verify_WithExplicitKey_ValidSignature_ReturnsTrue()
    {
        using var rsa = RSA.Create(2048);
        var pem = rsa.ExportSubjectPublicKeyInfoPem();
        var signature = rsa.SignData(Encoding.UTF8.GetBytes(RawBody), HashAlgorithmName.SHA512, RSASignaturePadding.Pkcs1);
        var signatureBase64 = Convert.ToBase64String(signature);

        var result = WebhookVerifier.Verify(RawBody, signatureBase64, pem);

        Assert.True(result);
    }

    [Fact]
    public void Verify_WithExplicitKey_ForgedSignature_ReturnsFalse()
    {
        using var rsa = RSA.Create(2048);
        var pem = rsa.ExportSubjectPublicKeyInfoPem();

        using var attackerRsa = RSA.Create(2048);
        var forgedSignature = attackerRsa.SignData(Encoding.UTF8.GetBytes(RawBody), HashAlgorithmName.SHA512, RSASignaturePadding.Pkcs1);
        var forgedSignatureBase64 = Convert.ToBase64String(forgedSignature);

        var result = WebhookVerifier.Verify(RawBody, forgedSignatureBase64, pem);

        Assert.False(result);
    }

    [Fact]
    public void Verify_WithExplicitKey_TamperedBody_ReturnsFalse()
    {
        using var rsa = RSA.Create(2048);
        var pem = rsa.ExportSubjectPublicKeyInfoPem();
        var signature = rsa.SignData(Encoding.UTF8.GetBytes(RawBody), HashAlgorithmName.SHA512, RSASignaturePadding.Pkcs1);
        var signatureBase64 = Convert.ToBase64String(signature);

        var tamperedBody = RawBody.Replace("Paid", "Declined");

        var result = WebhookVerifier.Verify(tamperedBody, signatureBase64, pem);

        Assert.False(result);
    }

    [Fact]
    public async Task VerifyAsync_FetchesAndCachesPublicKey_PerEnvironment()
    {
        using var rsa = RSA.Create(2048);
        var pem = rsa.ExportSubjectPublicKeyInfoPem();
        var signature = rsa.SignData(Encoding.UTF8.GetBytes(RawBody), HashAlgorithmName.SHA512, RSASignaturePadding.Pkcs1);
        var signatureBase64 = Convert.ToBase64String(signature);

        var publicKeyRequests = 0;
        var handler = FakeHttpMessageHandler.FromSync(req =>
        {
            Assert.Equal("/api/h2h/public-key", req.RequestUri!.AbsolutePath);
            Assert.Null(req.Headers.Authorization);
            publicKeyRequests++;
            return FakeHttpMessageHandler.Json(HttpStatusCode.OK, $"{{\"value\":{System.Text.Json.JsonSerializer.Serialize(pem)}}}");
        });

        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "acquiring-token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
        });

        var first = await client.Webhooks.VerifyAsync(RawBody, signatureBase64);
        var second = await client.Webhooks.VerifyAsync(RawBody, signatureBase64);

        Assert.True(first);
        Assert.True(second);
        Assert.Equal(1, publicKeyRequests); // ключ должен быть закэширован после первого запроса
    }

    [Fact]
    public void Parse_ValidBody_ReturnsTypedEvent()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ => throw new InvalidOperationException("Не должно вызываться."));
        var client = new WataClient(handler.ToHttpClient(), new WataOptions { AcquiringToken = "token" });

        var evt = client.Webhooks.Parse(RawBody);

        Assert.Equal(Guid.Parse("11111111-1111-1111-1111-111111111111"), evt.TransactionId);
        Assert.Equal("Paid", evt.TransactionStatus.Value);
    }

    [Fact]
    public void Parse_UnknownEnumValue_DoesNotThrow_AndKeepsRawString()
    {
        var handler = FakeHttpMessageHandler.FromSync(_ => throw new InvalidOperationException("Не должно вызываться."));
        var client = new WataClient(handler.ToHttpClient(), new WataOptions { AcquiringToken = "token" });

        const string bodyWithNewStatus = "{\"transactionId\":\"11111111-1111-1111-1111-111111111111\",\"transactionStatus\":\"SomeBrandNewStatus\"}";

        var evt = client.Webhooks.Parse(bodyWithNewStatus);

        Assert.Equal("SomeBrandNewStatus", evt.TransactionStatus.Value);
    }
}
