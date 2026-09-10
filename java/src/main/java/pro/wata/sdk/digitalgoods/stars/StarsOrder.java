package pro.wata.sdk.digitalgoods.stars;

import pro.wata.sdk.digitalgoods.DgObject;
import pro.wata.sdk.model.StarsOrderStatus;

/**
 * Заказ Telegram Stars. Если {@link #getStatus()} равен
 * {@link StarsOrderStatus#REVIEW}, заказ превысил порог автоподтверждения и
 * <b>не будет выполнен</b>, пока его явно не подтвердят через
 * {@link StarsClient#confirmOrder(String)} или отклонят через
 * {@link StarsClient#rejectOrder(String)}.
 */
public final class StarsOrder extends DgObject {

    private String id;
    private StarsOrderStatus status;
    private Integer count;
    private Double amount;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public StarsOrderStatus getStatus() {
        return status;
    }

    public void setStatus(StarsOrderStatus status) {
        this.status = status;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
