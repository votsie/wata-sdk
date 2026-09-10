package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Оплата картой по криптограмме ({@code POST /api/h2h/payments/card-crypto}).
 *
 * <p>Криптограмму ({@code cardCrypto}) формирует клиентский скрипт чекаута в
 * браузере плательщика — сервер мерчанта её не собирает и не должен видеть
 * данные карты в открытом виде.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class CardCryptoPaymentRequest {

    private final double amount;
    private final Currency currency;
    private final String cardCrypto;
    private final String ip;
    private final String returnUrl;
    private final DeviceData deviceData;
    private final String orderId;
    private final String description;

    private CardCryptoPaymentRequest(Builder b) {
        this.amount = b.amount;
        this.currency = b.currency;
        this.cardCrypto = b.cardCrypto;
        this.ip = b.ip;
        this.returnUrl = b.returnUrl;
        this.deviceData = b.deviceData;
        this.orderId = b.orderId;
        this.description = b.description;
    }

    public double getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getCardCrypto() {
        return cardCrypto;
    }

    public String getIp() {
        return ip;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public DeviceData getDeviceData() {
        return deviceData;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getDescription() {
        return description;
    }

    public static Builder builder(double amount, Currency currency, String cardCrypto, String ip,
                                   String returnUrl, DeviceData deviceData) {
        return new Builder(amount, currency, cardCrypto, ip, returnUrl, deviceData);
    }

    public static final class Builder {
        private final double amount;
        private final Currency currency;
        private final String cardCrypto;
        private final String ip;
        private final String returnUrl;
        private final DeviceData deviceData;
        private String orderId;
        private String description;

        public Builder(double amount, Currency currency, String cardCrypto, String ip,
                       String returnUrl, DeviceData deviceData) {
            this.amount = amount;
            this.currency = currency;
            this.cardCrypto = cardCrypto;
            this.ip = ip;
            this.returnUrl = returnUrl;
            this.deviceData = deviceData;
        }

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public CardCryptoPaymentRequest build() {
            return new CardCryptoPaymentRequest(this);
        }
    }
}
