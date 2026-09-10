package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Ответ на прямой платёж картой. {@code threeDsData} присутствует, если требуется 3DS. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CardCryptoPaymentResult(
        String transactionId,
        TransactionStatus status,
        ThreeDsData threeDsData,
        String errorCode,
        String errorDescription
) {
}
