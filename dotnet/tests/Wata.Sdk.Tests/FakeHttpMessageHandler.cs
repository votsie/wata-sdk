using System.Net;

namespace Wata.Sdk.Tests;

/// <summary>
/// Мок HTTP-транспорта: подставляется вместо реальной сети через кастомный
/// HttpMessageHandler, обёрнутый в HttpClient. Каждый вызов SendAsync описывается
/// делегатом-обработчиком, который получает HttpRequestMessage и решает, что вернуть.
/// Для синхронных обработчиков используйте статический фабричный метод FromSync —
/// два конструктора, принимающих похожие сигнатуры делегатов, были бы неоднозначны
/// для компилятора при передаче лямбд.
/// </summary>
public sealed class FakeHttpMessageHandler : HttpMessageHandler
{
    private readonly Func<HttpRequestMessage, Task<HttpResponseMessage>> _handler;

    public FakeHttpMessageHandler(Func<HttpRequestMessage, Task<HttpResponseMessage>> handler) => _handler = handler;

    public static FakeHttpMessageHandler FromSync(Func<HttpRequestMessage, HttpResponseMessage> handler) =>
        new(req => Task.FromResult(handler(req)));

    public List<HttpRequestMessage> Requests { get; } = new();

    protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        Requests.Add(request);
        return await _handler(request).ConfigureAwait(false);
    }

    public static HttpResponseMessage Json(HttpStatusCode status, string body) => new(status)
    {
        Content = new StringContent(body, System.Text.Encoding.UTF8, "application/json"),
    };

    public HttpClient ToHttpClient() => new(this);
}
