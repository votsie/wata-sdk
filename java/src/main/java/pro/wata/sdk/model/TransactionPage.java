package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.List;

/** Одна страница курсорного поиска транзакций. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionPage(
        List<Transaction> items,
        boolean hasNextPage,
        String nextCursorId,
        OffsetDateTime nextCursorDate,
        Double nextCursorAmount
) {
}
