"""Баланс терминала: `GET /api/h2h/finance/balance`."""

from __future__ import annotations

import datetime as dt

from .._http import AsyncTransport, SyncTransport
from ..types import Balance
from ._common import PREFIX, require_async, require_sync, validate_balance_date


class BalanceAPI:
    def __init__(self, transport: SyncTransport | None) -> None:
        self._transport = transport

    def get(self, date: str | dt.date) -> Balance:
        """Баланс терминала на дату. Допустимы только сегодня/вчера по UTC —
        проверяется локально, до сетевого вызова."""

        transport = require_sync(self._transport, "acquiring")
        date_str = validate_balance_date(date)
        response = transport.request("GET", f"{PREFIX}/finance/balance", params={"Date": date_str})
        return Balance.from_api(response.json())


class AsyncBalanceAPI:
    def __init__(self, transport: AsyncTransport | None) -> None:
        self._transport = transport

    async def get(self, date: str | dt.date) -> Balance:
        transport = require_async(self._transport, "acquiring")
        date_str = validate_balance_date(date)
        response = await transport.request("GET", f"{PREFIX}/finance/balance", params={"Date": date_str})
        return Balance.from_api(response.json())
