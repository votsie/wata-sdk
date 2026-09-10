package pro.wata.sdk.digitalgoods.stars;

import pro.wata.sdk.http.HttpTransport;

import java.util.Map;

/**
 * Telegram Stars (токен терминала Stars — всегда отдельный от Steam, см. п.1
 * SPEC.md). Количество звёзд: 50–50000.
 */
public final class StarsClient {

    private final HttpTransport transport;

    public StarsClient(HttpTransport transport) {
        this.transport = transport;
    }

    /** {@code GET /stars/price}. */
    public StarsPrice price() {
        return transport.get("/stars/price", null, StarsPrice.class);
    }

    /**
     * Создаёт заказ ({@code POST /stars}). SPEC.md не называет имя поля для
     * количества звёзд — передайте тело целиком (например {@code count},
     * получателя и т.д.). Если сумма заказа превышает порог
     * автоподтверждения, ответ придёт со статусом {@link pro.wata.sdk.model.StarsOrderStatus#REVIEW}
     * и заказ не будет выполнен, пока его не подтвердят.
     *
     * <p>Изменяющий запрос — по умолчанию не повторяется при сбое.
     */
    public StarsOrder createOrder(Map<String, Object> body) {
        return transport.post("/stars", body, StarsOrder.class, false);
    }

    /** {@code GET /stars/order/{id}}. */
    public StarsOrder getOrder(String id) {
        return transport.get("/stars/order/" + id, null, StarsOrder.class);
    }

    /** Подтверждает заказ в статусе {@code Review} ({@code POST /stars/order/{id}/confirm}). Не повторяется. */
    public StarsOrder confirmOrder(String id) {
        return transport.post("/stars/order/" + id + "/confirm", Map.of(), StarsOrder.class, false);
    }

    /** Отклоняет заказ в статусе {@code Review} ({@code POST /stars/order/{id}/reject}). Не повторяется. */
    public StarsOrder rejectOrder(String id) {
        return transport.post("/stars/order/" + id + "/reject", Map.of(), StarsOrder.class, false);
    }
}
