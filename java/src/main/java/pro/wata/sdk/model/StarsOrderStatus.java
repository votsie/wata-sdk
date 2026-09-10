package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Objects;

/**
 * Статус заказа Telegram Stars:
 * {@code Pending} &rarr; {@code Review} &rarr; {@code Paid} | {@code Refunded}
 * &rarr; {@code Success} | {@code Fail}.
 *
 * <p>Заказы дороже порога попадают в {@code Review} и <b>не выполняются</b>,
 * пока их явно не подтвердят через
 * {@link pro.wata.sdk.digitalgoods.stars.StarsClient#confirmOrder(String)}.
 */
public final class StarsOrderStatus {

    private static final Map<String, StarsOrderStatus> KNOWN = EnumSupport.registry();

    public static final StarsOrderStatus PENDING = register("Pending");
    /** Требует ручного подтверждения — заказ не выполняется автоматически. */
    public static final StarsOrderStatus REVIEW = register("Review");
    public static final StarsOrderStatus PAID = register("Paid");
    public static final StarsOrderStatus REFUNDED = register("Refunded");
    public static final StarsOrderStatus SUCCESS = register("Success");
    public static final StarsOrderStatus FAIL = register("Fail");

    private final String value;
    private final boolean known;

    private StarsOrderStatus(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static StarsOrderStatus register(String value) {
        StarsOrderStatus status = new StarsOrderStatus(value, true);
        KNOWN.put(value, status);
        return status;
    }

    @JsonCreator
    public static StarsOrderStatus of(String value) {
        return EnumSupport.resolve(value, KNOWN, StarsOrderStatus::new);
    }

    @JsonValue
    public String value() {
        return value;
    }

    public boolean isKnown() {
        return known;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StarsOrderStatus other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
