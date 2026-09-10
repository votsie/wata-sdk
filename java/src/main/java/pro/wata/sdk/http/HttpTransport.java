package pro.wata.sdk.http;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import pro.wata.sdk.errors.WataApiErrorBody;
import pro.wata.sdk.errors.WataApiException;
import pro.wata.sdk.errors.WataAuthException;
import pro.wata.sdk.errors.WataNetworkException;
import pro.wata.sdk.errors.WataRateLimitException;
import pro.wata.sdk.errors.WataServerException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Транспорт для одного продукта: один {@link HttpClient}, одна базовая
 * ссылка, один токен. Токены между продуктами никогда не переиспользуются —
 * у каждого продукта свой экземпляр {@link HttpTransport}
 * (см. {@link pro.wata.sdk.WataClient}).
 */
public final class HttpTransport {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String bearerToken;
    private final Duration requestTimeout;
    private final RetryPolicy retryPolicy;
    private final ObjectMapper mapper = JsonSupport.mapper();

    public HttpTransport(String baseUrl, String bearerToken, Duration requestTimeout, RetryPolicy retryPolicy) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.bearerToken = bearerToken;
        this.requestTimeout = requestTimeout;
        this.retryPolicy = retryPolicy;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .build();
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public <T> T get(String path, Map<String, Object> query, Class<T> responseType) {
        return get(path, query, mapper.getTypeFactory().constructType(responseType));
    }

    public <T> T get(String path, Map<String, Object> query, JavaType responseType) {
        HttpResponse<String> response = send("GET", path + QueryStrings.encode(query), null, true);
        return readBody(response, path, responseType);
    }

    /** Изменяющий запрос по умолчанию не повторяется — {@code retryable = false}. */
    public <T> T post(String path, Object body, Class<T> responseType, boolean retryable) {
        return post(path, body, mapper.getTypeFactory().constructType(responseType), retryable);
    }

    public <T> T post(String path, Object body, JavaType responseType, boolean retryable) {
        String json = writeBody(body, path);
        HttpResponse<String> response = send("POST", path, json, retryable);
        return readBody(response, path, responseType);
    }

    public void postNoResponseBody(String path, Object body, boolean retryable) {
        String json = writeBody(body, path);
        send("POST", path, json, retryable);
    }

    private String writeBody(Object body, String path) {
        try {
            return mapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new WataNetworkException("Failed to serialize request body for " + path, path, e);
        }
    }

    private <T> T readBody(HttpResponse<String> response, String path, JavaType responseType) {
        if (responseType.hasRawClass(Void.class)) {
            return null;
        }
        try {
            return mapper.readValue(response.body(), responseType);
        } catch (IOException e) {
            throw new WataNetworkException("Failed to parse response body from " + path, path, e);
        }
    }

    private HttpResponse<String> send(String method, String path, String jsonBody, boolean retryable) {
        String url = baseUrl + path;
        int maxAttempts = retryable ? retryPolicy.maxAttempts() : 1;
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> response = executeOnce(method, url, jsonBody);
                int status = response.statusCode();

                if (status >= 200 && status < 300) {
                    return response;
                }
                if (status == 401 || status == 403) {
                    throw new WataAuthException(authErrorMessage(status), status, path);
                }
                if (status == 429) {
                    throw new WataRateLimitException("Rate limit exceeded for " + path, path, parseRetryAfter(response));
                }
                if (status >= 500) {
                    lastFailure = new WataServerException("Server error " + status + " from " + path, status, path);
                    if (attempt < maxAttempts) {
                        sleep(retryPolicy.delayBeforeAttempt(attempt));
                        continue;
                    }
                    throw lastFailure;
                }
                // 4xx (except 401/403/429): typed API error, never retried.
                throw toApiException(response, path, status);
            } catch (IOException | InterruptedException e) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                }
                lastFailure = new WataNetworkException("Network error calling " + path, path, e);
                if (attempt < maxAttempts) {
                    sleep(retryPolicy.delayBeforeAttempt(attempt));
                    continue;
                }
                throw lastFailure;
            }
        }
        // Unreachable in practice: loop always returns or throws.
        throw lastFailure != null ? lastFailure : new WataNetworkException("Request failed for " + path, path, null);
    }

    private HttpResponse<String> executeOnce(String method, String url, String jsonBody) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(requestTimeout)
                .header("Accept", "application/json");
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        HttpRequest.BodyPublisher bodyPublisher = jsonBody == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(jsonBody);
        if (jsonBody != null) {
            builder.header("Content-Type", "application/json");
        }
        builder.method(method, bodyPublisher);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String authErrorMessage(int status) {
        return "Authorization failed (" + status + "): token expired, revoked, belongs to a different terminal, "
                + "or the request IP is not allow-listed";
    }

    private WataApiException toApiException(HttpResponse<String> response, String path, int status) {
        String body = response.body();
        try {
            WataApiErrorBody.Envelope envelope = mapper.readValue(body, WataApiErrorBody.Envelope.class);
            WataApiErrorBody error = envelope.error();
            if (error != null) {
                return new WataApiException(error.message(), status, path, error.code(), error.details(), error.validationErrors());
            }
        } catch (IOException ignored) {
            // Falls through to a generic error below; the raw body is not surfaced to avoid leaking secrets.
        }
        return new WataApiException("Request to " + path + " failed with status " + status, status, path, null, null, null);
    }

    private Duration parseRetryAfter(HttpResponse<String> response) {
        return response.headers().firstValue("Retry-After").map(value -> {
            try {
                return Duration.ofSeconds(Long.parseLong(value.trim()));
            } catch (NumberFormatException e) {
                try {
                    ZonedDateTime target = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
                    Duration diff = Duration.between(ZonedDateTime.now(target.getZone()), target);
                    return diff.isNegative() ? Duration.ZERO : diff;
                } catch (RuntimeException parseFailure) {
                    return null;
                }
            }
        }).orElse(null);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WataNetworkException("Interrupted while waiting to retry", null, e);
        }
    }
}
