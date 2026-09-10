package pro.wata.sdk.digitalgoods.deposit;

import pro.wata.sdk.digitalgoods.DgObject;
import pro.wata.sdk.model.DepositOrderStatus;

/**
 * Статус заказа, оплаченного с депозита мерчанта
 * ({@code GET /v1/deposit/order/{orderId}}). Общий эндпоинт для
 * депозитных заказов Steam/Top-Up/ваучеров.
 */
public final class DepositOrder extends DgObject {

    private String orderId;
    private String id;
    private DepositOrderStatus status;
    private Double amount;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DepositOrderStatus getStatus() {
        return status;
    }

    public void setStatus(DepositOrderStatus status) {
        this.status = status;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
