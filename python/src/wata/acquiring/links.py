"""Платёжные ссылки: `POST/GET /api/h2h/links`, `GET /api/h2h/links/{id}`."""

from __future__ import annotations

from typing import Any, Iterable

from .._http import AsyncTransport, SyncTransport
from ..types import (
    Currency,
    PaymentLink,
    PaymentLinkPage,
    PaymentLinkStatus,
    PaymentLinkType,
    Subscription,
    clean,
    model_to_api,
)
from ._common import PREFIX, enum_value, require_async, require_sync


def _create_body(
    *,
    amount: float,
    currency: Currency | str,
    description: str | None,
    order_id: str | None,
    success_redirect_url: str | None,
    fail_redirect_url: str | None,
    expiration_date_time: str | None,
    type: PaymentLinkType | str | None,
    is_arbitrary_amount_allowed: bool | None,
    arbitrary_amount_prompts: list[float] | None,
    email: str | None,
    phone: str | None,
    username: str | None,
    user_id: str | None,
    subscription: Subscription | None,
) -> dict[str, Any]:
    return clean(
        {
            "amount": amount,
            "currency": enum_value(currency),
            "description": description,
            "orderId": order_id,
            "successRedirectUrl": success_redirect_url,
            "failRedirectUrl": fail_redirect_url,
            "expirationDateTime": expiration_date_time,
            "type": enum_value(type),
            "isArbitraryAmountAllowed": is_arbitrary_amount_allowed,
            "arbitraryAmountPrompts": arbitrary_amount_prompts,
            "email": email,
            "phone": phone,
            "username": username,
            "userId": user_id,
            "subscription": model_to_api(subscription) if subscription is not None else None,
        }
    )


def _search_params(
    *,
    order_id: str | None,
    creation_time_from: str | None,
    creation_time_to: str | None,
    amount_from: float | None,
    amount_to: float | None,
    currencies: Iterable[Currency | str] | None,
    statuses: Iterable[PaymentLinkStatus | str] | None,
    sorting: str | None,
    skip_count: int | None,
    max_result_count: int | None,
) -> dict[str, Any]:
    return clean(
        {
            "OrderId": order_id,
            "CreationTimeFrom": creation_time_from,
            "CreationTimeTo": creation_time_to,
            "AmountFrom": amount_from,
            "AmountTo": amount_to,
            "Currencies": [enum_value(c) for c in currencies] if currencies else None,
            "Statuses": [enum_value(s) for s in statuses] if statuses else None,
            "Sorting": sorting,
            "SkipCount": skip_count,
            "MaxResultCount": max_result_count,
        }
    )


class LinksAPI:
    """Синхронный доступ к платёжным ссылкам."""

    def __init__(self, transport: SyncTransport | None) -> None:
        self._transport = transport

    def create(
        self,
        *,
        amount: float,
        currency: Currency | str,
        description: str | None = None,
        order_id: str | None = None,
        success_redirect_url: str | None = None,
        fail_redirect_url: str | None = None,
        expiration_date_time: str | None = None,
        type: PaymentLinkType | str | None = None,
        is_arbitrary_amount_allowed: bool | None = None,
        arbitrary_amount_prompts: list[float] | None = None,
        email: str | None = None,
        phone: str | None = None,
        username: str | None = None,
        user_id: str | None = None,
        subscription: Subscription | None = None,
    ) -> PaymentLink:
        """Создаёт платёжную ссылку (`POST /links`). Изменяющий запрос — не повторяется при сбое."""

        transport = require_sync(self._transport, "acquiring")
        body = _create_body(
            amount=amount,
            currency=currency,
            description=description,
            order_id=order_id,
            success_redirect_url=success_redirect_url,
            fail_redirect_url=fail_redirect_url,
            expiration_date_time=expiration_date_time,
            type=type,
            is_arbitrary_amount_allowed=is_arbitrary_amount_allowed,
            arbitrary_amount_prompts=arbitrary_amount_prompts,
            email=email,
            phone=phone,
            username=username,
            user_id=user_id,
            subscription=subscription,
        )
        response = transport.request("POST", f"{PREFIX}/links", json_body=body, mutating=True)
        return PaymentLink.from_api(response.json())

    def search(
        self,
        *,
        order_id: str | None = None,
        creation_time_from: str | None = None,
        creation_time_to: str | None = None,
        amount_from: float | None = None,
        amount_to: float | None = None,
        currencies: Iterable[Currency | str] | None = None,
        statuses: Iterable[PaymentLinkStatus | str] | None = None,
        sorting: str | None = None,
        skip_count: int | None = None,
        max_result_count: int | None = None,
    ) -> PaymentLinkPage:
        """Постраничный поиск ссылок (`GET /links`, `SkipCount`/`MaxResultCount`)."""

        transport = require_sync(self._transport, "acquiring")
        params = _search_params(
            order_id=order_id,
            creation_time_from=creation_time_from,
            creation_time_to=creation_time_to,
            amount_from=amount_from,
            amount_to=amount_to,
            currencies=currencies,
            statuses=statuses,
            sorting=sorting,
            skip_count=skip_count,
            max_result_count=max_result_count,
        )
        response = transport.request("GET", f"{PREFIX}/links", params=params)
        return PaymentLinkPage.from_api(response.json())

    def get(self, link_id: str) -> PaymentLink:
        """Ссылка по идентификатору (`GET /links/{id}`)."""

        transport = require_sync(self._transport, "acquiring")
        response = transport.request("GET", f"{PREFIX}/links/{link_id}")
        return PaymentLink.from_api(response.json())


