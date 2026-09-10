"""Асинхронный клиент — та же логика, что и синхронный, но с await."""

from __future__ import annotations

import httpx
import pytest
import respx

from wata import AsyncWataClient
from wata.errors import WataConfigError


@pytest.mark.asyncio
async def test_async_client_creates_link():
    async with AsyncWataClient(acquiring="acquiring-token") as client:
        with respx.mock(base_url="https://api.wata.pro") as mocked:
            mocked.post("/api/h2h/links").mock(
                return_value=httpx.Response(
                    200,
                    json={"id": "1", "amount": 100, "currency": "RUB", "status": "Opened", "url": "https://pay"},
                )
            )
            link = await client.acquiring.links.create(amount=100, currency="RUB")
            assert link.url == "https://pay"


@pytest.mark.asyncio
async def test_async_client_missing_token_raises_before_network():
    async with AsyncWataClient(acquiring="acquiring-token") as client:
        with respx.mock(assert_all_called=False) as mocked:
            with pytest.raises(WataConfigError, match="steam"):
                await client.steam.get_order("order-1")
            assert len(mocked.calls) == 0


@pytest.mark.asyncio
async def test_async_client_iter_all_transactions_two_pages():
    page1 = {
        "items": [{"id": "tx-1", "amount": 10, "currency": "RUB", "status": "Paid", "kind": "Payment"}],
        "hasNextPage": True,
        "nextCursorId": "c1",
        "nextCursorAmount": 10,
        "nextCursorDate": "2026-01-01T00:00:00Z",
    }
    page2 = {
        "items": [{"id": "tx-2", "amount": 20, "currency": "RUB", "status": "Paid", "kind": "Payment"}],
        "hasNextPage": False,
    }
    async with AsyncWataClient(acquiring="acquiring-token") as client:
        with respx.mock(base_url="https://api.wata.pro") as mocked:
            route = mocked.get("/api/h2h/v2/transactions")
            route.side_effect = [httpx.Response(200, json=page1), httpx.Response(200, json=page2)]

            items = [item async for item in client.acquiring.transactions.iter_all()]
            assert [i.id for i in items] == ["tx-1", "tx-2"]
            assert route.call_count == 2
