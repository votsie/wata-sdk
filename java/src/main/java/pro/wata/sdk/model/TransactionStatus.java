package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Objects;

/** Статус транзакции: {@code Created}, {@code Pending}, {@code Paid}, {@code Declined}. */
public final class TransactionStatus {

    private static final Map<String, TransactionStatus> KNOWN = EnumSupport.registry();

    public static final TransactionStatus CREATED = register("Created");
    public static final TransactionStatus PENDING = register("Pending");
    public static final TransactionStatus PAID = register("Paid");
    public static final TransactionStatus DECLINED = register("Declined");

    private final String value;
    private final boolean known;

    private TransactionStatus(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static TransactionStatus register(String value) {
        TransactionStatus status = new TransactionStatus(value, true);
        KNOWN.put(value, status);
        return status;
    }

    @JsonCreator
    public static TransactionStatus of(String value) {
        return EnumSupport.resolve(value, KNOWN, TransactionStatus::new);
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
        if (!(o instanceof TransactionStatus other)) return false;
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
