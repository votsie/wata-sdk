package pro.wata.sdk.acquiring;

import pro.wata.sdk.errors.WataConfigException;
import pro.wata.sdk.http.HttpTransport;
import pro.wata.sdk.model.Balance;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Баланс терминала: {@code GET /api/h2h/finance/balance}.
 *
 * <p>Допустимы только сегодняшняя и вчерашняя дата по UTC — это проверяется
 * локально, до сетевого вызова, как того требует спецификация.
 */
public final class BalanceClient {

    private final HttpTransport transport;
    private final Clock clock;

    BalanceClient(HttpTransport transport) {
        this(transport, Clock.systemUTC());
    }

    /** Для тестов: позволяет подставить фиксированные "текущие сутки" по UTC. */
    BalanceClient(HttpTransport transport, Clock clock) {
        this.transport = transport;
        this.clock = clock;
    }

    public Balance get(LocalDate date) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        LocalDate yesterday = today.minusDays(1);
        if (!date.equals(today) && !date.equals(yesterday)) {
            throw new WataConfigException(
                    "Balance date must be today (" + today + ") or yesterday (" + yesterday + ") in UTC, got " + date);
        }
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("Date", date);
        return transport.get("/finance/balance", query, Balance.class);
    }
}
