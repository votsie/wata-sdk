package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Objects;

/** Статус платёжной ссылки: {@code Opened} или {@code Closed}. */
public final class PaymentLinkStatus {

    private static final Map<String, PaymentLinkStatus> KNOWN = EnumSupport.registry();

    public static final PaymentLinkStatus OPENED = register("Opened");
    public static final PaymentLinkStatus CLOSED = register("Closed");

    private final String value;
    private final boolean known;

    private PaymentLinkStatus(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static PaymentLinkStatus register(String value) {
        PaymentLinkStatus status = new PaymentLinkStatus(value, true);
        KNOWN.put(value, status);
        return status;
    }

    @JsonCreator
    public static PaymentLinkStatus of(String value) {
        return EnumSupport.resolve(value, KNOWN, PaymentLinkStatus::new);
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
        if (!(o instanceof PaymentLinkStatus other)) return false;
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
