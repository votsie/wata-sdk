package pro.wata.sdk.errors;

/**
 * {@code 401}/{@code 403}: токен истёк, отозван, принадлежит другому терминалу,
 * либо запрос пришёл с несогласованного IP.
 */
public class WataAuthException extends WataException {

    public WataAuthException(String message, int httpStatus, String requestPath) {
        super(message, httpStatus, requestPath, null);
    }
}
