package pro.wata.sdk.http;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Экспоненциальная задержка с джиттером между повторами.
 *
 * <p>Повторяются только сетевые ошибки и ответы {@code 5xx} (см. п.3
 * спецификации). Изменяющие запросы (создание ссылки/платежа/возврата) по
 * умолчанию не ретраятся вовсе — вызывающий код передаёт
 * {@code retryable = false} для них, и тогда используется единственная попытка
 * независимо от значения {@link #maxAttempts()}.
 */
public final class RetryPolicy {

    public static final RetryPolicy DEFAULT = new RetryPolicy(3, Duration.ofMillis(250), Duration.ofSeconds(4));

    private final int maxAttempts;
    private final Duration baseDelay;
    private final Duration maxDelay;

    public RetryPolicy(int maxAttempts, Duration baseDelay, Duration maxDelay) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
    }

    /** Общее число попыток, включая первую (по умолчанию 3). */
    public int maxAttempts() {
        return maxAttempts;
    }

    /** Задержка перед попыткой номер {@code attempt} (1 — первая повторная попытка, т.е. вторая по счёту). */
    public Duration delayBeforeAttempt(int attempt) {
        long exponentialMs = baseDelay.toMillis() * (1L << Math.max(0, attempt - 1));
        long cappedMs = Math.min(exponentialMs, maxDelay.toMillis());
        long jitterMs = ThreadLocalRandom.current().nextLong(0, Math.max(1, cappedMs / 4) + 1);
        return Duration.ofMillis(cappedMs + jitterMs);
    }
}
