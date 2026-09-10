"""Общие ручки депозитного баланса и статуса заказа (`/api/v1/deposit/...`).

Путь один и тот же для Steam/Top-Up/ваучеров — сервер определяет терминал (и,
соответственно, продукт) по токену, поэтому этот модуль лишь переиспользуется
из `steam.py`/`topup.py`/`vouchers.py`, каждый раз со своим транспортом.
"""

from __future__ import annotations

from .._http import AsyncTransport, SyncTransport
from ..types import DepositBalance, DepositOrderStatus, DgObject
from ._common import PREFIX


def balance_sync(transport: SyncTransport) -> DepositBalance:
    response = transport.request("GET", f"{PREFIX}/v1/deposit/balance")
    return DepositBalance.from_api(response.json())


async def balance_async(transport: AsyncTransport) -> DepositBalance:
    response = await transport.request("GET", f"{PREFIX}/v1/deposit/balance")
    return DepositBalance.from_api(response.json())


def order_status_sync(transport: SyncTransport, order_id: str) -> DgObject:
    response = transport.request("GET", f"{PREFIX}/v1/deposit/order/{order_id}")
    return DgObject.from_api(response.json(), status_enum=DepositOrderStatus)


async def order_status_async(transport: AsyncTransport, order_id: str) -> DgObject:
    response = await transport.request("GET", f"{PREFIX}/v1/deposit/order/{order_id}")
    return DgObject.from_api(response.json(), status_enum=DepositOrderStatus)
