import { Transport, mapQuery } from '../http.js';
import type { Currency, TransactionKind, TransactionStatus, TransactionType } from '../types.js';

export interface Transaction {
  id: string;
  orderId?: string;
  amount: number;
  currency: Currency;
  status: TransactionStatus;
  kind: TransactionKind;
  type?: TransactionType;
  creationTime: string;
  paymentLinkId?: string;
  errorCode?: string;
  errorDescription?: string;
  /** Комиссия. В вебхуке то же значение называется `commission`. */
  totalCommission?: number;
  email?: string;
}

export interface SearchTransactionsQuery {
  orderId?: string;
  creationTimeFrom?: string;
  creationTimeTo?: string;
  amountFrom?: number;
  amountTo?: number;
  currencies?: Currency[];
  paymentLinkIds?: string[];
  statuses?: TransactionStatus[];
  sorting?: string;
  maxResultCount?: number;
  cursorId?: string;
  cursorAmount?: number;
  cursorDate?: string;
}

export interface SearchTransactionsResponse {
  items: Transaction[];
  hasNextPage: boolean;
  nextCursorId?: string;
  nextCursorDate?: string;
  nextCursorAmount?: number;
}

const SEARCH_TRANSACTIONS_KEY_MAP: Record<string, string> = {
  orderId: 'OrderId',
  creationTimeFrom: 'CreationTimeFrom',
  creationTimeTo: 'CreationTimeTo',
  amountFrom: 'AmountFrom',
  amountTo: 'AmountTo',
  currencies: 'Currencies',
  paymentLinkIds: 'PaymentLinkIds',
  statuses: 'Statuses',
  sorting: 'Sorting',
  maxResultCount: 'MaxResultCount',
  cursorId: 'CursorId',
  cursorAmount: 'CursorAmount',
  cursorDate: 'CursorDate',
};

/** Курсорные поля страницы поиска транзакций. */
type Cursor = Pick<SearchTransactionsQuery, 'cursorId' | 'cursorAmount' | 'cursorDate'>;

export class TransactionsApi {
  constructor(private readonly transport: Transport) {}

  /** Поиск транзакций (одна страница). Для перебора всех страниц используйте `iterate()`. */
  search(query: SearchTransactionsQuery = {}): Promise<SearchTransactionsResponse> {
    return this.transport.request<SearchTransactionsResponse>({
      method: 'GET',
      path: '/v2/transactions',
      query: mapQuery(query, SEARCH_TRANSACTIONS_KEY_MAP),
    });
  }

  /** Транзакция по идентификатору. */
  get(id: string): Promise<Transaction> {
    return this.transport.request<Transaction>({
      method: 'GET',
      path: `/transactions/${encodeURIComponent(id)}`,
    });
  }

  /**
   * Асинхронный генератор по всем страницам поиска транзакций. Сам
   * переносит `CursorId`, `CursorAmount` и `CursorDate` между
   * запросами — ручное листание пропускает `CursorDate` и ломает
   * вторую страницу, поэтому предпочитайте этот метод прямому вызову
   * `search()` в цикле.
   *
   * @example
   * for await (const tx of client.acquiring.transactions.iterate({ statuses: ['Paid'] })) {
   *   console.log(tx.id);
   * }
   */
  async *iterate(query: SearchTransactionsQuery = {}): AsyncGenerator<Transaction, void, void> {
    let cursor: Cursor = {
      cursorId: query.cursorId,
      cursorAmount: query.cursorAmount,
      cursorDate: query.cursorDate,
    };
    let hasNextPage = true;

    while (hasNextPage) {
      const page = await this.search({ ...query, ...cursor });
      for (const item of page.items) {
        yield item;
      }
      hasNextPage = page.hasNextPage;
      cursor = {
        cursorId: page.nextCursorId,
        cursorAmount: page.nextCursorAmount,
        cursorDate: page.nextCursorDate,
      };
      // Защита от зацикливания: сервер сообщил hasNextPage, но не дал курсор.
      if (hasNextPage && !cursor.cursorId && !cursor.cursorDate && cursor.cursorAmount === undefined) {
        break;
      }
    }
  }
}
