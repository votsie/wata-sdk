package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

/**
 * Параметры подписки для многоразовой платёжной ссылки.
 *
 * @param period     номер периода
 * @param interval   {@code Test}, {@code Week} или {@code Month}
 * @param maxPeriods максимальное число периодов
 * @param amount     сумма списания за период
 * @param startDate  дата начала подписки, опционально
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Subscription(
        Integer period,
        SubscriptionInterval interval,
        Integer maxPeriods,
        Double amount,
        OffsetDateTime startDate
) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Integer period;
        private SubscriptionInterval interval;
        private Integer maxPeriods;
        private Double amount;
        private OffsetDateTime startDate;

        public Builder period(int period) {
            this.period = period;
            return this;
        }

        public Builder interval(SubscriptionInterval interval) {
            this.interval = interval;
            return this;
        }

        public Builder maxPeriods(int maxPeriods) {
            this.maxPeriods = maxPeriods;
            return this;
        }

        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder startDate(OffsetDateTime startDate) {
            this.startDate = startDate;
            return this;
        }

        public Subscription build() {
            return new Subscription(period, interval, maxPeriods, amount, startDate);
        }
    }
}
