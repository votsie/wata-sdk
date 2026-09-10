from __future__ import annotations

from ..errors import WataConfigError
from .._http import AsyncTransport, SyncTransport

PREFIX = "/api"


def require_sync(transport: SyncTransport | None, product: str) -> SyncTransport:
    if transport is None:
        raise WataConfigError(
            f"Токен продукта '{product}' не задан в WataClient — обращение к этому продукту невозможно. "
            "Напоминание: Stars и Steam всегда живут на отдельных терминалах и требуют собственных токенов."
        )
    return transport


def require_async(transport: AsyncTransport | None, product: str) -> AsyncTransport:
    if transport is None:
        raise WataConfigError(
            f"Токен продукта '{product}' не задан в AsyncWataClient — обращение к этому продукту невозможно. "
            "Напоминание: Stars и Steam всегда живут на отдельных терминалах и требуют собственных токенов."
        )
    return transport