class AsyncLinksAPI:
    """Асинхронный доступ к платёжным ссылкам — та же логика, что и :class:`LinksAPI`."""

    def __init__(self, transport: AsyncTransport | None) -> None:
        self._transport = transport

    async def create(
        self,
        *,
        amount: float,
        currency: Currency | str,
        description: str | None = None,
        order_id: str | None = None,
        success_redirect_url: str | None = None,
        fail_redirect_url: str | None = None,
        expiration_date_time: str | None = None,
        type: PaymentLinkType | str | None = None,
        is_arbitrary_amount_allowed: bool | None = None,
        arbitrary_amount_prompts: list[float] | None = None,
        email: str | None = None,
        phone: str | None = None,
        username: str | None = None,
        user_id: str | None = None,
        subscription: Subscription | None = None,
    ) -> PaymentLink:
        transport = require_async(self._transport, "acquiring")
        body = _create_body(
            amount=amount,
            currency=currency,
            description=description,
            order_id=order_id,
            success_redirect_url=success_redirect_url,
            fail_redirect_url=fail_redirect_url,
            expiration_date_time=expiration_date_time,
            type=type,
            is_arbitrary_amount_allowed=is_arbitrary_amount_allowed,
            arbitrary_amount_prompts=arbitrary_amount_prompts,
            email=email,
            phone=phone,
            username=username,
            user_id=user_id,
            subscription=subscription,
        )
        response = await transport.request("POST", f"{PREFIX}/links", json_body=body, mutating=True)
        return PaymentLink.from_api(response.json())

    async def search(
        self,
        *,
        order_id: str | None = None,
        creation_time_from: str | None = None,
        creation_time_to: str | None = None,
        amount_from: float | None = None,
        amount_to: float | None = None,
        currencies: Iterable[Currency | str] | None = None,
        statuses: Iterable[PaymentLinkStatus | str] | None = None,
        sorting: str | None = None,
        skip_count: int | None = None,
        max_result_count: int | None = None,
    ) -> PaymentLinkPage:
        transport = require_async(self._transport, "acquiring")
        params = _search_params(
            order_id=order_id,
            creation_time_from=creation_time_from,
            creation_time_to=creation_time_to,
            amount_from=amount_from,
            amount_to=amount_to,
            currencies=currencies,
            statuses=statuses,
            sorting=sorting,
            skip_count=skip_count,
            max_result_count=max_result_count,
        )
        response = await transport.request("GET", f"{PREFIX}/links", params=params)
        return PaymentLinkPage.from_api(response.json())

    async def get(self, link_id: str) -> PaymentLink:
        transport = require_async(self._transport, "acquiring")
        response = await transport.request("GET", f"{PREFIX}/links/{link_id}")
        return PaymentLink.from_api(response.json())
