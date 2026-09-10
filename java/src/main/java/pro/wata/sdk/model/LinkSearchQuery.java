package pro.wata.sdk.model;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Параметры поиска платёжных ссылок ({@code GET /api/h2h/links}). */
public final class LinkSearchQuery {

    private final String orderId;
    private final OffsetDateTime creationTimeFrom;
    private final OffsetDateTime creationTimeTo;
    private final Double amountFrom;
    private final Double amountTo;
    private final List<Currency> currencies;
    private final List<PaymentLinkStatus> statuses;
    private final Sorting sorting;
    private final Integer skipCount;
    private final Integer maxResultCount;

    private LinkSearchQuery(Builder b) {
        this.orderId = b.orderId;
        this.creationTimeFrom = b.creationTimeFrom;
        this.creationTimeTo = b.creationTimeTo;
        this.amountFrom = b.amountFrom;
        this.amountTo = b.amountTo;
        this.currencies = b.currencies;
        this.statuses = b.statuses;
        this.sorting = b.sorting;
        this.skipCount = b.skipCount;
        this.maxResultCount = b.maxResultCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Query-параметры без пустых значений, ключи в PascalCase — как их ожидает API. */
    public Map<String, Object> toQueryParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        putIfPresent(params, "OrderId", orderId);
        putIfPresent(params, "CreationTimeFrom", creationTimeFrom);
        putIfPresent(params, "CreationTimeTo", creationTimeTo);
        putIfPresent(params, "AmountFrom", amountFrom);
        putIfPresent(params, "AmountTo", amountTo);
        putIfPresent(params, "Currencies", currencies);
        putIfPresent(params, "Statuses", statuses);
        putIfPresent(params, "Sorting", sorting == null ? null : sorting.wireValue());
        putIfPresent(params, "SkipCount", skipCount);
        putIfPresent(params, "MaxResultCount", maxResultCount);
        return params;
    }

    private static void putIfPresent(Map<String, Object> params, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        params.put(key, value);
    }

    public static final class Builder {
        private String orderId;
        private OffsetDateTime creationTimeFrom;
        private OffsetDateTime creationTimeTo;
        private Double amountFrom;
        private Double amountTo;
        private List<Currency> currencies;
        private List<PaymentLinkStatus> statuses;
        private Sorting sorting;
        private Integer skipCount;
        /** По умолчанию сервер использует 10, максимум — 1000. */
        private Integer maxResultCount;

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder creationTimeFrom(OffsetDateTime creationTimeFrom) {
            this.creationTimeFrom = creationTimeFrom;
            return this;
        }

        public Builder creationTimeTo(OffsetDateTime creationTimeTo) {
            this.creationTimeTo = creationTimeTo;
            return this;
        }

        public Builder amountFrom(double amountFrom) {
            this.amountFrom = amountFrom;
            return this;
        }

        public Builder amountTo(double amountTo) {
            this.amountTo = amountTo;
            return this;
        }

        public Builder currencies(List<Currency> currencies) {
            this.currencies = currencies;
            return this;
        }

        public Builder statuses(List<PaymentLinkStatus> statuses) {
            this.statuses = statuses;
            return this;
        }

        public Builder sorting(Sorting sorting) {
            this.sorting = sorting;
            return this;
        }

        public Builder skipCount(int skipCount) {
            this.skipCount = skipCount;
            return this;
        }

        public Builder maxResultCount(int maxResultCount) {
            this.maxResultCount = maxResultCount;
            return this;
        }

        public LinkSearchQuery build() {
            return new LinkSearchQuery(this);
        }
    }
}
