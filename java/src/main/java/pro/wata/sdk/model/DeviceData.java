package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Данные устройства плательщика, обязательные для прямых платежей
 * ({@code card-crypto}, {@code sbp}, {@code tpay}). Точный набор полей не
 * зафиксирован единым перечнем в контракте эквайринга WATA и может расширяться,
 * поэтому класс хранит поля как карту "ключ-значение" вместо жёсткой схемы:
 * это не позволяет новому полю банка/браузера сломать сборку.
 *
 * <p>Собирайте объект через {@link #builder()}, задавая любые поля, которые
 * требует ваш чекаут (обычно это данные фингерпринта браузера: user agent,
 * часовой пояс, разрешение экрана и т.п.).
 */
public final class DeviceData {

    private final Map<String, Object> fields;

    private DeviceData(Map<String, Object> fields) {
        this.fields = fields;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonValue
    public Map<String, Object> asMap() {
        return fields;
    }

    public static final class Builder {
        private final Map<String, Object> fields = new LinkedHashMap<>();

        public Builder put(String key, Object value) {
            fields.put(key, value);
            return this;
        }

        public Builder userAgent(String userAgent) {
            return put("userAgent", userAgent);
        }

        public Builder ip(String ip) {
            return put("ip", ip);
        }

        public Builder acceptHeader(String acceptHeader) {
            return put("acceptHeader", acceptHeader);
        }

        public Builder colorDepth(int colorDepth) {
            return put("colorDepth", colorDepth);
        }

        public Builder screenHeight(int screenHeight) {
            return put("screenHeight", screenHeight);
        }

        public Builder screenWidth(int screenWidth) {
            return put("screenWidth", screenWidth);
        }

        public Builder timeZoneOffset(int timeZoneOffset) {
            return put("timeZoneOffset", timeZoneOffset);
        }

        public Builder language(String language) {
            return put("language", language);
        }

        public Builder javaEnabled(boolean javaEnabled) {
            return put("javaEnabled", javaEnabled);
        }

        public DeviceData build() {
            return new DeviceData(new LinkedHashMap<>(fields));
        }
    }
}
