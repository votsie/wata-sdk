package pro.wata.sdk.digitalgoods.deposit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Баланс депозита мерчанта ({@code GET /v1/deposit/balance}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DepositBalance(double totalBalance, double frozenBalance, double availableBalance) {
}
