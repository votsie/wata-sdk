package pro.wata.sdk.errors;

import java.util.List;
import java.util.Map;

/**
 * Ошибка {@code 4xx} (кроме 401/403/429) с телом
 * {@code {error: {code, message, details, validationErrors}}}. Несёт код
 * ошибки WATA ({@code PL_*}, {@code TRA_*}, {@code ORD_*}, {@code STM_*},
 * {@code STR_*}, {@code TPP_*}, {@code VCR_*}), сообщение и HTTP-статус.
 */
public class WataApiException extends WataException {

    private final Object details;
    private final List<Map<String, Object>> validationErrors;

    public WataApiException(String message, int httpStatus, String requestPath, String wataErrorCode,
                             Object details, List<Map<String, Object>> validationErrors) {
        super(message, httpStatus, requestPath, wataErrorCode);
        this.details = details;
        this.validationErrors = validationErrors;
    }

    /** Дополнительные детали ошибки, если сервер их прислал. */
    public Object details() {
        return details;
    }

    /** Ошибки валидации отдельных полей запроса, если применимо. */
    public List<Map<String, Object>> validationErrors() {
        return validationErrors;
    }
}
