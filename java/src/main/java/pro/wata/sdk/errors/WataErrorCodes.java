package pro.wata.sdk.errors;

/**
 * Известные коды ошибок WATA из поля {@code error.code}.
 *
 * <p>Список неполный по своей природе: платформа может добавить код без изменения
 * версии SDK. Поэтому это справочник констант, а не перечисление — код ошибки
 * всегда доступен как исходная строка, даже незнакомая.
 */
public final class WataErrorCodes {

    private WataErrorCodes() {
    }

    // Платёжные ссылки.
    public static final String LINK_NOT_FOUND = "PL_1001";
    public static final String LINK_INVALID = "PL_1002";
    public static final String LINK_EXPIRED = "PL_1003";

    // Шифрование карточных данных.
    public static final String CRYPTO_INVALID = "CRY_1001";

    // Возвраты. Транзакции: TRA_1001..TRA_1019 — валидация,
    // TRA_2001..TRA_2999 — отказы шлюза и эмитента.
    public static final String REFUND_INVALID_AMOUNT = "TRA_1101";
    public static final String REFUND_INSUFFICIENT_FUNDS = "TRA_1102";
    public static final String REFUND_PENDING_EXISTS = "TRA_1103";

    /** Семейство, к которому относится код ошибки WATA. */
    public enum Family {
        UNKNOWN,
        PAYMENT_LINK,
        CRYPTO,
        TRANSACTION,
        REFUND,
        ORDER,
        STEAM,
        STARS,
        TOPUP,
        VOUCHER
    }

    /**
     * Определяет семейство ошибки по префиксу кода. Позволяет обработать целую
     * группу отказов, не перечисляя каждый код по отдельности.
     *
     * @param code код из ответа WATA, может быть {@code null}
     * @return семейство ошибки либо {@link Family#UNKNOWN}
     */
    public static Family familyOf(String code) {
        if (code == null || code.isEmpty()) {
            return Family.UNKNOWN;
        }
        if (code.startsWith("PL_")) {
            return Family.PAYMENT_LINK;
        }
        if (code.startsWith("CRY_")) {
            return Family.CRYPTO;
        }
        if (code.startsWith("TRA_11")) {
            return Family.REFUND;
        }
        if (code.startsWith("TRA_")) {
            return Family.TRANSACTION;
        }
        if (code.startsWith("ORD_")) {
            return Family.ORDER;
        }
        if (code.startsWith("STM_")) {
            return Family.STEAM;
        }
        if (code.startsWith("STR_")) {
            return Family.STARS;
        }
        if (code.startsWith("TPP_")) {
            return Family.TOPUP;
        }
        if (code.startsWith("VCR_")) {
            return Family.VOUCHER;
        }
        return Family.UNKNOWN;
    }
}
