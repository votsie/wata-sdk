from __future__ import annotations

import datetime as dt
from typing import Any

from ..errors import WataConfigError
from .._http import AsyncTransport, SyncTransport

PREFIX = "/api/h2h"


def require_sync(transport: SyncTransport | None, product: str) -> SyncTransport:
    if transport is None:
        raise WataConfigError(
            f"Токен продукта '{product}' не задан в WataClient — обращение к этому продукту невозможно."
        )
    return transport


def require_async(transport: AsyncTransport | None, product: str) -> AsyncTransport:
    if transport is None:
        raise WataConfigError(
            f"Токен продукта '{product}' не задан в AsyncWataClient — обращение к этому продукту невозможно."
        )
    return transport


def validate_balance_date(value: str | dt.date) -> str:
    """Проверяет, что дата баланса — сегодня или вчера по UTC, до сетевого вызова."""

    if isinstance(value, dt.date):
        parsed = value
        raw = value.isoformat()
    else:
        raw = value
        try:
            parsed = dt.date.fromisoformat(value)
        except ValueError as exc:
            raise WataConfigError(f"Некорректный формат даты баланса: {value!r} (ожидается YYYY-MM-DD)") from exc

    today = dt.datetime.now(dt.timezone.utc).date()
    yesterday = today - dt.timedelta(days=1)
    if parsed not in (today, yesterday):
        raise WataConfigError(
            "Баланс доступен только за сегодняшнюю или вчерашнюю дату по UTC "
            f"({yesterday.isoformat()} или {today.isoformat()}), получено {raw}."
        )
    return raw


def enum_value(value: Any) -> Any:
    """Возвращает `.value` для enum, либо значение как есть (для сырых строк)."""

    return value.value if hasattr(value, "value") else value
