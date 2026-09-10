"""Steam (токен терминала Steam): `/api/v3/steam/...` (acquiring), `/api/v1/steam/deposit/...` (deposit).

Два сценария оплаты различаются тем, что задано:
- `net_amount` — сумма зачисления на Steam-аккаунт (сколько получит игрок);
- `amount` / `price` — сумма платежа (сколько спишется у плательщика).

Комиссия и курс делают эти величины разными — SDK называет их явно, а не
общим словом «сумма».
"""

from __future__ import annotations

from typing import Any

from .._http import AsyncTransport, SyncTransport
from ..types import DepositBalance, DepositOrderStatus, DgObject, DgOrderStatus, clean
from . import deposit as _deposit
from ._common import PREFIX, require_async, require_sync

_PRODUCT = "steam"


class SteamAPI:
    """Синхронный доступ к Steam."""

    def __init__(self, transport: SyncTransport | None) -> None:
        self._transport = transport

    # ---- оплата покупателем (acquiring) -------------------------------

    def quote_by_net_amount(self, *, account: str, net_amount: float, **extra: Any) -> DgObject:
        """`GET /v3/steam/amount` — сколько нужно заплатить, чтобы зачислить `net_amount`."""

        transport = require_sync(self._transport, _PRODUCT)
        params = clean({"account": account, "netAmount": net_amount, **extra})
        response = transport.request("GET", f"{PREFIX}/v3/steam/amount", params=params)
        return DgObject.from_api(response.json())

    def quote_by_amount(self, *, account: str, amount: float, margin: float | None = None, **extra: Any) -> DgObject:
        """`GET /v3/steam/by-amount` — сколько будет зачислено при платеже `amount`."""

        transport = require_sync(self._transport, _PRODUCT)
        params = clean({"account": account, "amount": amount, "margin": margin, **extra})
        response = transport.request("GET", f"{PREFIX}/v3/steam/by-amount", params=params)
        return DgObject.from_api(response.json())

    def create_by_net_amount(
        self, *, account: str, net_amount: float, amount: float,
        description: str, order_id: str, **extra: Any
    ) -> DgObject:
        """`POST /v3/steam` — создать заказ по сумме зачисления."""

        transport = require_sync(self._transport, _PRODUCT)
        body = clean({
            "account": account,
            "netAmount": net_amount,
            "amount": amount,
            "description": description,
            "orderId": order_id,
            **extra,
        })
        response = transport.request("POST", f"{PREFIX}/v3/steam", json_body=body, mutating=True)
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    def create_by_amount(
        self, *, account: str, amount: float, margin: float,
        description: str, order_id: str, **extra: Any
    ) -> DgObject:
        """`POST /v3/steam/by-amount` — создать заказ по сумме платежа."""

        transport = require_sync(self._transport, _PRODUCT)
        body = clean({
            "account": account,
            "amount": amount,
            "margin": margin,
            "description": description,
            "orderId": order_id,
            **extra,
        })
        response = transport.request("POST", f"{PREFIX}/v3/steam/by-amount", json_body=body, mutating=True)
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    def get_order(self, order_id: str) -> DgObject:
        """`GET /v3/steam/order/{id}`."""

        transport = require_sync(self._transport, _PRODUCT)
        response = transport.request("GET", f"{PREFIX}/v3/steam/order/{order_id}")
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    # ---- оплата с депозита мерчанта ------------------------------------

    def deposit_price(self, *, account: str, net_amount: float, **extra: Any) -> DgObject:
        """`GET /v1/steam/deposit/price`."""

        transport = require_sync(self._transport, _PRODUCT)
        params = clean({"account": account, "netAmount": net_amount, **extra})
        response = transport.request("GET", f"{PREFIX}/v1/steam/deposit/price", params=params)
        return DgObject.from_api(response.json())

    def deposit_net_amount(self, *, account: str, price: float, **extra: Any) -> DgObject:
        """`GET /v1/steam/deposit/netamount`."""

        transport = require_sync(self._transport, _PRODUCT)
        params = clean({"account": account, "price": price, **extra})
        response = transport.request("GET", f"{PREFIX}/v1/steam/deposit/netamount", params=params)
        return DgObject.from_api(response.json())

    def deposit_create(
        self, *, account: str, net_amount: float, description: str, order_id: str, **extra: Any
    ) -> DgObject:
        """`POST /v1/steam/deposit` — списание с депозита по сумме зачисления."""

        transport = require_sync(self._transport, _PRODUCT)
        body = clean({
            "account": account,
            "netAmount": net_amount,
            "description": description,
            "orderId": order_id,
            **extra,
        })
        response = transport.request("POST", f"{PREFIX}/v1/steam/deposit", json_body=body, mutating=True)
        return DgObject.from_api(response.json(), status_enum=DepositOrderStatus)

    def deposit_create_by_price(
        self, *, account: str, price: float, description: str, order_id: str, **extra: Any
    ) -> DgObject:
        """`POST /v1/steam/deposit/by-price` — списание с депозита по сумме платежа."""

        transport = require_sync(self._transport, _PRODUCT)
        body = clean({
            "account": account,
            "price": price,
            "description": description,
            "orderId": order_id,
            **extra,
        })
        response = transport.request("POST", f"{PREFIX}/v1/steam/deposit/by-price", json_body=body, mutating=True)
        return DgObject.from_api(response.json(), status_enum=DepositOrderStatus)

    def deposit_order_status(self, order_id: str) -> DgObject:
        """`GET /v1/deposit/order/{orderId}` — общий статус депозитного заказа."""

        transport = require_sync(self._transport, _PRODUCT)
        return _deposit.order_status_sync(transport, order_id)

    def deposit_balance(self) -> DepositBalance:
        """`GET /v1/deposit/balance`."""

        transport = require_sync(self._transport, _PRODUCT)
        return _deposit.balance_sync(transport)


