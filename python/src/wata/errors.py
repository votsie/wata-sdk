"""Иерархия ошибок WATA SDK.

Базовый класс — :class:`WataError`. Каждая ошибка несёт HTTP-статус (если
применим), путь запроса и код ошибки WATA (если сервер его вернул). Секреты
(токен, тело запроса с картой) никогда не попадают в текст ошибки.
"""

from __future__ import annotations

from typing import Any


class WataError(Exception):
    """Базовая ошибка SDK."""

    def __init__(
        self,
        message: str,
        *,
        http_status: int | None = None,
        path: str | None = None,
        wata_code: str | None = None,
    ) -> None:
        super().__init__(message)
        self.message = message
        self.http_status = http_status
        self.path = path
        self.wata_code = wata_code

    def __str__(self) -> str:  # pragma: no cover - тривиально
        parts = [self.message]
        if self.http_status is not None:
            parts.append(f"http_status={self.http_status}")
        if self.wata_code is not None:
            parts.append(f"wata_code={self.wata_code}")
        if self.path is not None:
            parts.append(f"path={self.path}")
        return " ".join(parts)


class WataConfigError(WataError):
    """Ошибка конфигурации SDK: нет токена нужного продукта, неверное
    окружение, недопустимая дата баланса. Всегда возникает до сетевого
    вызова."""


class WataAuthError(WataError):
    """401/403: токен истёк, отозван, принадлежит другому терминалу либо
    запрос идёт с несогласованного IP."""


class WataRateLimitError(WataError):
    """429: превышен лимит запросов. `retry_after` — число секунд до
    повторной попытки, если сервер его указал."""

    def __init__(
        self,
        message: str,
        *,
        http_status: int | None = None,
        path: str | None = None,
        wata_code: str | None = None,
        retry_after: float | None = None,
    ) -> None:
        super().__init__(message, http_status=http_status, path=path, wata_code=wata_code)
        self.retry_after = retry_after


class WataApiError(WataError):
    """4xx с телом `{error: {code, message, details, validationErrors}}`."""

    def __init__(
        self,
        message: str,
        *,
        http_status: int | None = None,
        path: str | None = None,
        wata_code: str | None = None,
        details: Any = None,
        validation_errors: Any = None,
    ) -> None:
        super().__init__(message, http_status=http_status, path=path, wata_code=wata_code)
        self.details = details
        self.validation_errors = validation_errors


class WataServerError(WataError):
    """5xx после исчерпания попыток повтора."""


class WataNetworkError(WataError):
    """Таймаут, обрыв соединения и прочие сетевые сбои транспорта."""


class WataWebhookError(WataError):
    """Подпись вебхука не сошлась либо не удалось получить публичный ключ."""


# --- Коды ошибок WATA -------------------------------------------------------
#
# Список неполный по своей природе: платформа может добавить код без изменения
# версии SDK. Поэтому это справочник известных значений, а не Enum —
# ``WataApiError.wata_code`` всегда содержит исходную строку, даже незнакомую.

# Платёжные ссылки
LINK_NOT_FOUND = "PL_1001"
LINK_INVALID = "PL_1002"
LINK_EXPIRED = "PL_1003"

# Шифрование карточных данных
CRYPTO_INVALID = "CRY_1001"

# Возвраты (транзакции: TRA_1001..TRA_1019 — валидация, TRA_2001..TRA_2999 — отказы шлюза)
REFUND_INVALID_AMOUNT = "TRA_1101"
REFUND_INSUFFICIENT_FUNDS = "TRA_1102"
REFUND_PENDING_EXISTS = "TRA_1103"


def error_family(code: str | None) -> str:
    """Семейство ошибки по префиксу кода.

    Позволяет обработать целую группу отказов, не перечисляя каждый код:
    ``payment-link``, ``crypto``, ``transaction``, ``refund``, ``order``,
    ``steam``, ``stars``, ``topup``, ``voucher`` либо ``unknown``.
    """

    if not code:
        return "unknown"
    if code.startswith("PL_"):
        return "payment-link"
    if code.startswith("CRY_"):
        return "crypto"
    if code.startswith("TRA_11"):
        return "refund"
    if code.startswith("TRA_"):
        return "transaction"
    if code.startswith("ORD_"):
        return "order"
    if code.startswith("STM_"):
        return "steam"
    if code.startswith("STR_"):
        return "stars"
    if code.startswith("TPP_"):
        return "topup"
    if code.startswith("VCR_"):
        return "voucher"
    return "unknown"
