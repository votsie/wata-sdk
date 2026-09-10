package pro.wata.sdk.digitalgoods.vouchers;

import pro.wata.sdk.digitalgoods.DgObject;
import pro.wata.sdk.model.DgOrderStatus;

import java.util.List;

/**
 * Заказ ваучера. Отдельного эндпоинта выдачи кодов нет — {@link #getCodes()}
 * заполняется прямо в статусе заказа и может появиться с задержкой до 10 минут
 * после оплаты. Точное имя поля с кодами SPEC.md не называет; если у живого
 * API оно другое, значение всё равно доступно через {@link #extra()}.
 */
public final class VoucherOrder extends DgObject {

    private String id;
    private DgOrderStatus status;
    private Double amount;
    private List<String> codes;

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

    public List<String> getCodes() {
        return codes;
    }

    public void setCodes(List<String> codes) {
        this.codes = codes;
    }
}
