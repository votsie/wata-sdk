package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

/** Баланс терминала на конкретную дату ({@code GET /api/h2h/finance/balance}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Balance(String terminalPublicId, LocalDate date, double balance, Currency currency) {
}
