"""Обращение к продукту без токена должно падать до сетевого вызова."""

from __future__ import annotations

import pytest
import respx

from wata import WataClient
from wata.errors import WataConfigError


def test_missing_token_raises_before_network_call():
    client = WataClient(acquiring="acquiring-token")
    try:
        with respx.mock(assert_all_called=False) as mocked:
            with pytest.raises(WataConfigError, match="stars"):
                client.stars.price(username="someone")
            assert len(mocked.calls) == 0
    finally:
        client.close()


@pytest.mark.parametrize("product_attr", ["steam", "topup", "vouchers"])
def test_missing_token_names_the_missing_product(product_attr):
    client = WataClient()
    try:
        api = getattr(client, product_attr)
        with pytest.raises(WataConfigError, match=product_attr):
            api.get_order("some-id")
    finally:
        client.close()


def test_acquiring_without_token_raises_config_error():
    client = WataClient(stars="stars-token")
    try:
        with pytest.raises(WataConfigError, match="acquiring"):
            client.acquiring.links.get("id-1")
    finally:
        client.close()


def test_unknown_environment_rejected():
    with pytest.raises(WataConfigError):
        WataClient(acquiring="tok", environment="staging")


def test_sandbox_rejected_for_digital_goods_token():
    with pytest.raises(WataConfigError, match="[Пп]есочниц"):
        WataClient(steam="steam-token", environment="sandbox")


def test_sandbox_allowed_for_acquiring_only():
    client = WataClient(acquiring="tok", environment="sandbox")
    try:
        assert client.environment == "sandbox"
    finally:
        client.close()
