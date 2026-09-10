package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Objects;

/** Вид транзакции: {@code Payment} или {@code Refund}. */
public final class TransactionKind {

    private static final Map<String, TransactionKind> KNOWN = EnumSupport.registry();

    public static final TransactionKind PAYMENT = register("Payment");
    public static final TransactionKind REFUND = register("Refund");

    private final String value;
    private final boolean known;

    private TransactionKind(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static TransactionKind register(String value) {
        TransactionKind kind = new TransactionKind(value, true);
        KNOWN.put(value, kind);
        return kind;
    }

    @JsonCreator
    public static TransactionKind of(String value) {
        return EnumSupport.resolve(value, KNOWN, TransactionKind::new);
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
        if (!(o instanceof TransactionKind other)) return false;
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
