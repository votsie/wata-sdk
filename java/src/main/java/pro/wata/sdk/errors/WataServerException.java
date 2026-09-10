package pro.wata.sdk.errors;

/** {@code 5xx}, полученный после исчерпания попыток повтора. */
public class WataServerException extends WataException {

    public WataServerException(String message, int httpStatus, String requestPath) {
        super(message, httpStatus, requestPath, null);
    }
}
