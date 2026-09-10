package pro.wata.sdk;

import pro.wata.sdk.errors.WataConfigException;

/**
 * Окружение эквайринга. У боевого контура и песочницы разные базовые адреса
 * <b>и разные публичные ключи вебхуков</b> — кэш ключа привязан к окружению
 * (см. {@link pro.wata.sdk.webhooks.WebhookVerifier}).
 *
 * <p>Для цифровых товаров отдельного адреса песочницы не существует: запрос
 * {@link #digitalGoodsBaseUrl()} на {@link #SANDBOX} бросает
 * {@link WataConfigException}, а не молча уходит в боевой контур.
 */
public enum WataEnvironment {

    PRODUCTION("https://api.wata.pro", "https://dg-api.wata.pro"),
    SANDBOX("https://api-sandbox.wata.pro", null);

    private final String acquiringBaseUrl;
    private final String digitalGoodsBaseUrl;

    WataEnvironment(String acquiringBaseUrl, String digitalGoodsBaseUrl) {
        this.acquiringBaseUrl = acquiringBaseUrl;
        this.digitalGoodsBaseUrl = digitalGoodsBaseUrl;
    }

    public String acquiringBaseUrl() {
        return acquiringBaseUrl;
    }

    public String digitalGoodsBaseUrl() {
        if (digitalGoodsBaseUrl == null) {
            throw new WataConfigException(
                    "Digital goods have no sandbox environment documented by WATA; "
                            + "use WataEnvironment.PRODUCTION for Steam/Stars/Top-Up/vouchers, "
                            + "there is no silent fallback to production from here");
        }
        return digitalGoodsBaseUrl;
    }
}
