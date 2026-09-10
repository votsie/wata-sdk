package pro.wata.sdk.acquiring;

import pro.wata.sdk.http.HttpTransport;
import pro.wata.sdk.model.CardCryptoPaymentRequest;
import pro.wata.sdk.model.CardCryptoPaymentResult;
import pro.wata.sdk.model.SbpPaymentRequest;
import pro.wata.sdk.model.SbpPaymentResult;
import pro.wata.sdk.model.TPayPaymentRequest;
import pro.wata.sdk.model.TPayPaymentResult;

/**
 * Прямые платежи: {@code /api/h2h/payments/card-crypto}, {@code /sbp}, {@code /tpay}.
 * Все три — изменяющие запросы и по умолчанию не повторяются при сбое.
 */
public final class PaymentsClient {

    private final HttpTransport transport;

    PaymentsClient(HttpTransport transport) {
        this.transport = transport;
    }

    /**
     * Оплата картой по криптограмме. Криптограмму формирует клиентский скрипт
     * чекаута в браузере плательщика — сервер мерчанта её не собирает.
     * Возможен {@code threeDsData} в ответе — редирект или автосабмит формы 3DS.
     */
    public CardCryptoPaymentResult cardCrypto(CardCryptoPaymentRequest request) {
        return transport.post("/payments/card-crypto", request, CardCryptoPaymentResult.class, false);
    }

    /** Оплата СБП. Валюты нет — только рубли. */
    public SbpPaymentResult sbp(SbpPaymentRequest request) {
        return transport.post("/payments/sbp", request, SbpPaymentResult.class, false);
    }

    /** Оплата T-Pay. */
    public TPayPaymentResult tpay(TPayPaymentRequest request) {
        return transport.post("/payments/tpay", request, TPayPaymentResult.class, false);
    }
}
