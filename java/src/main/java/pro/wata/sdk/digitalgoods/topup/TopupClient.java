package pro.wata.sdk.digitalgoods.topup;

import com.fasterxml.jackson.databind.JsonNode;
import pro.wata.sdk.digitalgoods.deposit.DepositClient;
import pro.wata.sdk.digitalgoods.deposit.DepositOrder;
import pro.wata.sdk.http.HttpTransport;

import java.util.Map;

/** Top-Up (токен терминала Top-Up). */
public final class TopupClient {

    private final HttpTransport transport;
    private final DepositClient deposit;

    public TopupClient(HttpTransport transport) {
        this.transport = transport;
        this.deposit = new DepositClient(transport);
    }

    /** {@code GET /v3/topup/all}. SPEC.md не описывает форму ответа — возвращается как есть. */
    public JsonNode listOffers() {
        return transport.get("/v3/topup/all", null, JsonNode.class);
    }

    /**
     * {@code POST /v3/topup}. SPEC.md не перечисляет тело запроса — передайте
     * его целиком. Изменяющий запрос, по умолчанию не повторяется.
     */
    public TopUpOrder createOrder(Map<String, Object> body) {
        return transport.post("/v3/topup", body, TopUpOrder.class, false);
    }

    /** {@code GET /v3/topup/orders/{id}}. */
    public TopUpOrder getOrder(String id) {
        return transport.get("/v3/topup/orders/" + id, null, TopUpOrder.class);
    }

    /** {@code GET /v1/deposit/topups}. */
    public JsonNode listDepositOffers() {
        return transport.get("/v1/deposit/topups", null, JsonNode.class);
    }

    /** {@code POST /v1/deposit/topups}. Изменяющий запрос, по умолчанию не повторяется. */
    public DepositOrder createDepositOrder(Map<String, Object> body) {
        return transport.post("/v1/deposit/topups", body, DepositOrder.class, false);
    }

    /** Депозитный баланс и статус общих депозитных заказов — см. {@link DepositClient}. */
    public DepositClient deposit() {
        return deposit;
    }
}
