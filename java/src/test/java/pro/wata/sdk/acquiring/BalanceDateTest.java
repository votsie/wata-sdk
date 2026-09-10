package pro.wata.sdk.acquiring;

import org.junit.jupiter.api.Test;
import pro.wata.sdk.errors.WataConfigException;
import pro.wata.sdk.http.HttpTransport;
import pro.wata.sdk.http.RetryPolicy;
import pro.wata.sdk.model.Balance;
import pro.wata.sdk.support.MockHttpServer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Обязательное покрытие по п.9.3 SPEC.md: проверка даты баланса. Допустимы
 * только сегодняшняя и вчерашняя дата по UTC, локально, до сетевого вызова
 * (см. п.4 SPEC.md, раздел «Баланс»).
 */
class BalanceDateTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void rejectsADateOlderThanYesterdayWithoutCallingTheServer() {
        try (MockHttpServer server = new MockHttpServer().start()) {
            HttpTransport transport = new HttpTransport(server.baseUrl(), "test-token", Duration.ofSeconds(5), RetryPolicy.DEFAULT);
            BalanceClient balanceClient = new BalanceClient(transport, FIXED_CLOCK);

            assertThrows(WataConfigException.class, () -> balanceClient.get(java.time.LocalDate.parse("2026-06-13")));
            assertEquals(0, server.requests().size(), "An invalid balance date must never reach the network");
        }
    }

    @Test
    void rejectsAFutureDateWithoutCallingTheServer() {
        try (MockHttpServer server = new MockHttpServer().start()) {
            HttpTransport transport = new HttpTransport(server.baseUrl(), "test-token", Duration.ofSeconds(5), RetryPolicy.DEFAULT);
            BalanceClient balanceClient = new BalanceClient(transport, FIXED_CLOCK);

            assertThrows(WataConfigException.class, () -> balanceClient.get(java.time.LocalDate.parse("2026-06-16")));
            assertEquals(0, server.requests().size());
        }
    }

    @Test
    void acceptsTodayAndYesterdayInUtc() {
        try (MockHttpServer server = new MockHttpServer()
                .json("/finance/balance", 200,
                        "{\"terminalPublicId\":\"t1\",\"date\":\"2026-06-15\",\"balance\":100.5,\"currency\":\"RUB\"}")
                .start()) {

            HttpTransport transport = new HttpTransport(server.baseUrl(), "test-token", Duration.ofSeconds(5), RetryPolicy.DEFAULT);
            BalanceClient balanceClient = new BalanceClient(transport, FIXED_CLOCK);

            Balance today = balanceClient.get(java.time.LocalDate.parse("2026-06-15"));
            assertEquals(100.5, today.balance());

            Balance yesterday = balanceClient.get(java.time.LocalDate.parse("2026-06-14"));
            assertEquals("t1", yesterday.terminalPublicId());

            assertEquals(2, server.requests().size());
        }
    }
}
