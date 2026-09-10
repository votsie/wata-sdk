package pro.wata.sdk.acquiring;

import org.junit.jupiter.api.Test;
import pro.wata.sdk.http.HttpTransport;
import pro.wata.sdk.http.JsonSupport;
import pro.wata.sdk.http.RetryPolicy;
import pro.wata.sdk.model.CreateLinkRequest;
import pro.wata.sdk.model.Currency;
import pro.wata.sdk.model.PaymentLink;
import pro.wata.sdk.support.MockHttpServer;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверяет заголовок авторизации и тело запроса на создание платёжной ссылки. */
class LinksClientTest {

    @Test
    void createSendsTheBearerTokenAndTheRequestBody() {
        String responseBody = """
                {"id":"link-1","amount":1500.0,"currency":"RUB","status":"Opened",
                 "url":"https://pay.wata.pro/link-1","terminalName":"Main","terminalPublicId":"pub-1",
                 "creationTime":"2026-01-01T00:00:00Z","type":"OneTime"}
                """;

        try (MockHttpServer server = new MockHttpServer()
                .json("/links", 200, responseBody)
                .start()) {

            HttpTransport transport = new HttpTransport(server.baseUrl(), "secret-token", Duration.ofSeconds(5), RetryPolicy.DEFAULT);
            LinksClient links = new LinksClient(transport, JsonSupport.mapper());

            PaymentLink link = links.create(CreateLinkRequest.builder(1500.0, Currency.RUB).description("Order #1").build());

            assertEquals("link-1", link.id());
            assertEquals(Currency.RUB, link.currency());
            assertEquals(Currency.RUB.value(), "RUB");

            var request = server.requests().get(0);
            assertEquals("POST", request.method());
            assertEquals("Bearer secret-token", request.headers().get("Authorization").get(0));
            assertTrue(request.body().contains("\"amount\":1500.0"));
            assertTrue(request.body().contains("\"description\":\"Order #1\""));
        }
    }
}
