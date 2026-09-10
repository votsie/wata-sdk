"""Продукт «Эквайринг» (`/api/h2h`): ссылки, транзакции, возвраты, баланс, прямые платежи."""

from __future__ import annotations

from .._http import AsyncTransport, SyncTransport
from .balance import AsyncBalanceAPI, BalanceAPI
from .links import AsyncLinksAPI, LinksAPI
from .payments import AsyncPaymentsAPI, PaymentsAPI
from .refunds import AsyncRefundsAPI, RefundsAPI
from .transactions import AsyncTransactionsAPI, TransactionsAPI
from ._common import PREFIX

__all__ = ["Acquiring", "AsyncAcquiring"]


class Acquiring:
    """Синхронный фасад продукта «Эквайринг»."""

    def __init__(self, transport: SyncTransport | None, *, public_key_transport: SyncTransport) -> None:
        self.links = LinksAPI(transport)
        self.transactions = TransactionsAPI(transport)
        self.refunds = RefundsAPI(transport)
        self.balance = BalanceAPI(transport)
        self.payments = PaymentsAPI(transport)
        self._public_key_transport = public_key_transport

    def get_public_key(self) -> str:
        """`GET /public-key` — вызывается без заголовка авторизации."""

        response = self._public_key_transport.request("GET", f"{PREFIX}/public-key", auth=False)
        return response.json()["value"]


class AsyncAcquiring:
    """Асинхронный фасад продукта «Эквайринг»."""

    def __init__(self, transport: AsyncTransport | None, *, public_key_transport: AsyncTransport) -> None:
        self.links = AsyncLinksAPI(transport)
        self.transactions = AsyncTransactionsAPI(transport)
        self.refunds = AsyncRefundsAPI(transport)
        self.balance = AsyncBalanceAPI(transport)
        self.payments = AsyncPaymentsAPI(transport)
        self._public_key_transport = public_key_transport

    async def get_public_key(self) -> str:
        response = await self._public_key_transport.request("GET", f"{PREFIX}/public-key", auth=False)
        return response.json()["value"]
