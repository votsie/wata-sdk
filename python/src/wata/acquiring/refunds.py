"""Возврат: `POST /api/h2h/transactions/refunds`."""

from __future__ import annotations

from .._http import AsyncTransport, SyncTransport
from ..types import RefundResult, clean
from ._common import PREFIX, require_async, require_sync


class RefundsAPI:
    def __init__(self, transport: SyncTransport | None) -> None:
        self._transport = transport

    def create(self, *, original_transaction_id: str, amount: float) -> RefundResult:
        """Создаёт возврат. Валюта берётся из исходной транзакции — отдельного
        поля нет. Изменяющий запрос — не повторяется при сбое, чтобы не создать
        второй возврат."""

        transport = require_sync(self._transport, "acquiring")
        body = clean({"originalTransactionId": original_transaction_id, "amount": amount})
        response = transport.request("POST", f"{PREFIX}/transactions/refunds", json_body=body, mutating=True)
        return RefundResult.from_api(response.json())


class AsyncRefundsAPI:
    def __init__(self, transport: AsyncTransport | None) -> None:
        self._transport = transport

    async def create(self, *, original_transaction_id: str, amount: float) -> RefundResult:
        transport = require_async(self._transport, "acquiring")
        body = clean({"originalTransactionId": original_transaction_id, "amount": amount})
        response = await transport.request("POST", f"{PREFIX}/transactions/refunds", json_body=body, mutating=True)
        return RefundResult.from_api(response.json())
