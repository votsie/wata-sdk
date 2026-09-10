package pro.wata.sdk.acquiring;

import com.fasterxml.jackson.databind.ObjectMapper;
import pro.wata.sdk.http.HttpTransport;

/** Эквайринг (H2H): ссылки, транзакции, возвраты, баланс, прямые платежи. */
public final class AcquiringClient {

    private final LinksClient links;
    private final TransactionsClient transactions;
    private final RefundsClient refunds;
    private final BalanceClient balance;
    private final PaymentsClient payments;

    public AcquiringClient(HttpTransport transport, ObjectMapper mapper) {
        this.links = new LinksClient(transport, mapper);
        this.transactions = new TransactionsClient(transport);
        this.refunds = new RefundsClient(transport);
        this.balance = new BalanceClient(transport);
        this.payments = new PaymentsClient(transport);
    }

    public LinksClient links() {
        return links;
    }

    public TransactionsClient transactions() {
        return transactions;
    }

    public RefundsClient refunds() {
        return refunds;
    }

    public BalanceClient balance() {
        return balance;
    }

    public PaymentsClient payments() {
        return payments;
    }
}
