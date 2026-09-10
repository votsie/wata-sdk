package pro.wata.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import pro.wata.sdk.acquiring.AcquiringClient;
import pro.wata.sdk.digitalgoods.stars.StarsClient;
import pro.wata.sdk.digitalgoods.steam.SteamClient;
import pro.wata.sdk.digitalgoods.topup.TopupClient;
import pro.wata.sdk.digitalgoods.vouchers.VouchersClient;
import pro.wata.sdk.errors.WataConfigException;
import pro.wata.sdk.http.HttpTransport;
import pro.wata.sdk.http.JsonSupport;
import pro.wata.sdk.http.RetryPolicy;
import pro.wata.sdk.webhooks.WebhookVerifier;

import java.time.Duration;

/**
 * Точка входа в SDK. Токен WATA выпускается на терминал, а не на аккаунт, и
 * Stars/Steam всегда живут на отдельных терминалах — поэтому клиент строится
 * из набора токенов по продуктам, и обращение к продукту без токена бросает
 * {@link WataConfigException} до какого-либо сетевого вызова (см. п.1 SPEC.md).
 *
 * <pre>{@code
 * WataClient client = WataClient.builder()
 *         .acquiringToken(System.getenv("WATA_ACQUIRING_TOKEN"))
 *         .starsToken(System.getenv("WATA_STARS_TOKEN"))
 *         .build();
 *
 * PaymentLink link = client.acquiring().links().create(
 *         CreateLinkRequest.builder(1500.0, Currency.RUB).description("Order #1").build());
 * }</pre>
 *
 * <p>Если у мерчанта один терминал совмещает несколько продуктов, он может
 * осознанно передать один и тот же токен в несколько полей builder'а — SDK
 * никогда не переиспользует токены между продуктами сам.
 *
 * <p>Каждый продуктовый клиент ({@link #acquiring()}, {@link #steam()}, ...)
 * строится лениво, при первом обращении, — так и отсутствие нужного токена, и
 * запрос песочницы для цифровых товаров (которой не существует, см.
 * {@link WataEnvironment#digitalGoodsBaseUrl()}) одинаково всплывают на самом
 * обращении к продукту, а не заранее в {@code build()} и не посреди сетевого
 * вызова.
 */
public final class WataClient {

    private final WataEnvironment environment;
    private final Duration timeout;
    private final RetryPolicy retryPolicy;
    private final ObjectMapper mapper = JsonSupport.mapper();

    private final String acquiringToken;
    private final String starsToken;
    private final String steamToken;
    private final String topupToken;
    private final String vouchersToken;

    private volatile AcquiringClient acquiringClient;
    private volatile StarsClient starsClient;
    private volatile SteamClient steamClient;
    private volatile TopupClient topupClient;
    private volatile VouchersClient vouchersClient;
    private volatile WebhookVerifier webhookVerifier;

    private WataClient(Builder builder) {
        this.environment = builder.environment;
        this.timeout = builder.timeout;
        this.retryPolicy = builder.retryPolicy;
        this.acquiringToken = builder.acquiringToken;
        this.starsToken = builder.starsToken;
        this.steamToken = builder.steamToken;
        this.topupToken = builder.topupToken;
        this.vouchersToken = builder.vouchersToken;
    }

    public static Builder builder() {
        return new Builder();
    }

    public WataEnvironment environment() {
        return environment;
    }

    /** Эквайринг (H2H). Требует {@code acquiringToken}. */
    public synchronized AcquiringClient acquiring() {
        requireToken(acquiringToken, "acquiring");
        if (acquiringClient == null) {
            HttpTransport transport = new HttpTransport(
                    environment.acquiringBaseUrl() + "/api/h2h", acquiringToken, timeout, retryPolicy);
            acquiringClient = new AcquiringClient(transport, mapper);
        }
        return acquiringClient;
    }

    /** Steam. Требует {@code steamToken} — терминал Steam всегда отдельный от Stars. */
    public synchronized SteamClient steam() {
        requireToken(steamToken, "steam");
        if (steamClient == null) {
            steamClient = new SteamClient(digitalGoodsTransport(steamToken));
        }
        return steamClient;
    }

    /** Telegram Stars. Требует {@code starsToken} — терминал Stars всегда отдельный от Steam. */
    public synchronized StarsClient stars() {
        requireToken(starsToken, "stars");
        if (starsClient == null) {
            starsClient = new StarsClient(digitalGoodsTransport(starsToken));
        }
        return starsClient;
    }

    /** Top-Up. Требует {@code topupToken}. */
    public synchronized TopupClient topup() {
        requireToken(topupToken, "topup");
        if (topupClient == null) {
            topupClient = new TopupClient(digitalGoodsTransport(topupToken));
        }
        return topupClient;
    }

    /** Ваучеры. Требует {@code vouchersToken}. */
    public synchronized VouchersClient vouchers() {
        requireToken(vouchersToken, "vouchers");
        if (vouchersClient == null) {
            vouchersClient = new VouchersClient(digitalGoodsTransport(vouchersToken));
        }
        return vouchersClient;
    }

    /** Проверка и разбор вебхуков. Не требует токена ни одного продукта. */
    public synchronized WebhookVerifier webhooks() {
        if (webhookVerifier == null) {
            webhookVerifier = WebhookVerifier.forEnvironment(environment, timeout);
        }
        return webhookVerifier;
    }

    private HttpTransport digitalGoodsTransport(String token) {
        return new HttpTransport(environment.digitalGoodsBaseUrl() + "/api", token, timeout, retryPolicy);
    }

    private void requireToken(String token, String productName) {
        if (token == null) {
            throw new WataConfigException(
                    "The '" + productName + "' token is not configured; pass it via WataClient.builder()."
                            + productName + "Token(...) before calling client." + productName + "()");
        }
    }

    public static final class Builder {
        private WataEnvironment environment = WataEnvironment.PRODUCTION;
        private Duration timeout = Duration.ofSeconds(60);
        private RetryPolicy retryPolicy = RetryPolicy.DEFAULT;
        private String acquiringToken;
        private String starsToken;
        private String steamToken;
        private String topupToken;
        private String vouchersToken;

        public Builder environment(WataEnvironment environment) {
            this.environment = environment;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder acquiringToken(String acquiringToken) {
            this.acquiringToken = acquiringToken;
            return this;
        }

        /** Терминал Stars всегда отдельный от Steam — не передавайте сюда токен Steam. */
        public Builder starsToken(String starsToken) {
            this.starsToken = starsToken;
            return this;
        }

        /** Терминал Steam всегда отдельный от Stars — не передавайте сюда токен Stars. */
        public Builder steamToken(String steamToken) {
            this.steamToken = steamToken;
            return this;
        }

        public Builder topupToken(String topupToken) {
            this.topupToken = topupToken;
            return this;
        }

        public Builder vouchersToken(String vouchersToken) {
            this.vouchersToken = vouchersToken;
            return this;
        }

        public WataClient build() {
            return new WataClient(this);
        }
    }
}
