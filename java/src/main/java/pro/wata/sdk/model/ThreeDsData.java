package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Данные для прохождения 3DS после {@code POST /api/h2h/payments/card-crypto}.
 * {@code method} обычно {@code GET} (редирект по {@code url}) или {@code POST}
 * (автосабмит формы с {@code parameters}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ThreeDsData(String url, String method, Map<String, String> parameters) {
}
