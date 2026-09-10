package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Ответ на оплату T-Pay: ссылка для перехода в приложение банка. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TPayPaymentResult(
        String transactionId,
        TransactionStatus status,
        String tPayLink,
        String errorCode,
        String errorDescription
) {
}
