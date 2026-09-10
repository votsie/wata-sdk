using System.Collections.Concurrent;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Wata.Sdk.Errors;
using Wata.Sdk.Http;

namespace Wata.Sdk.Webhooks;

/// <summary>
/// Проверка подписи и разбор вебхуков WATA.
/// <para>
/// Подпись проверяется по СЫРОМУ телу запроса (байты или строка) — никогда по уже
/// разобранному объекту: пересборка JSON меняет порядок ключей и ломает подпись.
/// В ASP.NET Core это значит, что тело нужно прочитать из Request.Body ДО
/// модель-байндинга (см. README, раздел про приём вебхуков).
/// </para>
/// <para>
/// Алгоритм — SHA512withRSA (RSA PKCS#1 v1.5 + SHA-512), не SHA-256. Публичный ключ
/// кэшируется отдельно на боевое окружение и песочницу, поскольку у них разные ключи.
/// </para>
/// </summary>
public sealed class WebhookVerifier
{
    // Кэш ключа — на инстанс WebhookVerifier (то есть на один WataClient/одно окружение).
    // Если в процессе живут два клиента (например, отдельно на прод и на sandbox),
    // у каждого свой словарь и свой ключ — перепутать окружения невозможно.
    private readonly ConcurrentDictionary<string, string> _keyCache = new();

    private readonly HttpClient _httpClient;
    private readonly Uri _publicKeyEndpoint;
    private readonly string _cacheKey;

    internal WebhookVerifier(HttpClient httpClient, Uri acquiringBaseUrl, WataEnvironment environment)
    {
        _httpClient = httpClient;
        _publicKeyEndpoint = new Uri(acquiringBaseUrl, "/api/h2h/public-key");
        _cacheKey = environment + ":" + acquiringBaseUrl;
    }

    /// <summary>
    /// Проверяет подпись, самостоятельно получая и кэшируя публичный ключ (привязанный
    /// к окружению клиента). Тело — сырые байты, ДО разбора JSON.
    /// </summary>
    public async Task<bool> VerifyAsync(ReadOnlyMemory<byte> rawBody, string signatureBase64, CancellationToken cancellationToken = default)
    {
        var pem = await GetOrFetchPublicKeyAsync(cancellationToken).ConfigureAwait(false);
        return Verify(rawBody.Span, signatureBase64, pem);
    }

    /// <summary>Перегрузка для тела в виде строки. Строка должна быть исходным телом запроса как есть.</summary>
    public Task<bool> VerifyAsync(string rawBody, string signatureBase64, CancellationToken cancellationToken = default) =>
        VerifyAsync(Encoding.UTF8.GetBytes(rawBody), signatureBase64, cancellationToken);

    /// <summary>Проверяет подпись с явно заданным PEM-ключом — без сети и без кэша.</summary>
    public static bool Verify(ReadOnlySpan<byte> rawBody, string signatureBase64, string publicKeyPem)
    {
        byte[] signature;
        try
        {
            signature = Convert.FromBase64String(signatureBase64);
        }
        catch (FormatException ex)
        {
            throw new WataWebhookException("Подпись вебхука не в формате base64.", ex);
        }

        using var rsa = RSA.Create();
        try
        {
            rsa.ImportFromPem(publicKeyPem);
        }
        catch (Exception ex) when (ex is CryptographicException or FormatException)
        {
            throw new WataWebhookException("Не удалось разобрать публичный ключ вебхука (ожидается PEM).", ex);
        }

        return rsa.VerifyData(rawBody, signature, HashAlgorithmName.SHA512, RSASignaturePadding.Pkcs1);
    }

    /// <summary>Проверяет подпись с явно заданным PEM-ключом (перегрузка для строки).</summary>
    public static bool Verify(string rawBody, string signatureBase64, string publicKeyPem) =>
        Verify(Encoding.UTF8.GetBytes(rawBody), signatureBase64, publicKeyPem);

    /// <summary>Разбирает тело вебхука в типизированное событие. Вызывайте после успешной проверки подписи.</summary>
    public WataWebhookEvent Parse(ReadOnlySpan<byte> rawBody) =>
        JsonSerializer.Deserialize<WataWebhookEvent>(rawBody, WataJson.Options)
        ?? throw new WataWebhookException("Не удалось разобрать тело вебхука: пустой результат.");

    /// <summary>Разбирает тело вебхука в типизированное событие (перегрузка для строки).</summary>
    public WataWebhookEvent Parse(string rawBody) =>
        JsonSerializer.Deserialize<WataWebhookEvent>(rawBody, WataJson.Options)
        ?? throw new WataWebhookException("Не удалось разобрать тело вебхука: пустой результат.");

    private async Task<string> GetOrFetchPublicKeyAsync(CancellationToken cancellationToken)
    {
        if (_keyCache.TryGetValue(_cacheKey, out var cached))
            return cached;

        using var request = new HttpRequestMessage(HttpMethod.Get, _publicKeyEndpoint);
        using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);

        if (!response.IsSuccessStatusCode)
        {
            throw new WataWebhookException(
                $"Не удалось получить публичный ключ вебхука по адресу {_publicKeyEndpoint} (HTTP {(int)response.StatusCode}).");
        }

        var content = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
        WataApiPublicKeyResponse? parsed;
        try
        {
            parsed = JsonSerializer.Deserialize<WataApiPublicKeyResponse>(content, WataJson.Options);
        }
        catch (JsonException ex)
        {
            throw new WataWebhookException("Ответ с публичным ключом вебхука пришёл в неожиданном формате.", ex);
        }

        if (parsed is null || string.IsNullOrWhiteSpace(parsed.Value))
            throw new WataWebhookException("Пустой ответ при получении публичного ключа вебхука.");

        _keyCache[_cacheKey] = parsed.Value;
        return parsed.Value;
    }

    private sealed record WataApiPublicKeyResponse(string Value);
}
