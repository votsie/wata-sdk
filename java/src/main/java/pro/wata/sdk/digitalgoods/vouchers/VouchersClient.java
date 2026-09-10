package pro.wata.sdk.digitalgoods.vouchers;

import com.fasterxml.jackson.databind.JsonNode;
import pro.wata.sdk.digitalgoods.deposit.DepositClient;
import pro.wata.sdk.http.HttpTransport;

import java.util.Map;

/** Ваучеры (токен терминала Vouchers). */
public final class VouchersClient {

    private final HttpTransport transport;
    private final DepositClient deposit;

    public VouchersClient(HttpTransport transport) {
        this.transport = transport;
        this.deposit = new DepositClient(transport);
    }

    /** {@code GET /v3/vouchers/all}. SPEC.md не описывает форму ответа — возвращается как есть. */
    public JsonNode listOffers() {
        return transport.get("/v3/vouchers/all", null, JsonNode.class);
    }

    /**
     * {@code POST /v3/vouchers}. SPEC.md не перечисляет тело запроса —
     * передайте его целиком. Коды здесь не возвращаются — опрашивайте
     * {@link #getOrder(String)}, они могут появиться с задержкой до 10 минут.
     * Изменяющий запрос, по умолчанию не повторяется.
     */
    public VoucherOrder createOrder(Map<String, Object> body) {
        return transport.post("/v3/vouchers", body, VoucherOrder.class, false);
    }

    /** {@code GET /v3/vouchers/order/{id}}, включая коды, если уже выданы. */
    public VoucherOrder getOrder(String id) {
        return transport.get("/v3/vouchers/order/" + id, null, VoucherOrder.class);
    }

    /** {@code GET /v1/deposit/vouchers}. */
    public JsonNode listDepositOffers() {
        return transport.get("/v1/deposit/vouchers", null, JsonNode.class);
    }

    /** {@code POST /v1/deposit/vouchers}. Изменяющий запрос, по умолчанию не повторяется. */
    public VoucherOrder createDepositOrder(Map<String, Object> body) {
        return transport.post("/v1/deposit/vouchers", body, VoucherOrder.class, false);
    }

    /** Депозитный баланс и статус общих депозитных заказов — см. {@link DepositClient}. */
    public DepositClient deposit() {
        return deposit;
    }
}
