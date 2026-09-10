package pro.wata.sdk.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Общая инфраструктура для «открытых» enum-подобных типов ({@link Currency},
 * {@link TransactionStatus} и т.д.).
 *
 * <p>Обычный Java {@code enum} не годится: API живой и может прислать
 * значение, которого SDK ещё не знает (см. раздел 6 спецификации). Обычный
 * enum в такой ситуации либо роняет разбор, либо (с
 * {@code READ_UNKNOWN_ENUM_VALUES_AS_NULL}) молча теряет исходную строку.
 * Здесь вместо этого используется класс-обёртка над {@code String}: известные
 * значения — синглтоны с говорящими именами, неизвестные оборачиваются на
 * лету с сохранением исходной строки и флагом {@code known == false}.
 */
final class EnumSupport {

    private EnumSupport() {
    }

    static <T> Map<String, T> registry() {
        return new ConcurrentHashMap<>();
    }

    static <T> T resolve(String value, Map<String, T> known, BiFunction<String, Boolean, T> factory) {
        if (value == null) {
            return null;
        }
        T existing = known.get(value);
        return existing != null ? existing : factory.apply(value, false);
    }
}
