"""Telegram Stars (токен терминала Stars): `/api/stars/...`.

Количество звёзд должно быть в диапазоне 50–50000. Заказы дороже порога
модерации попадают в статус `Review` и **не выполняются**, пока их явно не
подтвердят через :meth:`confirm_order` — иначе интегратор будет ждать выдачи,
которой не будет.
"""

from __future__ import annotations

from typing import Any

from .._http import AsyncTransport, SyncTransport
from ..types import DgObject, StarsOrderStatus, clean
from ._common import PREFIX, require_async, require_sync

_PRODUCT = "stars"
MIN_COUNT = 50
MAX_COUNT = 50000


def _validate_count(count: int) -> None:
    if not (MIN_COUNT <= count <= MAX_COUNT):
        raise ValueError(
            f"Количество звёзд должно быть в диапазоне {MIN_COUNT}..{MAX_COUNT}, получено {count}"
        )


def _create_body(
    *,
    username: str,
    count: int,
    amount: float,
    description: str,
    order_id: str,
    extra: dict[str, Any],
) -> dict[str, Any]:
    """Тело `POST /stars`. Имена полей — как в API, camelCase."""

    _validate_count(count)
    return clean(
        {
            "username": username,
            "count": count,
            "amount": amount,
            "description": description,
            "orderId": order_id,
            **extra,
        }
    )


class StarsAPI:
    """Синхронный доступ к Telegram Stars."""

    def __init__(self, transport: SyncTransport | None) -> None:
        self._transport = transport

    def price(self, *, username: str, **extra: Any) -> DgObject:
        """`GET /stars/price` — стоимость звезды и минимальная сумма заказа.

        Ответ содержит ``starPrice`` и ``minPrice``.
        """

        transport = require_sync(self._transport, _PRODUCT)
        params = clean({"username": username, **extra})
        response = transport.request("GET", f"{PREFIX}/stars/price", params=params)
        return DgObject.from_api(response.json())

    def create(
        self,
        *,
        username: str,
        count: int,
        amount: float,
        description: str,
        order_id: str,
        **extra: Any,
    ) -> DgObject:
        """`POST /stars` — создать заказ на покупку звёзд.

        :param username: получатель в Telegram
        :param count: количество звёзд, 50–50000
        :param amount: сумма заказа
        :param description: описание заказа
        :param order_id: ваш идентификатор заказа

        Заказ дороже порога модерации возвращается в статусе ``Review`` и
        требует явного :meth:`confirm_order` либо :meth:`reject_order`.
        В ответе приходят ``price``, ``commission`` и ``paymentLink``.
        """

        transport = require_sync(self._transport, _PRODUCT)
        body = _create_body(
            username=username,
            count=count,
            amount=amount,
            description=description,
            order_id=order_id,
            extra=extra,
        )
        response = transport.request("POST", f"{PREFIX}/stars", json_body=body, mutating=True)
        return DgObject.from_api(response.json(), status_enum=StarsOrderStatus)

    def get_order(self, order_id: str) -> DgObject:
        """`GET /stars/order/{id}`."""

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request("GET", f"{PREFIX}/stars/order/{order_id}")
        return DgObject.from_api(response.json(), status_enum=StarsOrderStatus)

    def confirm_order(self, order_id: str) -> DgObject:
        """`POST /stars/order/{id}/confirm` — подтвердить заказ в статусе `Review`."""

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request(
            "POST", f"{PREFIX}/stars/order/{order_id}/confirm", mutating=True
        )
        return DgObject.from_api(response.json(), status_enum=StarsOrderStatus)

    def reject_order(self, order_id: str) -> DgObject:
        """`POST /stars/order/{id}/reject` — отклонить заказ в статусе `Review`."""

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request(
            "POST", f"{PREFIX}/stars/order/{order_id}/reject", mutating=True
        )
        return DgObject.from_api(response.json(), status_enum=StarsOrderStatus)


class AsyncStarsAPI:
    """Асинхронный доступ к Telegram Stars — та же логика, что и :class:`StarsAPI`."""

    def __init__(self, transport: AsyncTransport | None) -> None:
        self._transport = transport

    async def price(self, *, username: str, **extra: Any) -> DgObject:
        """`GET /stars/price` — стоимость звезды и минимальная сумма заказа."""

        transport = require_async(self._transport, _PRODUCT)
        params = clean({"username": username, **extra})
        response = await transport.request("GET", f"{PREFIX}/stars/price", params=params)
        return DgObject.from_api(response.json())

    async def create(
        self,
        *,
        username: str,
        count: int,
        amount: float,
        description: str,
        order_id: str,
        **extra: Any,
    ) -> DgObject:
        """`POST /stars` — создать заказ на покупку звёзд."""

        transport = require_async(self._transport, _PRODUCT)
        body = _create_body(
            username=username,
            count=count,
            amount=amount,
            description=description,
            order_id=order_id,
            extra=extra,
        )
        response = await transport.request(
            "POST", f"{PREFIX}/stars", json_body=body, mutating=True
        )
        return DgObject.from_api(response.json(), status_enum=StarsOrderStatus)

    async def get_order(self, order_id: str) -> DgObject:
        """`GET /stars/order/{id}`."""

        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request("GET", f"{PREFIX}/stars/order/{order_id}")
        return DgObject.from_api(response.json(), status_enum=StarsOrderStatus)

    async def confirm_order(self, order_id: str) -> DgObject:
        """`POST /stars/order/{id}/confirm` — подтвердить заказ в статусе `Review`."""

        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request(
            "POST", f"{PREFIX}/stars/order/{order_id}/confirm", mutating=True
        )
        return DgObject.from_api(response.json(), status_enum=StarsOrderStatus)

    async def reject_order(self, order_id: str) -> DgObject:
        """`POST /stars/order/{id}/reject` — отклонить заказ в статусе `Review`."""

        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request(
            "POST", f"{PREFIX}/stars/order/{order_id}/reject", mutating=True
        )
        return DgObject.from_api(response.json(), status_enum=StarsOrderStatus)
