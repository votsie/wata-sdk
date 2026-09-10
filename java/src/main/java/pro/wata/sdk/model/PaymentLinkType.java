package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Objects;

/** Тип платёжной ссылки: {@code OneTime} (по умолчанию) или {@code ManyTime}. */
public final class PaymentLinkType {

    private static final Map<String, PaymentLinkType> KNOWN = EnumSupport.registry();

    public static final PaymentLinkType ONE_TIME = register("OneTime");
    public static final PaymentLinkType MANY_TIME = register("ManyTime");

    private final String value;
    private final boolean known;

    private PaymentLinkType(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static PaymentLinkType register(String value) {
        PaymentLinkType type = new PaymentLinkType(value, true);
        KNOWN.put(value, type);
        return type;
    }

    @JsonCreator
    public static PaymentLinkType of(String value) {
        return EnumSupport.resolve(value, KNOWN, PaymentLinkType::new);
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
        if (!(o instanceof PaymentLinkType other)) return false;
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
