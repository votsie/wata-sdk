package pro.wata.sdk.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Единая настройка Jackson для всего SDK.
 *
 * <p>Неизвестные поля ответа не роняют разбор ({@code FAIL_ON_UNKNOWN_PROPERTIES}
 * выключен) — API живой и может добавить поле, о котором SDK ещё не знает.
 * Значения enum-подобных типов SDK ({@link pro.wata.sdk.model.Currency} и т.п.)
 * никогда не бросают исключение на незнакомом значении — это гарантируется их
 * собственной фабрикой {@code of(String)}, а не настройками маппера.
 */
public final class JsonSupport {

    private static final ObjectMapper MAPPER = build();

    private JsonSupport() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    private static ObjectMapper build() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        return mapper;
    }
}
