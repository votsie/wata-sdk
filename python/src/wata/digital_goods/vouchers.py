"""Ваучеры (токен терминала vouchers): `/api/v3/vouchers/...` (acquiring), `/api/v1/deposit/vouchers` (deposit).

Коды ваучеров возвращаются в статусе заказа (`get_order`/`deposit_order_status`)
— отдельного эндпоинта выдачи нет — и могут появиться с задержкой до 10 минут.

SPEC.md не перечисляет точный список полей тела запроса — `create`/`deposit_create`
принимают их как есть (именованные аргументы в camelCase, как ожидает WATA API).
"""

from __future__ import annotations

from typing import Any

from .._http import AsyncTransport, SyncTransport
from ..types import DepositBalance, DepositOrderStatus, DgObject, DgOrderStatus, clean
from . import deposit as _deposit
from ._common import PREFIX, require_async, require_sync

_PRODUCT = "vouchers"


def _create_body(
    *,
    voucher_id: str,
    amount: float,
    count: int,
    order_id: str,
    email: str,
    description: str,
    extra: dict[str, Any],
) -> dict[str, Any]:
    """Тело `POST /v3/vouchers`. Имена полей — как в API, camelCase."""

    return clean(
        {
            "voucherId": voucher_id,
            "amount": amount,
            "count": count,
            "orderId": order_id,
            "email": email,
            "description": description,
            **extra,
        }
    )


def _deposit_body(
    *,
    voucher_id: str,
    category_id: str,
    count: int,
    order_id: str,
    email: str,
    extra: dict[str, Any],
) -> dict[str, Any]:
    """Тело `POST /v1/deposit/vouchers`. В ответе приходит `codes[]`."""

    return clean(
        {
            "voucherId": voucher_id,
            "categoryId": category_id,
            "count": count,
            "orderId": order_id,
            "email": email,
            **extra,
        }
    )


class VouchersAPI:
    """Синхронный доступ к ваучерам."""

    def __init__(self, transport: SyncTransport | None) -> None:
        self._transport = transport

    def list_all(self, **params: Any) -> DgObject:
        """`GET /v3/vouchers/all`."""

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request("GET", f"{PREFIX}/v3/vouchers/all", params=clean(params))
        return DgObject.from_api(response.json())

    def create(
        self,
        *,
        voucher_id: str,
        amount: float,
        count: int,
        order_id: str,
        email: str,
        description: str,
        **extra: Any,
    ) -> DgObject:
        """`POST /v3/vouchers` — покупка ваучеров, оплачивает покупатель.

        Коды приходят в статусе заказа (`get_order`), с задержкой до 10 минут.
        """

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request(
            "POST",
            f"{PREFIX}/v3/vouchers",
            json_body=_create_body(
                voucher_id=voucher_id, amount=amount, count=count,
                order_id=order_id, email=email, description=description, extra=extra,
            ),
            mutating=True,
        )
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    def get_order(self, order_id: str) -> DgObject:
        """`GET /v3/vouchers/order/{id}` — код ваучера появляется здесь (может занять до 10 минут)."""

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request("GET", f"{PREFIX}/v3/vouchers/order/{order_id}")
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    def deposit_list(self, **params: Any) -> DgObject:
        """`GET /v1/deposit/vouchers`."""

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request("GET", f"{PREFIX}/v1/deposit/vouchers", params=clean(params))
        return DgObject.from_api(response.json())

    def deposit_create(
        self,
        *,
        voucher_id: str,
        category_id: str,
        count: int,
        order_id: str,
        email: str,
        **extra: Any,
    ) -> DgObject:
        """`POST /v1/deposit/vouchers` — списание с депозита; в ответе `codes[]`."""

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request(
            "POST",
            f"{PREFIX}/v1/deposit/vouchers",
            json_body=_deposit_body(
                voucher_id=voucher_id, category_id=category_id, count=count,
                order_id=order_id, email=email, extra=extra,
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


class AsyncVouchersAPI:
    """Асинхронный доступ к ваучерам — та же логика, что и :class:`VouchersAPI`."""

    def __init__(self, transport: AsyncTransport | None) -> None:
        self._transport = transport

    async def list_all(self, **params: Any) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request("GET", f"{PREFIX}/v3/vouchers/all", params=clean(params))
        return DgObject.from_api(response.json())

    async def create(
        self,
        *,
        voucher_id: str,
        amount: float,
        count: int,
        order_id: str,
        email: str,
        description: str,
        **extra: Any,
    ) -> DgObject:
        """`POST /v3/vouchers` — покупка ваучеров, оплачивает покупатель."""

        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request(
            "POST",
            f"{PREFIX}/v3/vouchers",
            json_body=_create_body(
                voucher_id=voucher_id, amount=amount, count=count,
                order_id=order_id, email=email, description=description, extra=extra,
            ),
            mutating=True,
        )
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    async def get_order(self, order_id: str) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request("GET", f"{PREFIX}/v3/vouchers/order/{order_id}")
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    async def deposit_list(self, **params: Any) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request("GET", f"{PREFIX}/v1/deposit/vouchers", params=clean(params))
        return DgObject.from_api(response.json())

    async def deposit_create(
        self,
        *,
        voucher_id: str,
        category_id: str,
        count: int,
        order_id: str,
        email: str,
        **extra: Any,
    ) -> DgObject:
        """`POST /v1/deposit/vouchers` — списание с депозита; в ответе `codes[]`."""

        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request(
            "POST",
            f"{PREFIX}/v1/deposit/vouchers",
            json_body=_deposit_body(
                voucher_id=voucher_id, category_id=category_id, count=count,
                order_id=order_id, email=email, extra=extra,
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
