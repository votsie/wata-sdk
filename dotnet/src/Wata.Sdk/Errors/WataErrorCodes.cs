namespace Wata.Sdk.Errors;

/// <summary>
/// Семейство, к которому относится код ошибки WATA.
/// </summary>
public enum WataErrorFamily
{
    Unknown,
    PaymentLink,
    Crypto,
    Transaction,
    Refund,
    Order,
    Steam,
    Stars,
    TopUp,
    Voucher,
}

/// <summary>
/// Известные коды ошибок WATA из поля <c>error.code</c>.
/// </summary>
/// <remarks>
/// Список неполный по своей природе: платформа может добавить код без изменения
/// версии SDK. Поэтому это справочник констант, а не перечисление —
/// <see cref="WataApiException.Code"/> всегда содержит исходную строку,
/// даже незнакомую.
/// </remarks>
public static class WataErrorCodes
{
    // Платёжные ссылки.
    public const string LinkNotFound = "PL_1001";
    public const string LinkInvalid = "PL_1002";
    public const string LinkExpired = "PL_1003";

    // Шифрование карточных данных.
    public const string CryptoInvalid = "CRY_1001";

    // Возвраты. Транзакции: TRA_1001..TRA_1019 — валидация,
    // TRA_2001..TRA_2999 — отказы шлюза и эмитента.
    public const string RefundInvalidAmount = "TRA_1101";
    public const string RefundInsufficientFunds = "TRA_1102";
    public const string RefundPendingExists = "TRA_1103";

    /// <summary>
    /// Определяет семейство ошибки по префиксу кода. Позволяет обработать целую
    /// группу отказов, не перечисляя каждый код по отдельности.
    /// </summary>
    public static WataErrorFamily FamilyOf(string? code) => code switch
    {
        null or "" => WataErrorFamily.Unknown,
        _ when code.StartsWith("PL_", StringComparison.Ordinal) => WataErrorFamily.PaymentLink,
        _ when code.StartsWith("CRY_", StringComparison.Ordinal) => WataErrorFamily.Crypto,
        _ when code.StartsWith("TRA_11", StringComparison.Ordinal) => WataErrorFamily.Refund,
        _ when code.StartsWith("TRA_", StringComparison.Ordinal) => WataErrorFamily.Transaction,
        _ when code.StartsWith("ORD_", StringComparison.Ordinal) => WataErrorFamily.Order,
        _ when code.StartsWith("STM_", StringComparison.Ordinal) => WataErrorFamily.Steam,
        _ when code.StartsWith("STR_", StringComparison.Ordinal) => WataErrorFamily.Stars,
        _ when code.StartsWith("TPP_", StringComparison.Ordinal) => WataErrorFamily.TopUp,
        _ when code.StartsWith("VCR_", StringComparison.Ordinal) => WataErrorFamily.Voucher,
        _ => WataErrorFamily.Unknown,
    };
}
