package pro.wata.sdk.model;

/**
 * Запрос на возврат ({@code POST /api/h2h/transactions/refunds}). Валюта
 * берётся из исходной транзакции — отдельного поля для неё нет, как и поля
 * «причина возврата».
 *
 * @param originalTransactionId идентификатор исходной транзакции (uuid), должна быть в статусе {@code Paid}
 * @param amount                сумма возврата: больше нуля, не больше доступного остатка, не более двух знаков после запятой
 */
public record RefundRequest(String originalTransactionId, double amount) {
}
