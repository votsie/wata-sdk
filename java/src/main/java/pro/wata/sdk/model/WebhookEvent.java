package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

/**
 * Разобранное событие вебхука эквайринга.
 *
 * <p>Обратите внимание: комиссия здесь называется {@code commission}, тогда
 * как в ответе {@code GET /transactions/{id}} то же значение называется
 * {@code totalCommission} (см. {@link Transaction#totalCommission()}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookEvent(
        TransactionType transactionType,
        TransactionKind kind,
        String id,
        String transactionId,
        String originalTransactionId,
        String terminalPublicId,
        TransactionStatus transactionStatus,
        String errorCode,
        String errorDescription,
        String terminalName,
        Double amount,
        Currency currency,
        String orderId,
        String orderDescription,
        Double commission,
        OffsetDateTime paymentTime,
        String email,
        String paymentLinkId,
        PayerData payerData
) {
}
