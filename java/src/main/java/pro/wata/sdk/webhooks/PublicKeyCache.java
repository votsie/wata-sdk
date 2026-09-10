package pro.wata.sdk.webhooks;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Кэш PEM-ключа вебхуков, привязанный к базовому адресу окружения (у боевого
 * контура и песочницы разные ключи — см. п.2 спецификации). Общий на все
 * экземпляры {@link WebhookVerifier} процесса, чтобы разные обработчики
 * вебхуков не дублировали сетевой запрос за ключом.
 */
final class PublicKeyCache {

    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
    private static final Duration TTL = Duration.ofMinutes(15);

    private PublicKeyCache() {
    }

    static String get(String baseUrl, Supplier<String> fetcher) {
        Entry entry = CACHE.get(baseUrl);
        if (entry != null && entry.expiresAt.isAfter(Instant.now())) {
            return entry.pem;
        }
        String pem = fetcher.get();
        CACHE.put(baseUrl, new Entry(pem, Instant.now().plus(TTL)));
        return pem;
    }

    static void invalidate(String baseUrl) {
        CACHE.remove(baseUrl);
    }

    private record Entry(String pem, Instant expiresAt) {
    }
}
