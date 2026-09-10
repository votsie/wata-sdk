package pro.wata.sdk.digitalgoods.stars;

import pro.wata.sdk.digitalgoods.DgObject;
import pro.wata.sdk.model.Currency;

/** Ответ {@code GET /stars/price}: стоимость и минимальный размер заказа. */
public final class StarsPrice extends DgObject {

    private Double price;
    private Integer minimum;
    private Currency currency;

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getMinimum() {
        return minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
}
