package pro.wata.sdk.digitalgoods.deposit;

import pro.wata.sdk.http.HttpTransport;

/**
 * Депозитные операции, общие для Steam/Top-Up/ваучеров: баланс депозита и
 * статус депозитного заказа. Каждый продуктовый клиент ({@code SteamClient},
 * {@code TopupClient}, {@code VouchersClient}) держит свой экземпляр,
 * построенный на транспорте со своим токеном — токены между продуктами не
 * переиспользуются, в том числе здесь.
 */
public final class DepositClient {

    private final HttpTransport transport;

    public DepositClient(HttpTransport transport) {
        this.transport = transport;
    }

    /** {@code GET /v1/deposit/balance}. */
    public DepositBalance balance() {
        return transport.get("/v1/deposit/balance", null, DepositBalance.class);
    }

    /** {@code GET /v1/deposit/order/{orderId}}. */
    public DepositOrder getOrder(String orderId) {
        return transport.get("/v1/deposit/order/" + orderId, null, DepositOrder.class);
    }
}
