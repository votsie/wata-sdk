"""Курсорная пагинация транзакций: генератор сам переносит CursorId/CursorAmount/CursorDate."""

from __future__ import annotations

import httpx
import respx

from wata import WataClient


def test_iter_all_transactions_walks_two_pages_via_cursor():
    page1 = {
        "items": [{"id": "tx-1", "amount": 10, "currency": "RUB", "status": "Paid", "kind": "Payment"}],
        "hasNextPage": True,
        "nextCursorId": "cursor-id-1",
        "nextCursorAmount": 10,
        "nextCursorDate": "2026-01-01T00:00:00Z",
    }
    page2 = {
        "items": [{"id": "tx-2", "amount": 20, "currency": "RUB", "status": "Paid", "kind": "Payment"}],
        "hasNextPage": False,
        "nextCursorId": None,
        "nextCursorAmount": None,
        "nextCursorDate": None,
    }

    client = WataClient(acquiring="acquiring-token")
    try:
        with respx.mock(base_url="https://api.wata.pro") as mocked:
            route = mocked.get("/api/h2h/v2/transactions")
            route.side_effect = [
                httpx.Response(200, json=page1),
                httpx.Response(200, json=page2),
            ]

            items = list(client.acquiring.transactions.iter_all())

            assert [item.id for item in items] == ["tx-1", "tx-2"]
            assert route.call_count == 2

            second_call_params = dict(route.calls[1].request.url.params)
            assert second_call_params["CursorId"] == "cursor-id-1"
            assert second_call_params["CursorAmount"] == "10"
            assert second_call_params["CursorDate"] == "2026-01-01T00:00:00Z"

            first_call_params = dict(route.calls[0].request.url.params)
            assert "CursorId" not in first_call_params
    finally:
        client.close()
