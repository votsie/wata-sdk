package pro.wata.sdk.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import pro.wata.sdk.WataEnvironment;
import pro.wata.sdk.errors.WataNetworkException;
import pro.wata.sdk.errors.WataWebhookException;
import pro.wata.sdk.http.JsonSupport;
import pro.wata.sdk.model.WebhookEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;

/**
 * Проверка и разбор вебхуков эквайринга.
 *
 * <p>Алгоритм подписи — <b>SHA512withRSA</b> (RSA PKCS#1 v1.5 + SHA-512), не
 * SHA-256. Ключ публикуется на {@code GET /api/h2h/public-key} без заголовка
 * авторизации, в поле {@code value}, в формате PEM. У боевого контура и
 * песочницы разные ключи, поэтому один экземпляр {@code WebhookVerifier}
 * привязан к одному {@link WataEnvironment}.
 *
 * <p><b>Подпись всегда проверяется по сырому телу запроса</b> — байтам или
 * строке, как их прислал сервер, до разбора JSON. Пересборка JSON из
 * распарсенного объекта меняет порядок ключей и ломает подпись, поэтому
 * методы этого класса намеренно не принимают разобранный объект.
 */
public final class WebhookVerifier {

    private static final String SIGNATURE_ALGORITHM = "SHA512withRSA";

    private final HttpClient httpClient;
    private final ObjectMapper mapper = JsonSupport.mapper();
    private final String publicKeyUrl;
    private final Duration timeout;

    // Package-private (not private): pro.wata.sdk.webhooks tests point this at a
    // mock server instead of a real WATA host via this constructor.
    WebhookVerifier(String acquiringBaseUrl, Duration timeout) {
        this.publicKeyUrl = acquiringBaseUrl + "/api/h2h/public-key";
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public static WebhookVerifier forEnvironment(WataEnvironment environment) {
        return forEnvironment(environment, Duration.ofSeconds(60));
    }

    public static WebhookVerifier forEnvironment(WataEnvironment environment, Duration timeout) {
        return new WebhookVerifier(environment.acquiringBaseUrl(), timeout);
    }

    /**
     * Проверяет подпись по сырому телу, подтягивая и кэшируя публичный ключ
     * окружения (кэш общий на все верификаторы того же окружения, см.
     * {@link PublicKeyCache}).
     */
    public boolean verify(byte[] rawBody, String signatureBase64) {
        String pem = PublicKeyCache.get(publicKeyUrl, this::fetchPublicKeyPem);
        return verify(rawBody, signatureBase64, pem);
    }

    public boolean verify(String rawBody, String signatureBase64) {
        return verify(rawBody.getBytes(StandardCharsets.UTF_8), signatureBase64);
    }

    /** Проверяет подпись заданным PEM-ключом, без обращения к сети. */
    public boolean verify(byte[] rawBody, String signatureBase64, String publicKeyPem) {
        try {
            PublicKey publicKey = parsePublicKey(publicKeyPem);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(rawBody);
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(signatureBytes);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new WataWebhookException("Failed to verify webhook signature", e);
        }
    }

    public boolean verify(String rawBody, String signatureBase64, String publicKeyPem) {
        return verify(rawBody.getBytes(StandardCharsets.UTF_8), signatureBase64, publicKeyPem);
    }

    /** Разбирает тело в типизированное событие. Подпись при этом не проверяется — вызовите {@code verify} отдельно. */
    public WebhookEvent parse(byte[] rawBody) {
        try {
            return mapper.readValue(rawBody, WebhookEvent.class);
        } catch (IOException e) {
            throw new WataWebhookException("Failed to parse webhook payload", e);
        }
    }

    public WebhookEvent parse(String rawBody) {
        return parse(rawBody.getBytes(StandardCharsets.UTF_8));
    }

    /** Сбрасывает закэшированный ключ этого окружения, например после ротации ключа на стороне WATA. */
    public void invalidateCachedKey() {
        PublicKeyCache.invalidate(publicKeyUrl);
    }

    private String fetchPublicKeyPem() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(publicKeyUrl))
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new WataWebhookException(
                        "Failed to fetch webhook public key: HTTP " + response.statusCode() + " from " + publicKeyUrl);
            }
            PublicKeyResponse body = mapper.readValue(response.body(), PublicKeyResponse.class);
            if (body.value() == null || body.value().isBlank()) {
                throw new WataWebhookException("Webhook public key response had no 'value' field");
            }
            return body.value();
        } catch (IOException e) {
            throw new WataNetworkException("Network error fetching webhook public key", publicKeyUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WataNetworkException("Interrupted fetching webhook public key", publicKeyUrl, e);
        }
    }

    private PublicKey parsePublicKey(String pem) throws GeneralSecurityException {
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
}
