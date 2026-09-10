package pro.wata.sdk.digitalgoods.steam;

import pro.wata.sdk.digitalgoods.RequestBodies;
import pro.wata.sdk.digitalgoods.deposit.DepositClient;
import DepositOrder;
import pro.wata.sdk.http.HttpTransport;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Steam (токен терминала Steam — всегда отдельный от Stars, см. п.1 SPEC.md).
 *
 * <p>Два сценария пополнения различаются тем, что задано: сумма зачисления на
 * аккаунт ({@code netAmount}) или сумма платежа ({@code amount}/{@code price}).
 * Комиссия и курс делают эти величины разными, поэтому методы этого класса
 * называют их явно, а не «суммой» вообще.
 */
public final class SteamClient {

    private final HttpTransport transport;
    private final DepositClient deposit;

    public SteamClient(HttpTransport transport) {
        this.transport = transport;
        this.deposit = new DepositClient(transport);
    }

    // -- acquiring-funded ---------------------------------------------------

    /** Сумма платежа за зачисление {@code netAmount} ({@code GET /v3/steam/amount}). */
    public SteamQuote amountForNetAmount(double netAmount) {
        return quote("/v3/steam/amount", "netAmount", netAmount);
    }

    /** Сумма зачисления за платёж {@code amount} ({@code GET /v3/steam/by-amount}). */
    public SteamQuote netAmountForAmount(double amount) {
        return quote("/v3/steam/by-amount", "amount", amount);
    }

    /**
     * Создаёт заказ на зачисление {@code netAmount} ({@code POST /v3/steam}).
     * SPEC.md не перечисляет остальные поля тела (например идентификатор
     * Steam-аккаунта) — передайте их через {@code extra}. Изменяющий запрос,
     * по умолчанию не повторяется.
     */
    public SteamOrder createOrderByNetAmount(double netAmount, Map<String, Object> extra) {
        Map<String, Object> known = new LinkedHashMap<>();
        known.put("netAmount", netAmount);
        return transport.post("/v3/steam", RequestBodies.merge(known, extra), SteamOrder.class, false);
    }

    /** Создаёт заказ на оплату {@code amount} ({@code POST /v3/steam/by-amount}). См. {@link #createOrderByNetAmount}. */
    public SteamOrder createOrderByAmount(double amount, Map<String, Object> extra) {
        Map<String, Object> known = new LinkedHashMap<>();
        known.put("amount", amount);
        return transport.post("/v3/steam/by-amount", RequestBodies.merge(known, extra), SteamOrder.class, false);
    }

    /** {@code GET /v3/steam/order/{id}}. */
    public SteamOrder getOrder(String id) {
        return transport.get("/v3/steam/order/" + id, null, SteamOrder.class);
    }

    // -- deposit-funded -------------------------------------------------------

    /** {@code GET /v1/steam/deposit/price}. */
    public SteamQuote depositPriceForNetAmount(double netAmount) {
        return quote("/v1/steam/deposit/price", "netAmount", netAmount);
    }

    /** {@code GET /v1/steam/deposit/netamount}. */
    public SteamQuote depositNetAmountForAmount(double amount) {
        return quote("/v1/steam/deposit/netamount", "amount", amount);
    }

    /** {@code POST /v1/steam/deposit}. Изменяющий запрос, по умолчанию не повторяется. */
    public DepositOrder createDepositOrderByNetAmount(double netAmount, Map<String, Object> extra) {
        Map<String, Object> known = new LinkedHashMap<>();
        known.put("netAmount", netAmount);
        return transport.post("/v1/steam/deposit", RequestBodies.merge(known, extra),
                DepositOrder.class, false);
    }

    /** {@code POST /v1/steam/deposit/by-price}. Изменяющий запрос, по умолчанию не повторяется. */
    public DepositOrder createDepositOrderByPrice(double price, Map<String, Object> extra) {
        Map<String, Object> known = new LinkedHashMap<>();
        known.put("price", price);
        return transport.post("/v1/steam/deposit/by-price", RequestBodies.merge(known, extra),
                DepositOrder.class, false);
    }

    /** Депозитный баланс и статус общих депозитных заказов — см. {@link DepositClient}. */
    public DepositClient deposit() {
        return deposit;
    }

    private SteamQuote quote(String path, String paramName, double paramValue) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put(paramName, paramValue);
        return transport.get(path, query, SteamQuote.class);
    }
}
