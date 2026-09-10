package pro.wata.sdk.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * Мок HTTP-сервера на {@link HttpServer} из JDK — без внешних библиотек вроде
 * WireMock, как того требует п.9.3 SPEC.md ("тесты без сети — на моках HTTP").
 */
public final class MockHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final CopyOnWriteArrayList<RecordedRequest> requests = new CopyOnWriteArrayList<>();

    public MockHttpServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            this.server.setExecutor(Executors.newCachedThreadPool());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start mock HTTP server", e);
        }
    }

    public MockHttpServer start() {
        server.start();
        return this;
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public java.util.List<RecordedRequest> requests() {
        return requests;
    }

    /** Регистрирует обработчик, возвращающий фиксированный JSON-ответ на каждый запрос к пути. */
    public MockHttpServer json(String path, int status, String jsonBody) {
        return handle(path, exchange -> respond(exchange, status, jsonBody));
    }

    /** Регистрирует обработчик, вызываемый на каждый запрос — для последовательностей ответов или подсчёта попыток. */
    public MockHttpServer handle(String path, HttpHandler handler) {
        server.createContext(path, exchange -> {
            byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
            requests.add(new RecordedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().toString(),
                    new String(bodyBytes, StandardCharsets.UTF_8),
                    Map.copyOf(exchange.getRequestHeaders())));
            handler.handle(exchange);
        });
        return this;
    }

    public static void respond(HttpExchange exchange, int status, String jsonBody) {
        try {
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    public record RecordedRequest(String method, String uri, String body, Map<String, java.util.List<String>> headers) {
    }
}
