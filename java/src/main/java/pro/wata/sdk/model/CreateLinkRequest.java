package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Запрос на создание платёжной ссылки ({@code POST /api/h2h/links}).
 *
 * <p>Обязательные поля — {@code amount} и {@code currency}, задаются через
 * {@link Builder#Builder(double, Currency)}. Остальные поля опциональны.
 *
 * <p>Документация по ограничениям (SDK их не проверяет жёстко, только
 * называет): сумма — от 10 RUB / 1 USD / 1 EUR до 999999.99; срок жизни
 * ссылки — от 10 минут до 30 дней, по умолчанию 3 дня; подсказок произвольной
 * суммы не больше 6, каждая не меньше {@code amount}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class CreateLinkRequest {

    private final double amount;
    private final Currency currency;
    private final String description;
    private final String orderId;
    private final String successRedirectUrl;
    private final String failRedirectUrl;
    private final OffsetDateTime expirationDateTime;
    private final PaymentLinkType type;
    private final Boolean isArbitraryAmountAllowed;
    private final List<Double> arbitraryAmountPrompts;
    private final String email;
    private final String phone;
    private final String username;
    private final String userId;
    private final Subscription subscription;

    private CreateLinkRequest(Builder b) {
        this.amount = b.amount;
        this.currency = b.currency;
        this.description = b.description;
        this.orderId = b.orderId;
        this.successRedirectUrl = b.successRedirectUrl;
        this.failRedirectUrl = b.failRedirectUrl;
        this.expirationDateTime = b.expirationDateTime;
        this.type = b.type;
        this.isArbitraryAmountAllowed = b.isArbitraryAmountAllowed;
        this.arbitraryAmountPrompts = b.arbitraryAmountPrompts == null ? null : List.copyOf(b.arbitraryAmountPrompts);
        this.email = b.email;
        this.phone = b.phone;
        this.username = b.username;
        this.userId = b.userId;
        this.subscription = b.subscription;
    }

    public double getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSuccessRedirectUrl() {
        return successRedirectUrl;
    }

    public String getFailRedirectUrl() {
        return failRedirectUrl;
    }

    public OffsetDateTime getExpirationDateTime() {
        return expirationDateTime;
    }

    public PaymentLinkType getType() {
        return type;
    }

    public Boolean getIsArbitraryAmountAllowed() {
        return isArbitraryAmountAllowed;
    }

    public List<Double> getArbitraryAmountPrompts() {
        return arbitraryAmountPrompts;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getUsername() {
        return username;
    }

    public String getUserId() {
        return userId;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public static Builder builder(double amount, Currency currency) {
        return new Builder(amount, currency);
    }

    public static final class Builder {
        private final double amount;
        private final Currency currency;
        private String description;
        private String orderId;
        private String successRedirectUrl;
        private String failRedirectUrl;
        private OffsetDateTime expirationDateTime;
        private PaymentLinkType type;
        private Boolean isArbitraryAmountAllowed;
        private List<Double> arbitraryAmountPrompts;
        private String email;
        private String phone;
        private String username;
        private String userId;
        private Subscription subscription;

        public Builder(double amount, Currency currency) {
            this.amount = amount;
            this.currency = currency;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder successRedirectUrl(String successRedirectUrl) {
            this.successRedirectUrl = successRedirectUrl;
            return this;
        }

        public Builder failRedirectUrl(String failRedirectUrl) {
            this.failRedirectUrl = failRedirectUrl;
            return this;
        }

        public Builder expirationDateTime(OffsetDateTime expirationDateTime) {
            this.expirationDateTime = expirationDateTime;
            return this;
        }

        /** По умолчанию {@link PaymentLinkType#ONE_TIME}, если не задано. */
        public Builder type(PaymentLinkType type) {
            this.type = type;
            return this;
        }

        public Builder isArbitraryAmountAllowed(boolean isArbitraryAmountAllowed) {
            this.isArbitraryAmountAllowed = isArbitraryAmountAllowed;
            return this;
        }

        /** Не больше 6 подсказок, каждая не меньше {@code amount} (проверяется сервером). */
        public Builder arbitraryAmountPrompts(List<Double> arbitraryAmountPrompts) {
            this.arbitraryAmountPrompts = arbitraryAmountPrompts == null ? null : new ArrayList<>(arbitraryAmountPrompts);
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder subscription(Subscription subscription) {
            this.subscription = subscription;
            return this;
        }

        public CreateLinkRequest build() {
            return new CreateLinkRequest(this);
        }
    }
}
