package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Ответ на запрос возврата. {@code kind} всегда {@link TransactionKind#REFUND}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RefundResult(
        String transactionId,
        String originalTransactionId,
        TransactionStatus transactionStatus,
        TransactionKind kind,
        String errorCode,
        String errorDescription
) {
}
