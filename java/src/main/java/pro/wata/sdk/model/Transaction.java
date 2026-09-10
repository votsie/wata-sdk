package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

/**
 * Транзакция. Обратите внимание: здесь комиссия называется
 * {@code totalCommission}, тогда как в вебхуке то же значение приходит как
 * {@code commission} (см. {@link pro.wata.sdk.model.WebhookEvent#commission()}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Transaction(
        String id,
        TransactionType transactionType,
        TransactionKind kind,
        TransactionStatus status,
        Double amount,
        Currency currency,
        Double totalCommission,
        String orderId,
        String orderDescription,
        String paymentLinkId,
        String originalTransactionId,
        String errorCode,
        String errorDescription,
        OffsetDateTime creationTime,
        OffsetDateTime paymentTime
) {
}
