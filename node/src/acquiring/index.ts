import type { Transport } from '../http.js';
import { BalanceApi } from './balance.js';
import { LinksApi } from './links.js';
import { PaymentsApi } from './payments.js';
import { RefundsApi } from './refunds.js';
import { TransactionsApi } from './transactions.js';

export * from './balance.js';
export * from './links.js';
export * from './payments.js';
export * from './refunds.js';
export * from './transactions.js';

/** Продукт "Эквайринг" (H2H): ссылки, транзакции, возвраты, баланс, прямые платежи. */
export class AcquiringApi {
  readonly links: LinksApi;
  readonly transactions: TransactionsApi;
  readonly refunds: RefundsApi;
  readonly balance: BalanceApi;
  readonly payments: PaymentsApi;

  constructor(transport: Transport) {
    this.links = new LinksApi(transport);
    this.transactions = new TransactionsApi(transport);
    this.refunds = new RefundsApi(transport);
    this.balance = new BalanceApi(transport);
    this.payments = new PaymentsApi(transport);
  }
}
