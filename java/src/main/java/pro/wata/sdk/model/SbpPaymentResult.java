package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Ответ на оплату СБП: ссылка для перехода в приложение банка. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SbpPaymentResult(
        String transactionId,
        TransactionStatus status,
        String sbpLink,
        String errorCode,
        String errorDescription
) {
}
