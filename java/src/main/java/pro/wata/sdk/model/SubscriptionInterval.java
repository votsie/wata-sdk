package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Objects;

/** Интервал подписки: {@code Test}, {@code Week}, {@code Month}. */
public final class SubscriptionInterval {

    private static final Map<String, SubscriptionInterval> KNOWN = EnumSupport.registry();

    public static final SubscriptionInterval TEST = register("Test");
    public static final SubscriptionInterval WEEK = register("Week");
    public static final SubscriptionInterval MONTH = register("Month");

    private final String value;
    private final boolean known;

    private SubscriptionInterval(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static SubscriptionInterval register(String value) {
        SubscriptionInterval interval = new SubscriptionInterval(value, true);
        KNOWN.put(value, interval);
        return interval;
    }

    @JsonCreator
    public static SubscriptionInterval of(String value) {
        return EnumSupport.resolve(value, KNOWN, SubscriptionInterval::new);
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
        if (!(o instanceof SubscriptionInterval other)) return false;
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
