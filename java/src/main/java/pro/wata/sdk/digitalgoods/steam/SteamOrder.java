package pro.wata.sdk.digitalgoods.steam;

import pro.wata.sdk.digitalgoods.DgObject;
import pro.wata.sdk.model.Currency;
import pro.wata.sdk.model.DgOrderStatus;

/** Заказ пополнения Steam, оплаченный эквайрингом. Поля вне схемы — в {@link #extra()}. */
public final class SteamOrder extends DgObject {

    private String id;
    private DgOrderStatus status;
    private Double amount;
    private Double netAmount;
    private Currency currency;

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

    public Double getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(Double netAmount) {
        this.netAmount = netAmount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
}
