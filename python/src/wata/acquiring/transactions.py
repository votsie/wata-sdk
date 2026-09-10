"""Транзакции: `GET /api/h2h/v2/transactions` (курсорная пагинация), `GET /api/h2h/transactions/{id}`."""

from __future__ import annotations

from typing import Any, AsyncIterator, Iterable, Iterator

from .._http import AsyncTransport, SyncTransport
from ..types import (
    Currency,
    Transaction,
    TransactionPage,
    TransactionStatus,
    clean,
)
from ._common import PREFIX, enum_value, require_async, require_sync


def _search_params(
    *,
    order_id: str | None,
    creation_time_from: str | None,
    creation_time_to: str | None,
    amount_from: float | None,
    amount_to: float | None,
    currencies: Iterable[Currency | str] | None,
    payment_link_ids: Iterable[str] | None,
    statuses: Iterable[TransactionStatus | str] | None,
    sorting: str | None,
    max_result_count: int | None,
    cursor_id: str | None,
    cursor_amount: float | None,
    cursor_date: str | None,
) -> dict[str, Any]:
    return clean(
        {
            "OrderId": order_id,
            "CreationTimeFrom": creation_time_from,
            "CreationTimeTo": creation_time_to,
            "AmountFrom": amount_from,
            "AmountTo": amount_to,
            "Currencies": [enum_value(c) for c in currencies] if currencies else None,
            "PaymentLinkIds": list(payment_link_ids) if payment_link_ids else None,
            "Statuses": [enum_value(s) for s in statuses] if statuses else None,
            "Sorting": sorting,
            "MaxResultCount": max_result_count,
            "CursorId": cursor_id,
            "CursorAmount": cursor_amount,
            "CursorDate": cursor_date,
        }
    )


class TransactionsAPI:
    """Синхронный доступ к транзакциям."""

    def __init__(self, transport: SyncTransport | None) -> None:
        self._transport = transport

    def search_page(
        self,
        *,
        order_id: str | None = None,
        creation_time_from: str | None = None,
        creation_time_to: str | None = None,
        amount_from: float | None = None,
        amount_to: float | None = None,
        currencies: Iterable[Currency | str] | None = None,
        payment_link_ids: Iterable[str] | None = None,
        statuses: Iterable[TransactionStatus | str] | None = None,
        sorting: str | None = None,
        max_result_count: int | None = None,
        cursor_id: str | None = None,
        cursor_amount: float | None = None,
        cursor_date: str | None = None,
    ) -> TransactionPage:
        """Одна страница поиска транзакций (`GET /v2/transactions`).

        Для перебора всех страниц предпочтительнее :meth:`iter_all` — он сам
        переносит курсорные поля между запросами.
        """

        transport = require_sync(self._transport, "acquiring")
        params = _search_params(
            order_id=order_id,
            creation_time_from=creation_time_from,
            creation_time_to=creation_time_to,
            amount_from=amount_from,
            amount_to=amount_to,
            currencies=currencies,
            payment_link_ids=payment_link_ids,
            statuses=statuses,
            sorting=sorting,
            max_result_count=max_result_count,
            cursor_id=cursor_id,
            cursor_amount=cursor_amount,
            cursor_date=cursor_date,
        )
        response = transport.request("GET", f"{PREFIX}/v2/transactions", params=params)
        return TransactionPage.from_api(response.json())

    def iter_all(
        self,
        *,
        order_id: str | None = None,
        creation_time_from: str | None = None,
        creation_time_to: str | None = None,
        amount_from: float | None = None,
        amount_to: float | None = None,
        currencies: Iterable[Currency | str] | None = None,
        payment_link_ids: Iterable[str] | None = None,
        statuses: Iterable[TransactionStatus | str] | None = None,
        sorting: str | None = None,
        max_result_count: int | None = None,
    ) -> Iterator[Transaction]:
        """Генератор по всем страницам транзакций.

        Сам переносит `CursorId`/`CursorAmount`/`CursorDate` между запросами —
        ручное листание пропускает `CursorDate` и ломает вторую страницу.
        """

        cursor_id: str | None = None
        cursor_amount: float | None = None
        cursor_date: str | None = None
        while True:
            page = self.search_page(
                order_id=order_id,
                creation_time_from=creation_time_from,
                creation_time_to=creation_time_to,
                amount_from=amount_from,
                amount_to=amount_to,
                currencies=currencies,
                payment_link_ids=payment_link_ids,
                statuses=statuses,
                sorting=sorting,
                max_result_count=max_result_count,
                cursor_id=cursor_id,
                cursor_amount=cursor_amount,
                cursor_date=cursor_date,
            )
            yield from page.items
            if not page.has_next_page:
                return
            cursor_id = page.next_cursor_id
            cursor_amount = page.next_cursor_amount
            cursor_date = page.next_cursor_date

    def get(self, transaction_id: str) -> Transaction:
        """Транзакция по идентификатору (`GET /transactions/{id}`)."""

        transport = require_sync(self._transport, "acquiring")
        response = transport.request("GET", f"{PREFIX}/transactions/{transaction_id}")
        return Transaction.from_api(response.json())


