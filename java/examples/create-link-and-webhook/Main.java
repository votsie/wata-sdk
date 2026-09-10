// Пример: создание платёжной ссылки и приём вебхука с проверкой подписи.
//
// Это не часть библиотеки (файл лежит вне src/main/java) — соберите и
// запустите его отдельно, положив wata-sdk и его зависимости (Jackson) в
// classpath, например после `mvn -q package`:
//
//   javac -cp "target/classes;target/dependency/*" -d target/examples examples/create-link-and-webhook/Main.java
//   java  -cp "target/classes;target/dependency/*;target/examples" Main
//
// (на Linux/macOS замените ";" на ":" в classpath).
//
// Без переменной окружения WATA_ACQUIRING_TOKEN пример пропустит создание
// ссылки и просто поднимет HTTP-сервер для приёма вебхуков — этого достаточно,
// чтобы направить на него curl или тестовую отправку вебхука из личного
// кабинета WATA.

import com.sun.net.httpserver.HttpServer;
import pro.wata.sdk.WataClient;
import pro.wata.sdk.errors.WataException;
import pro.wata.sdk.model.CreateLinkRequest;
import pro.wata.sdk.model.Currency;
import pro.wata.sdk.model.PaymentLink;
import pro.wata.sdk.model.WebhookEvent;
import pro.wata.sdk.webhooks.WebhookVerifier;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class Main {

    public static void main(String[] args) throws Exception {
        String acquiringToken = System.getenv("WATA_ACQUIRING_TOKEN");

        WataClient client = WataClient.builder()
                .acquiringToken(acquiringToken)
                .build();

        if (acquiringToken != null && !acquiringToken.isBlank()) {
            createLink(client);
        } else {
            System.out.println("WATA_ACQUIRING_TOKEN не задан — пропускаем создание ссылки, "
                    + "запускаем только сервер для вебхуков");
        }

        // Обработчик ниже — самая важная часть примера: единственный правильный
        // способ проверить подпись вебхука WATA.
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/webhooks/wata", exchange -> {
            try {
                handleWebhook(client, exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
        System.out.println("слушаем вебхуки WATA на http://localhost:8080/webhooks/wata");
    }

    private static void createLink(WataClient client) {
        try {
            PaymentLink link = client.acquiring().links().create(
                    CreateLinkRequest.builder(500.0, Currency.RUB)
                            .description("Пример заказа из create-link-and-webhook")
                            .orderId("example-order-1")
                            .build());
            System.out.printf("ссылка создана: id=%s url=%s status=%s%n", link.id(), link.url(), link.status());
        } catch (WataException e) {
            System.out.println("не удалось создать ссылку: " + e.getMessage());
        }
    }

    /**
     * Правила, которые важны именно здесь:
     * 1. Тело читается как сырые байты, и именно они передаются в verify().
     *    Никогда не разбирайте JSON заранее и не пересобирайте его для
     *    проверки — это меняет порядок ключей и ломает подпись.
     * 2. Всегда возвращайте 200 после того, как событие безопасно
     *    сохранено/поставлено в очередь, и делайте обработчик идемпотентным:
     *    WATA повторяет постоплатные вебхуки и вебхуки возврата до 32 часов,
     *    если не увидит 200.
     */
    private static void handleWebhook(WataClient client, com.sun.net.httpserver.HttpExchange exchange) throws Exception {
        byte[] rawBody = exchange.getRequestBody().readAllBytes();
        String signature = exchange.getRequestHeaders().getFirst("X-Signature");

        WebhookVerifier verifier = client.webhooks();
        boolean valid;
        try {
            valid = signature != null && verifier.verify(rawBody, signature);
        } catch (WataException e) {
            System.out.println("не удалось проверить подпись вебхука: " + e.getMessage());
            respond(exchange, 500, "verification failed");
            return;
        }

        if (!valid) {
            System.out.println("подпись вебхука не совпадает — запрос отклонён");
            respond(exchange, 400, "invalid signature");
            return;
        }

        WebhookEvent event = verifier.parse(rawBody);

        // TODO в реальном обработчике: найти event.orderId()/event.transactionId()
        // в своём хранилище и обеспечить идемпотентность до побочных эффектов
        // (отгрузка товара, отметка заказа оплаченным и т.д.).
        System.out.println("получен вебхук: " + event);

        respond(exchange, 200, "ok");
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws Exception {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
