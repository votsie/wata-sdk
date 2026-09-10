package pro.wata.sdk.model;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Параметры поиска транзакций ({@code GET /api/h2h/v2/transactions}).
 *
 * <p>Курсорные поля ({@code CursorId}, {@code CursorAmount}, {@code CursorDate})
 * не задаются напрямую — их переносит
 * {@link pro.wata.sdk.acquiring.TransactionsClient#searchAll(TransactionSearchQuery)}
 * между страницами. Ручной перенос курсора — источник ошибок (пропуск
 * {@code CursorDate} ломает вторую страницу), поэтому конструктор курсора
 * оставлен package-private.
 */
public final class TransactionSearchQuery {

    private final String orderId;
    private final OffsetDateTime creationTimeFrom;
    private final OffsetDateTime creationTimeTo;
    private final Double amountFrom;
    private final Double amountTo;
    private final List<Currency> currencies;
    private final List<String> paymentLinkIds;
    private final List<TransactionStatus> statuses;
    private final Sorting sorting;
    private final Integer maxResultCount;

    private final String cursorId;
    private final Double cursorAmount;
    private final OffsetDateTime cursorDate;

    private TransactionSearchQuery(Builder b, String cursorId, Double cursorAmount, OffsetDateTime cursorDate) {
        this.orderId = b.orderId;
        this.creationTimeFrom = b.creationTimeFrom;
        this.creationTimeTo = b.creationTimeTo;
        this.amountFrom = b.amountFrom;
        this.amountTo = b.amountTo;
        this.currencies = b.currencies;
        this.paymentLinkIds = b.paymentLinkIds;
        this.statuses = b.statuses;
        this.sorting = b.sorting;
        this.maxResultCount = b.maxResultCount;
        this.cursorId = cursorId;
        this.cursorAmount = cursorAmount;
        this.cursorDate = cursorDate;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Копия запроса с курсором следующей страницы. */
    public TransactionSearchQuery withCursor(String cursorId, Double cursorAmount, OffsetDateTime cursorDate) {
        Builder b = new Builder();
        b.orderId = this.orderId;
        b.creationTimeFrom = this.creationTimeFrom;
        b.creationTimeTo = this.creationTimeTo;
        b.amountFrom = this.amountFrom;
        b.amountTo = this.amountTo;
        b.currencies = this.currencies;
        b.paymentLinkIds = this.paymentLinkIds;
        b.statuses = this.statuses;
        b.sorting = this.sorting;
        b.maxResultCount = this.maxResultCount;
        return new TransactionSearchQuery(b, cursorId, cursorAmount, cursorDate);
    }

    public Map<String, Object> toQueryParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        putIfPresent(params, "OrderId", orderId);
        putIfPresent(params, "CreationTimeFrom", creationTimeFrom);
        putIfPresent(params, "CreationTimeTo", creationTimeTo);
        putIfPresent(params, "AmountFrom", amountFrom);
        putIfPresent(params, "AmountTo", amountTo);
        putIfPresent(params, "Currencies", currencies);
        putIfPresent(params, "PaymentLinkIds", paymentLinkIds);
        putIfPresent(params, "Statuses", statuses);
        putIfPresent(params, "Sorting", sorting == null ? null : sorting.wireValue());
        putIfPresent(params, "MaxResultCount", maxResultCount);
        putIfPresent(params, "CursorId", cursorId);
        putIfPresent(params, "CursorAmount", cursorAmount);
        putIfPresent(params, "CursorDate", cursorDate);
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
        private List<String> paymentLinkIds;
        private List<TransactionStatus> statuses;
        private Sorting sorting;
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

        public Builder paymentLinkIds(List<String> paymentLinkIds) {
            this.paymentLinkIds = paymentLinkIds;
            return this;
        }

        public Builder statuses(List<TransactionStatus> statuses) {
            this.statuses = statuses;
            return this;
        }

        public Builder sorting(Sorting sorting) {
            this.sorting = sorting;
            return this;
        }

        public Builder maxResultCount(int maxResultCount) {
            this.maxResultCount = maxResultCount;
            return this;
        }

        public TransactionSearchQuery build() {
            return new TransactionSearchQuery(this, null, null, null);
        }
    }
}