class AsyncTransactionsAPI:
    """Асинхронный доступ к транзакциям — та же логика, что и :class:`TransactionsAPI`."""

    def __init__(self, transport: AsyncTransport | None) -> None:
        self._transport = transport

    async def search_page(
        self,
        *,
        order_id: str | None = None,
        creation_time_from: str | None = None,
        creation_time_to: str | None = None,
        amount_from: float | None = None,
        amount_to: float | None = None,
        currencies: Iterable[Currency | str] | None = None,
        payment_link_ids: Iterable[str] | None = None,
        statuses: Iterable[TransactionStatus | str] | None = None,
        sorting: str | None = None,
        max_result_count: int | None = None,
        cursor_id: str | None = None,
        cursor_amount: float | None = None,
        cursor_date: str | None = None,
    ) -> TransactionPage:
        transport = require_async(self._transport, "acquiring")
        params = _search_params(
            order_id=order_id,
            creation_time_from=creation_time_from,
            creation_time_to=creation_time_to,
            amount_from=amount_from,
            amount_to=amount_to,
            currencies=currencies,
            payment_link_ids=payment_link_ids,
            statuses=statuses,
            sorting=sorting,
            max_result_count=max_result_count,
            cursor_id=cursor_id,
            cursor_amount=cursor_amount,
            cursor_date=cursor_date,
        )
        response = await transport.request("GET", f"{PREFIX}/v2/transactions", params=params)
        return TransactionPage.from_api(response.json())

    async def iter_all(
        self,
        *,
        order_id: str | None = None,
        creation_time_from: str | None = None,
        creation_time_to: str | None = None,
        amount_from: float | None = None,
        amount_to: float | None = None,
        currencies: Iterable[Currency | str] | None = None,
        payment_link_ids: Iterable[str] | None = None,
        statuses: Iterable[TransactionStatus | str] | None = None,
        sorting: str | None = None,
        max_result_count: int | None = None,
    ) -> AsyncIterator[Transaction]:
        cursor_id: str | None = None
        cursor_amount: float | None = None
        cursor_date: str | None = None
        while True:
            page = await self.search_page(
                order_id=order_id,
                creation_time_from=creation_time_from,
                creation_time_to=creation_time_to,
                amount_from=amount_from,
                amount_to=amount_to,
                currencies=currencies,
                payment_link_ids=payment_link_ids,
                statuses=statuses,
                sorting=sorting,
                max_result_count=max_result_count,
                cursor_id=cursor_id,
                cursor_amount=cursor_amount,
                cursor_date=cursor_date,
            )
            for item in page.items:
                yield item
            if not page.has_next_page:
                return
            cursor_id = page.next_cursor_id
            cursor_amount = page.next_cursor_amount
            cursor_date = page.next_cursor_date

    async def get(self, transaction_id: str) -> Transaction:
        transport = require_async(self._transport, "acquiring")
        response = await transport.request("GET", f"{PREFIX}/transactions/{transaction_id}")
        return Transaction.from_api(response.json())
