package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Objects;

/**
 * Статус заказа цифровых товаров (Steam, Top-Up, ваучеры):
 * {@code Pending} &rarr; {@code Paid} &rarr; {@code Success} | {@code Fail}.
 */
public final class DgOrderStatus {

    private static final Map<String, DgOrderStatus> KNOWN = EnumSupport.registry();

    public static final DgOrderStatus PENDING = register("Pending");
    public static final DgOrderStatus PAID = register("Paid");
    public static final DgOrderStatus SUCCESS = register("Success");
    public static final DgOrderStatus FAIL = register("Fail");

    private final String value;
    private final boolean known;

    private DgOrderStatus(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static DgOrderStatus register(String value) {
        DgOrderStatus status = new DgOrderStatus(value, true);
        KNOWN.put(value, status);
        return status;
    }

    @JsonCreator
    public static DgOrderStatus of(String value) {
        return EnumSupport.resolve(value, KNOWN, DgOrderStatus::new);
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
        if (!(o instanceof DgOrderStatus other)) return false;
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
