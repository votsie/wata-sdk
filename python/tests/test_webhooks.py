"""Проверка подписи вебхука: успех, подделка, явный ключ и авто-загрузка с кэшем."""

from __future__ import annotations

import json

import httpx
import pytest
import respx

from wata import WataClient
from wata.webhooks import parse_webhook, verify_signature

from .conftest import sign_body

BODY = json.dumps(
    {
        "transactionType": "SBP",
        "kind": "Payment",
        "id": "evt-1",
        "transactionId": "tx-1",
        "terminalPublicId": "pub-1",
        "transactionStatus": "Paid",
        "amount": 100.0,
        "currency": "RUB",
        "orderId": "order-1",
        "commission": 1.5,
    }
).encode("utf-8")


def test_verify_signature_success_with_explicit_key(rsa_keypair):
    private_key, public_pem = rsa_keypair
    signature = sign_body(private_key, BODY)
    assert verify_signature(BODY, signature, public_pem) is True


def test_verify_signature_rejects_forged_signature(rsa_keypair):
    private_key, public_pem = rsa_keypair
    tampered_body = BODY.replace(b"100.0", b"999.0")
    signature = sign_body(private_key, tampered_body)  # подпись для другого тела
    assert verify_signature(BODY, signature, public_pem) is False


def test_verify_signature_rejects_garbage_signature(rsa_keypair):
    _, public_pem = rsa_keypair
    assert verify_signature(BODY, "not-a-real-signature", public_pem) is False


def test_verify_signature_rejects_dict_body(rsa_keypair):
    _, public_pem = rsa_keypair
    with pytest.raises(TypeError):
        verify_signature({"not": "raw"}, "sig", public_pem)  # type: ignore[arg-type]


def test_parse_webhook_builds_typed_event():
    event = parse_webhook(BODY)
    assert event.id == "evt-1"
    assert event.order_id == "order-1"
    assert event.commission == 1.5
    assert event.transaction_status.value == "Paid"


def test_client_verify_webhook_fetches_and_caches_public_key(rsa_keypair):
    private_key, public_pem = rsa_keypair
    signature = sign_body(private_key, BODY)

    client = WataClient(acquiring="acquiring-token")
    try:
        with respx.mock(base_url="https://api.wata.pro") as mocked:
            route = mocked.get("/api/h2h/public-key").mock(
                return_value=httpx.Response(200, json={"value": public_pem})
            )
            assert client.verify_webhook(BODY, signature) is True
            assert route.call_count == 1
            # Второй вызов должен использовать закэшированный ключ, без нового запроса.
            assert client.verify_webhook(BODY, signature) is True
            assert route.call_count == 1
            # Проверка ключа не требует заголовка авторизации.
            assert "authorization" not in {h.lower() for h in route.calls[0].request.headers}
    finally:
        client.close()


def test_public_key_reachable_without_any_acquiring_token(rsa_keypair):
    """`GET /public-key` не требует авторизации и должен работать даже если
    клиент вообще не сконфигурирован с токеном эквайринга."""

    _, public_pem = rsa_keypair
    client = WataClient()  # ни одного продукта не сконфигурировано
    try:
        with respx.mock(base_url="https://api.wata.pro") as mocked:
            mocked.get("/api/h2h/public-key").mock(return_value=httpx.Response(200, json={"value": public_pem}))
            assert client.acquiring.get_public_key() == public_pem
    finally:
        client.close()


def test_client_verify_webhook_detects_forgery(rsa_keypair):
    private_key, public_pem = rsa_keypair
    tampered_body = BODY.replace(b"100.0", b"999.0")
    signature = sign_body(private_key, tampered_body)

    client = WataClient(acquiring="acquiring-token")
    try:
        with respx.mock(base_url="https://api.wata.pro") as mocked:
            mocked.get("/api/h2h/public-key").mock(return_value=httpx.Response(200, json={"value": public_pem}))
            assert client.verify_webhook(BODY, signature) is False
    finally:
        client.close()
