package pro.wata.sdk.acquiring;

import pro.wata.sdk.http.HttpTransport;
import pro.wata.sdk.model.Transaction;
import pro.wata.sdk.model.TransactionPage;
import pro.wata.sdk.model.TransactionSearchQuery;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Транзакции: {@code /api/h2h/v2/transactions} (курсорная пагинация) и
 * {@code /api/h2h/transactions/{id}}.
 *
 * <p>Ручной перенос курсора между страницами — источник ошибок (пропуск
 * {@code CursorDate} ломает вторую страницу), поэтому пагинация всегда идёт
 * через {@link #pages(TransactionSearchQuery)} или {@link #stream(TransactionSearchQuery)},
 * которые сами переносят {@code CursorId}, {@code CursorAmount} и {@code CursorDate}.
 */
public final class TransactionsClient {

    private final HttpTransport transport;

    TransactionsClient(HttpTransport transport) {
        this.transport = transport;
    }

    public Transaction get(String transactionId) {
        return transport.get("/transactions/" + transactionId, null, Transaction.class);
    }

    /** Одна страница результатов, без переноса курсора. */
    public TransactionPage searchPage(TransactionSearchQuery query) {
        return transport.get("/v2/transactions", query.toQueryParams(), TransactionPage.class);
    }

    /** Итератор по страницам: каждый следующий вызов сам подставляет курсор предыдущей страницы. */
    public Iterator<TransactionPage> pages(TransactionSearchQuery firstPageQuery) {
        return new PageIterator(firstPageQuery);
    }

    /** Плоский поток транзакций по всем страницам, без ручной работы с курсором. */
    public Stream<Transaction> stream(TransactionSearchQuery firstPageQuery) {
        Iterator<Transaction> flat = new Iterator<>() {
            private final Iterator<TransactionPage> pageIterator = pages(firstPageQuery);
            private Iterator<Transaction> current = java.util.Collections.emptyIterator();

            @Override
            public boolean hasNext() {
                while (!current.hasNext() && pageIterator.hasNext()) {
                    current = pageIterator.next().items().iterator();
                }
                return current.hasNext();
            }

            @Override
            public Transaction next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return current.next();
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(flat, Spliterator.ORDERED), false);
    }

    private final class PageIterator implements Iterator<TransactionPage> {
        private TransactionSearchQuery nextQuery;
        private boolean exhausted = false;

        private PageIterator(TransactionSearchQuery firstPageQuery) {
            this.nextQuery = firstPageQuery;
        }

        @Override
        public boolean hasNext() {
            return !exhausted;
        }

        @Override
        public TransactionPage next() {
            if (exhausted) {
                throw new NoSuchElementException("No more transaction pages");
            }
            TransactionPage page = searchPage(nextQuery);
            if (page.hasNextPage()) {
                nextQuery = nextQuery.withCursor(page.nextCursorId(), page.nextCursorAmount(), page.nextCursorDate());
            } else {
                exhausted = true;
            }
            return page;
        }
    }
}
