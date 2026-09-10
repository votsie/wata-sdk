"""Прямые платежи: `card-crypto`, `sbp`, `tpay` (`POST /api/h2h/payments/...`)."""

from __future__ import annotations

from typing import Any

from .._http import AsyncTransport, SyncTransport
from ..types import CardCryptoPaymentResult, Currency, SbpPaymentResult, TPayPaymentResult, clean
from ._common import PREFIX, enum_value, require_async, require_sync


def _card_crypto_body(
    *,
    amount: float,
    currency: Currency | str,
    card_crypto: str,
    ip: str,
    return_url: str,
    device_data: dict[str, Any],
    order_id: str | None,
) -> dict[str, Any]:
    return clean(
        {
            "amount": amount,
            "currency": enum_value(currency),
            "cardCrypto": card_crypto,
            "ip": ip,
            "returnUrl": return_url,
            "deviceData": device_data,
            "orderId": order_id,
        }
    )


def _sbp_body(
    *,
    amount: float,
    ip: str,
    return_url: str,
    device_data: dict[str, Any],
    order_id: str | None,
    first_name: str | None,
    last_name: str | None,
) -> dict[str, Any]:
    return clean(
        {
            "amount": amount,
            "ip": ip,
            "returnUrl": return_url,
            "deviceData": device_data,
            "orderId": order_id,
            "firstName": first_name,
            "lastName": last_name,
        }
    )


def _tpay_body(
    *,
    amount: float,
    ip: str,
    return_url: str,
    device_data: dict[str, Any],
    order_id: str | None,
) -> dict[str, Any]:
    return clean(
        {
            "amount": amount,
            "ip": ip,
            "returnUrl": return_url,
            "deviceData": device_data,
            "orderId": order_id,
        }
    )


class PaymentsAPI:
    """Синхронные прямые платежи. Все три метода — изменяющие запросы и не
    повторяются автоматически при сбое."""

    def __init__(self, transport: SyncTransport | None) -> None:
        self._transport = transport

    def card_crypto(
        self,
        *,
        amount: float,
        currency: Currency | str,
        card_crypto: str,
        ip: str,
        return_url: str,
        device_data: dict[str, Any],
        order_id: str | None = None,
    ) -> CardCryptoPaymentResult:
        """Оплата картой по криптограмме.

        `card_crypto` формирует скрипт чекаута в браузере плательщика — сервер
        мерчанта не собирает и не видит номер карты, только передаёт готовую
        криптограмму. В ответе возможен `three_ds_data` — тогда нужно провести
        плательщика через 3-D Secure (редирект или автосабмит формы).
        """

        transport = require_sync(self._transport, "acquiring")
        body = _card_crypto_body(
            amount=amount,
            currency=currency,
            card_crypto=card_crypto,
            ip=ip,
            return_url=return_url,
            device_data=device_data,
            order_id=order_id,
        )
        response = transport.request("POST", f"{PREFIX}/payments/card-crypto", json_body=body, mutating=True)
        return CardCryptoPaymentResult.from_api(response.json())

    def sbp(
        self,
        *,
        amount: float,
        ip: str,
        return_url: str,
        device_data: dict[str, Any],
        order_id: str | None = None,
        first_name: str | None = None,
        last_name: str | None = None,
    ) -> SbpPaymentResult:
        """Оплата СБП. Валюты нет — расчёт всегда в рублях."""

        transport = require_sync(self._transport, "acquiring")
        body = _sbp_body(
            amount=amount,
            ip=ip,
            return_url=return_url,
            device_data=device_data,
            order_id=order_id,
            first_name=first_name,
            last_name=last_name,
        )
        response = transport.request("POST", f"{PREFIX}/payments/sbp", json_body=body, mutating=True)
        return SbpPaymentResult.from_api(response.json())

    def tpay(
        self,
        *,
        amount: float,
        ip: str,
        return_url: str,
        device_data: dict[str, Any],
        order_id: str | None = None,
    ) -> TPayPaymentResult:
        """Оплата T-Pay. То же, что СБП, но без `first_name`/`last_name`."""

        transport = require_sync(self._transport, "acquiring")
        body = _tpay_body(
            amount=amount,
            ip=ip,
            return_url=return_url,
            device_data=device_data,
            order_id=order_id,
        )
        response = transport.request("POST", f"{PREFIX}/payments/tpay", json_body=body, mutating=True)
        return TPayPaymentResult.from_api(response.json())


class AsyncPaymentsAPI:
    """Асинхронные прямые платежи — та же логика, что и :class:`PaymentsAPI`."""

    def __init__(self, transport: AsyncTransport | None) -> None:
        self._transport = transport

    async def card_crypto(
        self,
        *,
        amount: float,
        currency: Currency | str,
        card_crypto: str,
        ip: str,
        return_url: str,
        device_data: dict[str, Any],
        order_id: str | None = None,
    ) -> CardCryptoPaymentResult:
        transport = require_async(self._transport, "acquiring")
        body = _card_crypto_body(
            amount=amount,
            currency=currency,
            card_crypto=card_crypto,
            ip=ip,
            return_url=return_url,
            device_data=device_data,
            order_id=order_id,
        )
        response = await transport.request("POST", f"{PREFIX}/payments/card-crypto", json_body=body, mutating=True)
        return CardCryptoPaymentResult.from_api(response.json())

    async def sbp(
        self,
        *,
        amount: float,
        ip: str,
        return_url: str,
        device_data: dict[str, Any],
        order_id: str | None = None,
        first_name: str | None = None,
        last_name: str | None = None,
    ) -> SbpPaymentResult:
        transport = require_async(self._transport, "acquiring")
        body = _sbp_body(
            amount=amount,
            ip=ip,
            return_url=return_url,
            device_data=device_data,
            order_id=order_id,
            first_name=first_name,
            last_name=last_name,
        )
        response = await transport.request("POST", f"{PREFIX}/payments/sbp", json_body=body, mutating=True)
        return SbpPaymentResult.from_api(response.json())

    async def tpay(
        self,
        *,
        amount: float,
        ip: str,
        return_url: str,
        device_data: dict[str, Any],
        order_id: str | None = None,
    ) -> TPayPaymentResult:
        transport = require_async(self._transport, "acquiring")
        body = _tpay_body(
            amount=amount,
            ip=ip,
            return_url=return_url,
            device_data=device_data,
            order_id=order_id,
        )
        response = await transport.request("POST", f"{PREFIX}/payments/tpay", json_body=body, mutating=True)
        return TPayPaymentResult.from_api(response.json())
