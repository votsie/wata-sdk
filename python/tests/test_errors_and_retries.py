"""Разбор ошибки с кодом WATA, отказ от повтора изменяющего запроса, ретраи для GET."""

from __future__ import annotations

import httpx
import pytest
import respx

from wata import WataClient
from wata.errors import WataApiError, WataAuthError, WataServerError


def test_api_error_carries_wata_code_message_and_status():
    client = WataClient(acquiring="acquiring-token")
    try:
        with respx.mock(base_url="https://api.wata.pro") as mocked:
            mocked.post("/api/h2h/transactions/refunds").mock(
                return_value=httpx.Response(
                    400,
                    json={
                        "error": {
                            "code": "TRA_NotEnoughFunds",
                            "message": "Недостаточно средств для возврата",
                            "details": None,
                            "validationErrors": None,
                        }
                    },
                )
            )
            with pytest.raises(WataApiError) as exc_info:
                client.acquiring.refunds.create(original_transaction_id="tx-1", amount=10)

            err = exc_info.value
            assert err.wata_code == "TRA_NotEnoughFunds"
            assert err.http_status == 400
            assert "Недостаточно средств" in err.message
            assert err.path == "/api/h2h/transactions/refunds"
    finally:
        client.close()


def test_auth_error_on_401():
    client = WataClient(acquiring="bad-token")
    try:
        with respx.mock(base_url="https://api.wata.pro") as mocked:
            mocked.get("/api/h2h/links/1").mock(return_value=httpx.Response(401, json={"error": {"message": "no"}}))
            with pytest.raises(WataAuthError):
                client.acquiring.links.get("1")
    finally:
        client.close()


def test_mutating_request_is_not_retried_on_server_error():
    """Создание платежа/заказа/возврата не должно повторяться при 5xx —
    повтор мог бы создать второй платёж."""

    client = WataClient(acquiring="acquiring-token")
    try:
        with respx.mock(base_url="https://api.wata.pro") as mocked:
            route = mocked.post("/api/h2h/links").mock(return_value=httpx.Response(500, json={}))
            with pytest.raises(WataServerError):
                client.acquiring.links.create(amount=100, currency="RUB")
            assert route.call_count == 1
    finally:
        client.close()


def test_non_mutating_request_is_retried_on_server_error_then_succeeds(monkeypatch):
    monkeypatch.setattr("wata._http.time.sleep", lambda _seconds: None)
    client = WataClient(acquiring="acquiring-token", max_retries=3)
    try:
        with respx.mock(base_url="https://api.wata.pro") as mocked:
            route = mocked.get("/api/h2h/links/1")
            route.side_effect = [
                httpx.Response(500, json={}),
                httpx.Response(200, json={"id": "1", "amount": 10, "currency": "RUB", "status": "Opened", "url": "u"}),
            ]
            link = client.acquiring.links.get("1")
            assert link.id == "1"
            assert route.call_count == 2
    finally:
        client.close()
