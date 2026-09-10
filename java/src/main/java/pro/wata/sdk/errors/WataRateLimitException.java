package pro.wata.sdk.errors;

import java.time.Duration;

/**
 * {@code 429}. Часть GET-эндпоинтов ограничена примерно одним запросом за
 * 30 секунд на объект. SDK не ретраит {@code 429} автоматически — вызывающий
 * код должен сам решить, ждать ли {@link #retryAfter()}.
 */
public class WataRateLimitException extends WataException {

    private final Duration retryAfter;

    public WataRateLimitException(String message, String requestPath, Duration retryAfter) {
        super(message, 429, requestPath, null);
        this.retryAfter = retryAfter;
    }

    /**
     * Через сколько можно повторить запрос, если сервер прислал заголовок
     * {@code Retry-After}. {@code null}, если сервер не сообщил интервал.
     */
    public Duration retryAfter() {
        return retryAfter;
    }
}
