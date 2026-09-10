package pro.wata.sdk.acquiring;

import org.junit.jupiter.api.Test;
import pro.wata.sdk.http.HttpTransport;
import pro.wata.sdk.http.RetryPolicy;
import pro.wata.sdk.model.Transaction;
import pro.wata.sdk.model.TransactionPage;
import pro.wata.sdk.model.TransactionSearchQuery;
import pro.wata.sdk.support.MockHttpServer;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Обязательное покрытие по п.9.3 SPEC.md: курсорная пагинация через две
 * страницы, без ручного переноса {@code CursorId}/{@code CursorAmount}/{@code CursorDate}.
 */
class TransactionsPaginationTest {

    @Test
    void iteratorCarriesTheCursorAcrossTwoPages() {
        String page1 = """
                {"items":[{"id":"tx-1","status":"Paid","kind":"Payment"}],
                 "hasNextPage":true,"nextCursorId":"tx-1","nextCursorAmount":100.0,
                 "nextCursorDate":"2026-01-01T10:00:00Z"}
                """;
        String page2 = """
                {"items":[{"id":"tx-2","status":"Paid","kind":"Payment"}],
                 "hasNextPage":false}
                """;

        AtomicInteger callCount = new AtomicInteger();
        try (MockHttpServer server = new MockHttpServer()
                .handle("/v2/transactions", exchange -> {
                    int call = callCount.incrementAndGet();
                    MockHttpServer.respond(exchange, 200, call == 1 ? page1 : page2);
                })
                .start()) {

            HttpTransport transport = new HttpTransport(server.baseUrl(), "test-token", Duration.ofSeconds(5), RetryPolicy.DEFAULT);
            TransactionsClient client = new TransactionsClient(transport);

            Iterator<TransactionPage> pages = client.pages(TransactionSearchQuery.builder().build());

            assertTrue(pages.hasNext());
            TransactionPage first = pages.next();
            assertEquals(1, first.items().size());
            assertEquals("tx-1", first.items().get(0).id());
            assertTrue(first.hasNextPage());

            assertTrue(pages.hasNext());
            TransactionPage second = pages.next();
            assertEquals("tx-2", second.items().get(0).id());
            assertFalse(second.hasNextPage());
            assertFalse(pages.hasNext());

            assertEquals(2, server.requests().size());
            String secondRequestUri = server.requests().get(1).uri();
            assertTrue(secondRequestUri.contains("CursorId=tx-1"), "Second page request must carry CursorId: " + secondRequestUri);
            assertTrue(secondRequestUri.contains("CursorAmount=100.0"), "Second page request must carry CursorAmount: " + secondRequestUri);
            assertTrue(secondRequestUri.contains("CursorDate="), "Second page request must carry CursorDate: " + secondRequestUri);
        }
    }

    @Test
    void streamFlattensBothPagesWithoutManualCursorHandling() {
        String page1 = """
                {"items":[{"id":"tx-1","status":"Paid","kind":"Payment"}],
                 "hasNextPage":true,"nextCursorId":"tx-1","nextCursorAmount":100.0,
                 "nextCursorDate":"2026-01-01T10:00:00Z"}
                """;
        String page2 = """
                {"items":[{"id":"tx-2","status":"Paid","kind":"Payment"}],
                 "hasNextPage":false}
                """;
        AtomicInteger callCount = new AtomicInteger();
        try (MockHttpServer server = new MockHttpServer()
                .handle("/v2/transactions", exchange -> {
                    int call = callCount.incrementAndGet();
                    MockHttpServer.respond(exchange, 200, call == 1 ? page1 : page2);
                })
                .start()) {

            HttpTransport transport = new HttpTransport(server.baseUrl(), "test-token", Duration.ofSeconds(5), RetryPolicy.DEFAULT);
            TransactionsClient client = new TransactionsClient(transport);

            List<String> ids = client.stream(TransactionSearchQuery.builder().build())
                    .map(Transaction::id)
                    .collect(Collectors.toList());

            assertEquals(List.of("tx-1", "tx-2"), ids);
        }
    }
}
