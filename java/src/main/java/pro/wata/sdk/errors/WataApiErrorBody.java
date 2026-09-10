package pro.wata.sdk.errors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Тело ошибки WATA: {@code {"error": {"code", "message", "details", "validationErrors"}}}.
 *
 * @param code             код ошибки WATA, например {@code PL_NOT_FOUND}, {@code TRA_INSUFFICIENT_FUNDS}
 * @param message          человекочитаемое сообщение
 * @param details          дополнительные детали, произвольная структура
 * @param validationErrors ошибки валидации полей запроса, если применимо
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WataApiErrorBody(
        String code,
        String message,
        Object details,
        @JsonProperty("validationErrors") List<Map<String, Object>> validationErrors
) {

    /** Обёртка верхнего уровня {@code {"error": {...}}}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope(WataApiErrorBody error) {
    }
}
