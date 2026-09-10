package pro.wata.sdk.digitalgoods;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Базовый класс для ответов цифровых товаров, чья схема специфицирована не
 * полностью (см. раздел 5 SPEC.md — контракт называет пути и по паре ключевых
 * полей на эндпоинт, но не весь JSON целиком).
 *
 * <p>Вместо того чтобы гадать имена остальных полей, неизвестные ключи
 * складываются в {@link #extra()} как есть. Так новое поле, добавленное живым
 * API, не теряется и не требует ждать обновления SDK, чтобы до него добраться —
 * в отличие от жёсткой схемы, которая такие поля обычно молча роняет.
 */
public abstract class DgObject {

    private final Map<String, Object> extra = new LinkedHashMap<>();

    @JsonAnySetter
    void setExtra(String name, Object value) {
        extra.put(name, value);
    }

    /** Поля ответа, не описанные типом явно, в порядке получения от сервера. */
    @JsonAnyGetter
    public Map<String, Object> extra() {
        return extra;
    }
}
