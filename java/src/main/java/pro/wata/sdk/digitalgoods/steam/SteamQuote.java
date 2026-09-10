package pro.wata.sdk.digitalgoods.steam;

import pro.wata.sdk.digitalgoods.DgObject;
import pro.wata.sdk.model.Currency;

/**
 * Котировка суммы Steam: одно из {@code amount}/{@code netAmount} задаётся
 * запросом, второе возвращает сервер. Комиссия и курс делают эти величины
 * разными — SDK намеренно называет их по-разному, а не общим словом «сумма».
 *
 * <p>Поля, не перечисленные здесь явно, попадают в {@link #extra()}.
 */
public final class SteamQuote extends DgObject {

    private Double amount;
    private Double netAmount;
    private Currency currency;

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
