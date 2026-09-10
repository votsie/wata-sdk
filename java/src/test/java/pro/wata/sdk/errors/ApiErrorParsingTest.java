package pro.wata.sdk.errors;

import org.junit.jupiter.api.Test;
import pro.wata.sdk.http.HttpTransport;
import pro.wata.sdk.http.RetryPolicy;
import pro.wata.sdk.model.PaymentLink;
import pro.wata.sdk.support.MockHttpServer;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Обязательное покрытие по п.9.3 SPEC.md: разбор ошибки с кодом WATA
 * ({@code PL_*}, {@code TRA_*}, ...), см. п.7 SPEC.md.
 */
class ApiErrorParsingTest {

    @Test
    void parsesTheWataErrorCodeFromA4xxResponse() {
        String body = """
                {"error":{"code":"PL_NOT_FOUND","message":"Payment link not found",
                          "details":{"linkId":"abc"},"validationErrors":null}}
                """;

        try (MockHttpServer server = new MockHttpServer()
                .json("/links/abc", 404, body)
                .start()) {

            HttpTransport transport = new HttpTransport(server.baseUrl(), "test-token", Duration.ofSeconds(5), RetryPolicy.DEFAULT);

            WataApiException exception = assertThrows(WataApiException.class,
                    () -> transport.get("/links/abc", null, PaymentLink.class));

            assertEquals("PL_NOT_FOUND", exception.wataErrorCode());
            assertEquals("Payment link not found", exception.getMessage());
            assertEquals(404, exception.httpStatus());
            assertEquals("/links/abc", exception.requestPath());
        }
    }

    @Test
    void authErrorsAreNeverRetriedAndAreNotApiExceptions() {
        try (MockHttpServer server = new MockHttpServer()
                .json("/links/abc", 401, "{\"error\":{\"code\":\"AUTH_INVALID\",\"message\":\"bad token\"}}")
                .start()) {

            HttpTransport transport = new HttpTransport(server.baseUrl(), "test-token", Duration.ofSeconds(5), RetryPolicy.DEFAULT);

            assertThrows(WataAuthException.class, () -> transport.get("/links/abc", null, PaymentLink.class));
            assertEquals(1, server.requests().size(), "401 must not be retried");
        }
    }
}
