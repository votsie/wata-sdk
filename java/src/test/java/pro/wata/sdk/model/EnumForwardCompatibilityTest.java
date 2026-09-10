package pro.wata.sdk.model;

import org.junit.jupiter.api.Test;
import pro.wata.sdk.http.JsonSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Раздел 6 SPEC.md: неизвестное значение enum, пришедшее с сервера, не должно
 * ронять разбор ответа — исходная строка сохраняется, а не заменяется на null
 * или значение по умолчанию.
 */
class EnumForwardCompatibilityTest {

    @Test
    void unknownTransactionStatusIsPreservedInsteadOfFailingToParse() throws Exception {
        String json = """
                {"id":"tx-1","status":"SomeBrandNewStatus","kind":"Payment"}
                """;

        Transaction transaction = JsonSupport.mapper().readValue(json, Transaction.class);

        assertEquals("SomeBrandNewStatus", transaction.status().value());
        assertFalse(transaction.status().isKnown());
    }

    @Test
    void knownValuesRemainSingletonsAndEqualByValue() {
        assertTrue(Currency.of("RUB") == Currency.RUB);
        assertEquals(Currency.of("XYZ"), Currency.of("XYZ"));
        assertFalse(Currency.of("XYZ").isKnown());
    }
}
