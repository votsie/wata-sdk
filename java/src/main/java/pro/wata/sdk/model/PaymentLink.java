package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.List;

/** Ответ на создание/получение платёжной ссылки. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentLink(
        String id,
        Double amount,
        Currency currency,
        PaymentLinkStatus status,
        String url,
        String terminalName,
        String terminalPublicId,
        OffsetDateTime creationTime,
        PaymentLinkType type,
        String orderId,
        OffsetDateTime expirationDateTime,
        Boolean isArbitraryAmountAllowed,
        List<Double> arbitraryAmounts
) {
}
