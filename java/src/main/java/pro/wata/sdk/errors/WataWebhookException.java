package pro.wata.sdk.errors;

/** Подпись вебхука не сошлась, либо не удалось получить публичный ключ. */
public class WataWebhookException extends WataException {

    public WataWebhookException(String message) {
        super(message);
    }

    public WataWebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