class AsyncSteamAPI:
    """Асинхронный доступ к Steam — та же логика, что и :class:`SteamAPI`."""

    def __init__(self, transport: AsyncTransport | None) -> None:
        self._transport = transport

    async def quote_by_net_amount(self, *, account: str, net_amount: float, **extra: Any) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        params = clean({"account": account, "netAmount": net_amount, **extra})
        response = await transport.request("GET", f"{PREFIX}/v3/steam/amount", params=params)
        return DgObject.from_api(response.json())

    async def quote_by_amount(self, *, account: str, amount: float, margin: float | None = None, **extra: Any) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        params = clean({"account": account, "amount": amount, "margin": margin, **extra})
        response = await transport.request("GET", f"{PREFIX}/v3/steam/by-amount", params=params)
        return DgObject.from_api(response.json())

    async def create_by_net_amount(
        self, *, account: str, net_amount: float, amount: float,
        description: str, order_id: str, **extra: Any
    ) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        body = clean({
            "account": account,
            "netAmount": net_amount,
            "amount": amount,
            "description": description,
            "orderId": order_id,
            **extra,
        })
        response = await transport.request("POST", f"{PREFIX}/v3/steam", json_body=body, mutating=True)
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    async def create_by_amount(
        self, *, account: str, amount: float, margin: float,
        description: str, order_id: str, **extra: Any
    ) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        body = clean({
            "account": account,
            "amount": amount,
            "margin": margin,
            "description": description,
            "orderId": order_id,
            **extra,
        })
        response = await transport.request("POST", f"{PREFIX}/v3/steam/by-amount", json_body=body, mutating=True)
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    async def get_order(self, order_id: str) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        response = await transport.request("GET", f"{PREFIX}/v3/steam/order/{order_id}")
        return DgObject.from_api(response.json(), status_enum=DgOrderStatus)

    async def deposit_price(self, *, account: str, net_amount: float, **extra: Any) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        params = clean({"account": account, "netAmount": net_amount, **extra})
        response = await transport.request("GET", f"{PREFIX}/v1/steam/deposit/price", params=params)
        return DgObject.from_api(response.json())

    async def deposit_net_amount(self, *, account: str, price: float, **extra: Any) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        params = clean({"account": account, "price": price, **extra})
        response = await transport.request("GET", f"{PREFIX}/v1/steam/deposit/netamount", params=params)
        return DgObject.from_api(response.json())

    async def deposit_create(
        self, *, account: str, net_amount: float, description: str, order_id: str, **extra: Any
    ) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        body = clean({
            "account": account,
            "netAmount": net_amount,
            "description": description,
            "orderId": order_id,
            **extra,
        })
        response = await transport.request("POST", f"{PREFIX}/v1/steam/deposit", json_body=body, mutating=True)
        return DgObject.from_api(response.json(), status_enum=DepositOrderStatus)

    async def deposit_create_by_price(
        self, *, account: str, price: float, description: str, order_id: str, **extra: Any
    ) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        body = clean({
            "account": account,
            "price": price,
            "description": description,
            "orderId": order_id,
            **extra,
        })
        response = await transport.request("POST", f"{PREFIX}/v1/steam/deposit/by-price", json_body=body, mutating=True)
        return DgObject.from_api(response.json(), status_enum=DepositOrderStatus)

    async def deposit_order_status(self, order_id: str) -> DgObject:
        transport = require_async(self._transport, _PRODUCT)
        return await _deposit.order_status_async(transport, order_id)

    async def deposit_balance(self) -> DepositBalance:
        transport = require_async(self._transport, _PRODUCT)
        return await _deposit.balance_async(transport)
