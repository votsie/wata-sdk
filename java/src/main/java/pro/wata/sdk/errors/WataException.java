package pro.wata.sdk.errors;

/**
 * Базовое исключение WATA SDK. Все остальные исключения SDK наследуют от него,
 * так что вызывающий код может ловить {@code WataException}, если ему не важна
 * конкретная причина сбоя.
 *
 * <p>Исключение непроверяемое ({@link RuntimeException}) — это соответствует
 * идиоме Java для ошибок HTTP-клиентов (см. {@code java.net.http}) и не
 * заставляет вызывающий код оборачивать каждый вызов в {@code try/catch}.
 *
 * <p>Ни одно исключение SDK никогда не включает в сообщение токен авторизации
 * или тело запроса с данными карты.
 */
public class WataException extends RuntimeException {

    private final Integer httpStatus;
    private final String requestPath;
    private final String wataErrorCode;

    public WataException(String message) {
        this(message, null, null, null, null);
    }

    public WataException(String message, Throwable cause) {
        this(message, cause, null, null, null);
    }

    public WataException(String message, Integer httpStatus, String requestPath, String wataErrorCode) {
        this(message, null, httpStatus, requestPath, wataErrorCode);
    }

    public WataException(String message, Throwable cause, Integer httpStatus, String requestPath, String wataErrorCode) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.requestPath = requestPath;
        this.wataErrorCode = wataErrorCode;
    }

    /** HTTP-статус ответа, если исключение возникло после сетевого вызова. */
    public Integer httpStatus() {
        return httpStatus;
    }

    /** Путь запроса, на котором произошла ошибка, если применимо. */
    public String requestPath() {
        return requestPath;
    }

    /** Код ошибки WATA (например {@code PL_NOT_FOUND}), если сервер его вернул. */
    public String wataErrorCode() {
        return wataErrorCode;
    }
}
