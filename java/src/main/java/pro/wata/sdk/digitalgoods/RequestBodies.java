package pro.wata.sdk.digitalgoods;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Собирает тело запроса из полей, названных в SPEC.md явно ({@code known}), и
 * произвольных дополнительных полей ({@code extra}), которые контракт не
 * перечисляет (например идентификатор аккаунта Steam или получателя Stars).
 * При конфликте ключей {@code extra} побеждает — это осознанный выбор вызывающей
 * стороны, а не подсказка SDK.
 */
public final class RequestBodies {

    private RequestBodies() {
    }

    public static Map<String, Object> merge(Map<String, Object> known, Map<String, Object> extra) {
        Map<String, Object> body = new LinkedHashMap<>(known);
        if (extra != null) {
            body.putAll(extra);
        }
        return body;
    }
}
