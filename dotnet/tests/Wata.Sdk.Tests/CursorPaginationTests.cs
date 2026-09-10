using System.Net;
using Wata.Sdk;
using Wata.Sdk.Acquiring.Models;
using Xunit;

namespace Wata.Sdk.Tests;

public sealed class CursorPaginationTests
{
    [Fact]
    public async Task SearchTransactionsAsync_WalksTwoPages_CarryingAllThreeCursorFields()
    {
        var page1 = """
        {
          "items": [ { "id": "11111111-1111-1111-1111-111111111111", "amount": 100, "currency": "RUB" } ],
          "hasNextPage": true,
          "nextCursorId": "22222222-2222-2222-2222-222222222222",
          "nextCursorDate": "2026-01-01T00:00:00Z",
          "nextCursorAmount": 100
        }
        """;

        var page2 = """
        {
          "items": [ { "id": "33333333-3333-3333-3333-333333333333", "amount": 200, "currency": "RUB" } ],
          "hasNextPage": false,
          "nextCursorId": null,
          "nextCursorDate": null,
          "nextCursorAmount": null
        }
        """;

        var callCount = 0;
        var handler = FakeHttpMessageHandler.FromSync(req =>
        {
            callCount++;
            var query = req.RequestUri!.Query;

            if (callCount == 1)
            {
                Assert.DoesNotContain("CursorId=", query);
                return FakeHttpMessageHandler.Json(HttpStatusCode.OK, page1);
            }

            Assert.Contains("CursorId=22222222-2222-2222-2222-222222222222", query);
            Assert.Contains("CursorAmount=100", query);
            Assert.Contains("CursorDate=", query);
            return FakeHttpMessageHandler.Json(HttpStatusCode.OK, page2);
        });

        var client = new WataClient(handler.ToHttpClient(), new WataOptions
        {
            AcquiringToken = "token",
            AcquiringBaseUrlOverride = new Uri("https://unit-test.local"),
        });

        var results = new List<Transaction>();
        await foreach (var transaction in client.Acquiring.SearchTransactionsAsync(new SearchTransactionsRequest()))
            results.Add(transaction);

        Assert.Equal(2, callCount);
        Assert.Equal(2, results.Count);
        Assert.Equal(Guid.Parse("11111111-1111-1111-1111-111111111111"), results[0].Id);
        Assert.Equal(Guid.Parse("33333333-3333-3333-3333-333333333333"), results[1].Id);
    }
}
