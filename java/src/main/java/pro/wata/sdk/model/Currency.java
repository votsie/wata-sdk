package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Objects;

/**
 * Валюта. {@code RUB}, {@code USD}, {@code EUR} подтверждены документацией.
 * {@code GBP} присутствует в схеме API, но документацией не подтверждён —
 * включён на всякий случай.
 *
 * <p>Значение, не входящее в этот список, не роняет разбор ответа: оно
 * оборачивается как есть, {@link #isKnown()} возвращает {@code false}, а
 * {@link #value()} сохраняет исходную строку сервера.
 */
public final class Currency {

    private static final Map<String, Currency> KNOWN = EnumSupport.registry();

    public static final Currency RUB = register("RUB");
    public static final Currency USD = register("USD");
    public static final Currency EUR = register("EUR");
    /** Не подтверждён документацией WATA, но присутствует в схеме API. */
    public static final Currency GBP = register("GBP");

    private final String value;
    private final boolean known;

    private Currency(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static Currency register(String value) {
        Currency currency = new Currency(value, true);
        KNOWN.put(value, currency);
        return currency;
    }

    @JsonCreator
    public static Currency of(String value) {
        return EnumSupport.resolve(value, KNOWN, Currency::new);
    }

    @JsonValue
    public String value() {
        return value;
    }

    /** {@code false}, если значение получено от сервера, но не описано в этом SDK. */
    public boolean isKnown() {
        return known;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Currency other)) return false;
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
