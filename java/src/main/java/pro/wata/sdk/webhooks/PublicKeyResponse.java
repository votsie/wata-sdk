package pro.wata.sdk.webhooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Ответ {@code GET /api/h2h/public-key}: {@code {"value": "-----BEGIN PUBLIC KEY-----..."}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
record PublicKeyResponse(String value) {
}
