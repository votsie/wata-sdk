package pro.wata.sdk.digitalgoods.topup;

import pro.wata.sdk.digitalgoods.DgObject;
import pro.wata.sdk.model.DgOrderStatus;

/** Заказ Top-Up, оплаченный эквайрингом ({@code /v3/topup}, {@code /v3/topup/orders/{id}}). */
public final class TopUpOrder extends DgObject {

    private String id;
    private DgOrderStatus status;
    private Double amount;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DgOrderStatus getStatus() {
        return status;
    }

    public void setStatus(DgOrderStatus status) {
        this.status = status;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
