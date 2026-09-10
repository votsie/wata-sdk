package pro.wata.sdk.errors;

/** Таймаут или обрыв соединения, не связанный с ответом сервера. */
public class WataNetworkException extends WataException {

    public WataNetworkException(String message, String requestPath, Throwable cause) {
        super(message, cause, null, requestPath, null);
    }
}
