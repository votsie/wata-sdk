"""Top-Up (токен терминала Top-Up): `/api/v3/topup/...` (acquiring), `/api/v1/deposit/topups` (deposit).

Поля запроса типизированы по спецификации. `fields` — набор значений, которые
требует конкретная позиция каталога (например идентификатор игрового аккаунта);
их состав описан в `fields[]` ответа каталога, поэтому это словарь.
"""

from __future__ import annotations

from typing import Any

from .._http import AsyncTransport, SyncTransport
from ..types import DepositBalance, DepositOrderStatus, DgObject, DgOrderStatus, clean
from . import deposit as _deposit
from ._common import PREFIX, require_async, require_sync

_PRODUCT = "topup"


def _create_body(
    *,
    topup_id: str,
    amount: float,
    order_id: str,
    email: str,
    description: str,
    fields: dict[str, Any] | None,
    extra: dict[str, Any],
) -> dict[str, Any]:
    """Тело `POST /v3/topup`. Имена полей — как в API, camelCase."""

    return clean(
        {
            "topupId": topup_id,
            "amount": amount,
            "orderId": order_id,
            "email": email,
            "description": description,
            "fields": fields,
            **extra,
        }
    )


def _deposit_body(
    *,
    topup_id: str,
    category_id: str,
    order_id: str,
    email: str,
    fields: dict[str, Any] | None,
    extra: dict[str, Any],
) -> dict[str, Any]:
    """Тело `POST /v1/deposit/topups`."""

    return clean(
        {
            "topupId": topup_id,
            "categoryId": category_id,
            "orderId": order_id,
            "email": email,
            "fields": fields,
            **extra,
        }
    )


class TopupAPI:
    """Синхронный доступ к Top-Up."""

    def __init__(self, transport: SyncTransport | None) -> None:
        self._transport = transport

    def list_all(self, **params: Any) -> DgObject:
        """`GET /v3/topup/all`."""

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request("GET", f"{PREFIX}/v3/topup/all", params=clean(params))
        return DgObject.from_api(response.json())

    def create(
        self,
        *,
        topup_id: str,
        amount: float,
        order_id: str,
        email: str,
        description: str,
        fields: dict[str, Any] | None = None,
        **extra: Any,
    ) -> DgObject:
        """`POST /v3/topup` — покупка позиции, оплачивает покупатель.

        В ответе приходят ``commission``, ``orderPrice`` и ``paymentLink``.
        """

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request(
            "POST",
            f"{PREFIX}/v3/topup",
            json_body=_create_body(
                topup_id=topup_id,
                amount=amount,
                order_id=order_id,
                email=email,
                description=description,
                fields=fields,
                extra=extra,
            ),
            mutating=True,
        )
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    def get_order(self, order_id: str) -> DgObject:
        """`GET /v3/topup/orders/{id}`."""

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request("GET", f"{PREFIX}/v3/topup/orders/{order_id}")
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    def deposit_list(self, **params: Any) -> DgObject:
        """`GET /v1/deposit/topups`."""

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request("GET", f"{PREFIX}/v1/deposit/topups", params=clean(params))
        return DgObject.from_api(response.json())

    def deposit_create(
        self,
        *,
        topup_id: str,
        category_id: str,
        order_id: str,
        email: str,
        fields: dict[str, Any] | None = None,
        **extra: Any,
    ) -> DgObject:
        """`POST /v1/deposit/topups` — списание с депозита мерчанта."""

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request(
            "POST",
            f"{PREFIX}/v1/deposit/topups",
            json_body=_deposit_body(
                topup_id=topup_id,
                category_id=category_id,
                order_id=order_id,
                email=email,
                fields=fields,
                extra=extra,
            ),
            mutating=True,
        )
        return DgObject.from_api(response.json(), status_enum=DepositOrderStatus)

    def deposit_order_status(self, order_id: str) -> DgObject:
        """`GET /v1/deposit/order/{orderId}`."""

        transport = require_sync(self._transport, _PRODUCT)
        return _deposit.order_status_sync(transport, order_id)

    def deposit_balance(self) -> DepositBalance:
        """`GET /v1/deposit/balance`."""

        transport = require_sync(self._transport, _PRODUCT)
        return _deposit.balance_sync(transport)


class AsyncTopupAPI:
    """Асинхронный доступ к Top-Up — та же логика, что и :class:`TopupAPI`."""

    def __init__(self, transport: AsyncTransport | None) -> None:
        self._transport = transport

    async def list_all(self, **params: Any) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request("GET", f"{PREFIX}/v3/topup/all", params=clean(params))
        return DgObject.from_api(response.json())

    async def create(
        self,
        *,
        topup_id: str,
        amount: float,
        order_id: str,
        email: str,
        description: str,
        fields: dict[str, Any] | None = None,
        **extra: Any,
    ) -> DgObject:
        """`POST /v3/topup` — покупка позиции, оплачивает покупатель."""

        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request(
            "POST",
            f"{PREFIX}/v3/topup",
            json_body=_create_body(
                topup_id=topup_id,
                amount=amount,
                order_id=order_id,
                email=email,
                description=description,
                fields=fields,
                extra=extra,
            ),
            mutating=True,
        )
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    async def get_order(self, order_id: str) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request("GET", f"{PREFIX}/v3/topup/orders/{order_id}")
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    async def deposit_list(self, **params: Any) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request("GET", f"{PREFIX}/v1/deposit/topups", params=clean(params))
        return DgObject.from_api(response.json())

    async def deposit_create(
        self,
        *,
        topup_id: str,
        category_id: str,
        order_id: str,
        email: str,
        fields: dict[str, Any] | None = None,
        **extra: Any,
    ) -> DgObject:
        """`POST /v1/deposit/topups` — списание с депозита мерчанта."""

        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request(
            "POST",
            f"{PREFIX}/v1/deposit/topups",
            json_body=_deposit_body(
                topup_id=topup_id,
                category_id=category_id,
                order_id=order_id,
                email=email,
                fields=fields,
                extra=extra,
            ),
            mutating=True,
        )
        return DgObject.from_api(response.json(), status_enum=DepositOrderStatus)

    async def deposit_order_status(self, order_id: str) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        return await _deposit.order_status_async(transport, order_id)

    async def deposit_balance(self) -> DepositBalance:
        transport = require_async(self._transport, _PRODUCT)
        return await _deposit.balance_async(transport)
