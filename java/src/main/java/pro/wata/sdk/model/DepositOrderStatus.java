package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Objects;

/** Статус депозитного заказа: {@code Pending}, {@code Success}, {@code Fail}. */
public final class DepositOrderStatus {

    private static final Map<String, DepositOrderStatus> KNOWN = EnumSupport.registry();

    public static final DepositOrderStatus PENDING = register("Pending");
    public static final DepositOrderStatus SUCCESS = register("Success");
    public static final DepositOrderStatus FAIL = register("Fail");

    private final String value;
    private final boolean known;

    private DepositOrderStatus(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static DepositOrderStatus register(String value) {
        DepositOrderStatus status = new DepositOrderStatus(value, true);
        KNOWN.put(value, status);
        return status;
    }

    @JsonCreator
    public static DepositOrderStatus of(String value) {
        return EnumSupport.resolve(value, KNOWN, DepositOrderStatus::new);
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
        if (!(o instanceof DepositOrderStatus other)) return false;
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
