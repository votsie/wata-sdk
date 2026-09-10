"""Баланс доступен только за сегодня/вчера по UTC — проверяется локально."""

from __future__ import annotations

import datetime as dt

import httpx
import pytest
import respx

from wata import WataClient
from wata.errors import WataConfigError


def test_balance_rejects_date_other_than_today_or_yesterday():
    client = WataClient(acquiring="acquiring-token")
    try:
        too_old = (dt.datetime.now(dt.timezone.utc).date() - dt.timedelta(days=5)).isoformat()
        with respx.mock(assert_all_called=False) as mocked:
            with pytest.raises(WataConfigError):
                client.acquiring.balance.get(too_old)
            assert len(mocked.calls) == 0
    finally:
        client.close()


def test_balance_accepts_today_and_yesterday():
    client = WataClient(acquiring="acquiring-token")
    try:
        today = dt.datetime.now(dt.timezone.utc).date()
        yesterday = today - dt.timedelta(days=1)
        with respx.mock(base_url="https://api.wata.pro") as mocked:
            mocked.get("/api/h2h/finance/balance").mock(
                return_value=httpx.Response(
                    200,
                    json={"terminalPublicId": "pub-1", "date": today.isoformat(), "balance": 100.0, "currency": "RUB"},
                )
            )
            balance_today = client.acquiring.balance.get(today)
            balance_yesterday = client.acquiring.balance.get(yesterday.isoformat())
            assert balance_today.balance == 100.0
            assert balance_yesterday.terminal_public_id == "pub-1"
    finally:
        client.close()
