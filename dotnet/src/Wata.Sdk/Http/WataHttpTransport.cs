using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using Wata.Sdk.Errors;

namespace Wata.Sdk.Http;

/// <summary>
/// Транспорт одного продукта (эквайринг, Stars, Steam, Top-Up, ваучеры): свой базовый
/// адрес и свой токен, но общий HttpClient (см. WataClient) — чтобы не плодить сокеты
/// и при этом никогда не путать токены между продуктами.
/// </summary>
internal sealed class WataHttpTransport
{
    private readonly HttpClient _httpClient;
    private readonly Uri _baseAddress;
    private readonly string? _token;
    private readonly TimeSpan _timeout;
    private readonly int _maxRetryAttempts;

    public WataHttpTransport(HttpClient httpClient, Uri baseAddress, string? token, TimeSpan timeout, int maxRetryAttempts)
    {
        _httpClient = httpClient;
        _baseAddress = baseAddress;
        _token = token;
        _timeout = timeout;
        _maxRetryAttempts = Math.Max(1, maxRetryAttempts);
    }

    /// <summary>
    /// Выполняет запрос и разбирает JSON-ответ в TResponse.
    /// allowRetry=false для изменяющих запросов (создание ссылки/заказа/платежа/возврата) —
    /// повтор такого запроса может создать вторую операцию, поэтому по умолчанию его не будет.
    /// </summary>
    public async Task<TResponse> SendAsync<TResponse>(
        HttpMethod method,
        string path,
        string? query,
        object? body,
        bool requiresAuth,
        bool allowRetry,
        CancellationToken cancellationToken)
    {
        if (requiresAuth && string.IsNullOrEmpty(_token))
        {
            throw new WataConfigurationException(
                "Внутренняя ошибка конфигурации: запрос " + method + " " + path + " требует токен, но он не был проверен заранее.");
        }

        var maxAttempts = allowRetry ? _maxRetryAttempts : 1;
        Exception? lastNetworkError = null;

        for (var attempt = 1; attempt <= maxAttempts; attempt++)
        {
            using var timeoutCts = new CancellationTokenSource(_timeout);
            using var linkedCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken, timeoutCts.Token);

            HttpResponseMessage response;
            try
            {
                using var request = BuildRequest(method, path, query, body, requiresAuth);
                response = await _httpClient
                    .SendAsync(request, HttpCompletionOption.ResponseHeadersRead, linkedCts.Token)
                    .ConfigureAwait(false);
            }
            catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
            {
                lastNetworkError = new WataNetworkException("Превышен таймаут запроса (" + _timeout + ").", null)
                {
                    RequestPath = path,
                };

                if (attempt >= maxAttempts)
                    throw lastNetworkError;

                await DelayBeforeRetryAsync(attempt, cancellationToken).ConfigureAwait(false);
                continue;
            }
            catch (HttpRequestException ex)
            {
                lastNetworkError = new WataNetworkException("Сетевая ошибка при обращении к WATA API.", ex)
                {
                    RequestPath = path,
                };

                if (attempt >= maxAttempts)
                    throw lastNetworkError;

                await DelayBeforeRetryAsync(attempt, cancellationToken).ConfigureAwait(false);
                continue;
            }

            using (response)
            {
                if (response.IsSuccessStatusCode)
                    return await ReadSuccessAsync<TResponse>(response, cancellationToken).ConfigureAwait(false);

                if ((int)response.StatusCode >= 500 && attempt < maxAttempts)
                {
                    await DelayBeforeRetryAsync(attempt, cancellationToken).ConfigureAwait(false);
                    continue;
                }

                throw await BuildErrorAsync(response, path, cancellationToken).ConfigureAwait(false);
            }
        }

        throw lastNetworkError ?? new WataNetworkException("Не удалось выполнить запрос к WATA API.", null) { RequestPath = path };
    }

    private HttpRequestMessage BuildRequest(HttpMethod method, string path, string? query, object? body, bool requiresAuth)
    {
        var url = new StringBuilder(_baseAddress.ToString().TrimEnd('/'));
        url.Append(path);
        if (!string.IsNullOrEmpty(query))
            url.Append('?').Append(query);

        var request = new HttpRequestMessage(method, url.ToString());

        if (requiresAuth)
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _token);

        if (body is not null)
        {
            var json = JsonSerializer.Serialize(body, body.GetType(), WataJson.Options);
            request.Content = new StringContent(json, Encoding.UTF8, "application/json");
        }

        return request;
    }

    private static async Task<TResponse> ReadSuccessAsync<TResponse>(HttpResponseMessage response, CancellationToken cancellationToken)
    {
        var content = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
        if (string.IsNullOrWhiteSpace(content))
            return default!;

        return JsonSerializer.Deserialize<TResponse>(content, WataJson.Options)!;
    }

    private static async Task<WataException> BuildErrorAsync(HttpResponseMessage response, string path, CancellationToken cancellationToken)
    {
        var status = (int)response.StatusCode;
        var content = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);

        WataApiErrorDetails? details = null;
        if (!string.IsNullOrWhiteSpace(content))
        {
            try
            {
                details = JsonSerializer.Deserialize<WataApiErrorBody>(content, WataJson.Options)?.Error;
            }
            catch (JsonException)
            {
                // Тело не в ожидаемом формате { error: {...} } — используем текст статуса как есть.
            }
        }

        if (status is 401 or 403)
        {
            return new WataAuthenticationException(
                details?.Message ?? "Ошибка аутентификации: токен недействителен, отозван, принадлежит другому терминалу или запрос идёт с несогласованного IP.")
            {
                HttpStatusCode = status,
                RequestPath = path,
                WataErrorCode = details?.Code,
            };
        }

        if (status == 429)
        {
            TimeSpan? retryAfter = null;
            if (response.Headers.RetryAfter is { } ra)
            {
                if (ra.Delta is { } delta)
                    retryAfter = delta;
                else if (ra.Date is { } date)
                    retryAfter = date - DateTimeOffset.UtcNow;
            }

            return new WataRateLimitException(details?.Message ?? "Превышен лимит запросов (429).", retryAfter)
            {
                HttpStatusCode = status,
                RequestPath = path,
                WataErrorCode = details?.Code,
            };
        }

        if (status >= 500)
        {
            return new WataServerException(details?.Message ?? ("Сервер WATA вернул ошибку " + status + "."))
            {
                HttpStatusCode = status,
                RequestPath = path,
                WataErrorCode = details?.Code,
            };
        }

        return new WataApiException(
            details?.Message ?? ("WATA API вернул ошибку " + status + "."),
            details?.Code,
            details?.Details,
            details?.ValidationErrors)
        {
            HttpStatusCode = status,
            RequestPath = path,
            WataErrorCode = details?.Code,
        };
    }

    private static async Task DelayBeforeRetryAsync(int attempt, CancellationToken cancellationToken)
    {
        var baseDelay = TimeSpan.FromMilliseconds(200 * Math.Pow(2, attempt - 1));
        var jitter = TimeSpan.FromMilliseconds(Random.Shared.Next(0, 100));
        await Task.Delay(baseDelay + jitter, cancellationToken).ConfigureAwait(false);
    }
}
