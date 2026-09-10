package pro.wata.sdk.acquiring;

import org.junit.jupiter.api.Test;
import pro.wata.sdk.errors.WataServerException;
import pro.wata.sdk.http.HttpTransport;
import pro.wata.sdk.http.RetryPolicy;
import pro.wata.sdk.model.PaymentLink;
import pro.wata.sdk.model.RefundRequest;
import pro.wata.sdk.support.MockHttpServer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Обязательное покрытие по п.9.3 SPEC.md: отказ от повтора изменяющего
 * запроса. Создание ссылки/платежа/возврата не должно повторяться при сбое —
 * повтор мог бы создать вторую сущность (см. п.3 SPEC.md).
 */
class MutatingRequestRetryTest {

    private static final RetryPolicy FAST_RETRY = new RetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(5));

    @Test
    void refundIsNotRetriedOn5xx() {
        AtomicInteger callCount = new AtomicInteger();
        try (MockHttpServer server = new MockHttpServer()
                .handle("/transactions/refunds", exchange -> {
                    callCount.incrementAndGet();
                    MockHttpServer.respond(exchange, 500, "{\"error\":{\"code\":\"INTERNAL\",\"message\":\"boom\"}}");
                })
                .start()) {

            HttpTransport transport = new HttpTransport(server.baseUrl(), "test-token", Duration.ofSeconds(5), FAST_RETRY);
            RefundsClient refunds = new RefundsClient(transport);

            assertThrows(WataServerException.class,
                    () -> refunds.refund(new RefundRequest("11111111-1111-1111-1111-111111111111", 10.0)));

            assertEquals(1, callCount.get(), "A mutating request must not be retried on 5xx");
        }
    }

    @Test
    void getRequestsAreRetriedOn5xxUpToTheConfiguredAttempts() {
        AtomicInteger callCount = new AtomicInteger();
        try (MockHttpServer server = new MockHttpServer()
                .handle("/links/abc", exchange -> {
                    callCount.incrementAndGet();
                    MockHttpServer.respond(exchange, 503, "{\"error\":{\"code\":\"UNAVAILABLE\",\"message\":\"retry me\"}}");
                })
                .start()) {

            HttpTransport transport = new HttpTransport(server.baseUrl(), "test-token", Duration.ofSeconds(5), FAST_RETRY);
            LinksClient links = new LinksClient(transport, pro.wata.sdk.http.JsonSupport.mapper());

            assertThrows(WataServerException.class, () -> links.get("abc"));

            assertEquals(3, callCount.get(), "A read-only GET must be retried up to maxAttempts on 5xx");
        }
    }
}
