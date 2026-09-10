package pro.wata.sdk.http;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;

/** Собирает query-строку, пропуская {@code null} и пустые значения (см. п.3 спецификации). */
public final class QueryStrings {

    private QueryStrings() {
    }

    public static String encode(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item == null) {
                        continue;
                    }
                    appendPair(sb, entry.getKey(), item);
                }
            } else {
                appendPair(sb, entry.getKey(), value);
            }
        }
        return sb.isEmpty() ? "" : "?" + sb;
    }

    private static void appendPair(StringBuilder sb, String key, Object value) {
        if (!sb.isEmpty()) {
            sb.append('&');
        }
        sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
        sb.append('=');
        sb.append(URLEncoder.encode(toWireValue(value), StandardCharsets.UTF_8));
    }

    private static String toWireValue(Object value) {
        if (value instanceof Temporal) {
            return value.toString();
        }
        return String.valueOf(value);
    }
}
