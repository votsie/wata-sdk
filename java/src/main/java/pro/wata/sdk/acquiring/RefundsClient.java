package pro.wata.sdk.acquiring;

import pro.wata.sdk.http.HttpTransport;
import pro.wata.sdk.model.RefundRequest;
import pro.wata.sdk.model.RefundResult;

/** Возвраты: {@code POST /api/h2h/transactions/refunds}. */
public final class RefundsClient {

    private final HttpTransport transport;

    RefundsClient(HttpTransport transport) {
        this.transport = transport;
    }

    /**
     * Создаёт возврат. Изменяющий запрос — по умолчанию не повторяется при
     * сбое: повтор мог бы создать второй возврат.
     */
    public RefundResult refund(RefundRequest request) {
        return transport.post("/transactions/refunds", request, RefundResult.class, false);
    }
}
