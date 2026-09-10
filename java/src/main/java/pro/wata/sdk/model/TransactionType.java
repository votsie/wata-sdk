package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Objects;

/** Способ оплаты транзакции: {@code CardCrypto}, {@code SBP}, {@code TPay}. */
public final class TransactionType {

    private static final Map<String, TransactionType> KNOWN = EnumSupport.registry();

    public static final TransactionType CARD_CRYPTO = register("CardCrypto");
    public static final TransactionType SBP = register("SBP");
    public static final TransactionType T_PAY = register("TPay");

    private final String value;
    private final boolean known;

    private TransactionType(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static TransactionType register(String value) {
        TransactionType type = new TransactionType(value, true);
        KNOWN.put(value, type);
        return type;
    }

    @JsonCreator
    public static TransactionType of(String value) {
        return EnumSupport.resolve(value, KNOWN, TransactionType::new);
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
        if (!(o instanceof TransactionType other)) return false;
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
