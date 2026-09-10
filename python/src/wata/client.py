"""Клиенты WATA: :class:`WataClient` (синхронный, основной) и
:class:`AsyncWataClient` (асинхронный, зеркальный API).

Токен WATA выпускается на терминал, а не на аккаунт — Stars и Steam всегда
живут на отдельных терминалах и требуют собственных токенов. Клиент поэтому
принимает набор токенов по продуктам; какие-либо из них можно не передавать.
Обращение к продукту без токена бросает :class:`~wata.errors.WataConfigError`
до сетевого вызова, называя недостающий токен.
"""

from __future__ import annotations

from ._http import DEFAULT_MAX_RETRIES, DEFAULT_TIMEOUT, AsyncTransport, SyncTransport
from .acquiring import Acquiring, AsyncAcquiring
from .digital_goods import (
    AsyncStarsAPI,
    AsyncSteamAPI,
    AsyncTopupAPI,
    AsyncVouchersAPI,
    StarsAPI,
    SteamAPI,
    TopupAPI,
    VouchersAPI,
)
from .errors import WataConfigError
from .types import WebhookEvent
from .webhooks import AsyncWebhookVerifier, RawBody, WebhookVerifier

_ACQUIRING_HOSTS = {
    "production": "https://api.wata.pro",
    "sandbox": "https://api-sandbox.wata.pro",
}
_DG_HOST_PRODUCTION = "https://dg-api.wata.pro"
_ENVIRONMENTS = frozenset(_ACQUIRING_HOSTS)


def _validate_environment(environment: str) -> str:
    if environment not in _ENVIRONMENTS:
        raise WataConfigError(
            f"Неизвестное окружение {environment!r}. Допустимые значения: {sorted(_ENVIRONMENTS)}."
        )
    return environment


def _dg_base_url(environment: str, product: str) -> str:
    if environment == "sandbox":
        raise WataConfigError(
            f"Песочница недоступна для продукта цифровых товаров '{product}' — "
            "у DG API нет отдельного адреса песочницы (см. docs/SPEC.md, раздел 2). "
            "Используйте environment='production' либо не задавайте токен этого продукта."
        )
    return _DG_HOST_PRODUCTION


class WataClient:
    """Синхронный клиент WATA (основной). Смотрите также :class:`AsyncWataClient`."""

    def __init__(
        self,
        *,
        acquiring: str | None = None,
        stars: str | None = None,
        steam: str | None = None,
        topup: str | None = None,
        vouchers: str | None = None,
        environment: str = "production",
        timeout: float = DEFAULT_TIMEOUT,
        max_retries: int = DEFAULT_MAX_RETRIES,
    ) -> None:
        self.environment = _validate_environment(environment)
        acquiring_base = _ACQUIRING_HOSTS[self.environment]

        self._acquiring_transport = (
            SyncTransport(base_url=acquiring_base, token=acquiring, timeout=timeout, max_retries=max_retries)
            if acquiring
            else None
        )
        # Публичный ключ вебхука не требует токена и должен быть доступен
        # независимо от того, задан ли токен эквайринга.
        self._public_key_transport = SyncTransport(
            base_url=acquiring_base, token=None, timeout=timeout, max_retries=max_retries
        )

        self._stars_transport = self._build_dg_transport(stars, "stars", timeout, max_retries)
        self._steam_transport = self._build_dg_transport(steam, "steam", timeout, max_retries)
        self._topup_transport = self._build_dg_transport(topup, "topup", timeout, max_retries)
        self._vouchers_transport = self._build_dg_transport(vouchers, "vouchers", timeout, max_retries)

        self.acquiring = Acquiring(self._acquiring_transport, public_key_transport=self._public_key_transport)
        self.stars = StarsAPI(self._stars_transport)
        self.steam = SteamAPI(self._steam_transport)
        self.topup = TopupAPI(self._topup_transport)
        self.vouchers = VouchersAPI(self._vouchers_transport)

        self._webhooks = WebhookVerifier(self.acquiring.get_public_key)

    def _build_dg_transport(
        self, token: str | None, product: str, timeout: float, max_retries: int
    ) -> SyncTransport | None:
        if not token:
            return None
        base_url = _dg_base_url(self.environment, product)
        return SyncTransport(base_url=base_url, token=token, timeout=timeout, max_retries=max_retries)

    def verify_webhook(self, raw_body: RawBody, signature: str, key_pem: str | None = None) -> bool:
        """Проверяет подпись `X-Signature` (SHA512withRSA) по сырому телу запроса.

        Без `key_pem` ключ подтягивается через `GET /public-key` и кэшируется
        на время жизни клиента. `raw_body` обязан быть `bytes`/`str` — не
        передавайте разобранный JSON, это сломает подпись.
        """

        return self._webhooks.verify(raw_body, signature, key_pem)

    def parse_webhook(self, raw_body: RawBody) -> WebhookEvent:
        """Разбирает тело вебхука в :class:`~wata.types.WebhookEvent`.

        Подпись нужно проверить отдельно через :meth:`verify_webhook` — сам
        разбор не защищает от подделки.
        """

        return self._webhooks.parse(raw_body)

    def close(self) -> None:
        for transport in (
            self._acquiring_transport,
            self._public_key_transport,
            self._stars_transport,
            self._steam_transport,
            self._topup_transport,
            self._vouchers_transport,
        ):
            if transport is not None:
                transport.close()

    def __enter__(self) -> "WataClient":
        return self

    def __exit__(self, *exc: object) -> None:
        self.close()


class AsyncWataClient:
    """Асинхронный клиент WATA — тот же API, что и :class:`WataClient`."""

    def __init__(
        self,
        *,
        acquiring: str | None = None,
        stars: str | None = None,
        steam: str | None = None,
        topup: str | None = None,
        vouchers: str | None = None,
        environment: str = "production",
        timeout: float = DEFAULT_TIMEOUT,
        max_retries: int = DEFAULT_MAX_RETRIES,
    ) -> None:
        self.environment = _validate_environment(environment)
        acquiring_base = _ACQUIRING_HOSTS[self.environment]

        self._acquiring_transport = (
            AsyncTransport(base_url=acquiring_base, token=acquiring, timeout=timeout, max_retries=max_retries)
            if acquiring
            else None
        )
        self._public_key_transport = AsyncTransport(
            base_url=acquiring_base, token=None, timeout=timeout, max_retries=max_retries
        )

        self._stars_transport = self._build_dg_transport(stars, "stars", timeout, max_retries)
        self._steam_transport = self._build_dg_transport(steam, "steam", timeout, max_retries)
        self._topup_transport = self._build_dg_transport(topup, "topup", timeout, max_retries)
        self._vouchers_transport = self._build_dg_transport(vouchers, "vouchers", timeout, max_retries)

        self.acquiring = AsyncAcquiring(self._acquiring_transport, public_key_transport=self._public_key_transport)
        self.stars = AsyncStarsAPI(self._stars_transport)
        self.steam = AsyncSteamAPI(self._steam_transport)
        self.topup = AsyncTopupAPI(self._topup_transport)
        self.vouchers = AsyncVouchersAPI(self._vouchers_transport)

        self._webhooks = AsyncWebhookVerifier(self.acquiring.get_public_key)

    def _build_dg_transport(
        self, token: str | None, product: str, timeout: float, max_retries: int
    ) -> AsyncTransport | None:
        if not token:
            return None
        base_url = _dg_base_url(self.environment, product)
        return AsyncTransport(base_url=base_url, token=token, timeout=timeout, max_retries=max_retries)

    async def verify_webhook(self, raw_body: RawBody, signature: str, key_pem: str | None = None) -> bool:
        return await self._webhooks.verify(raw_body, signature, key_pem)

    def parse_webhook(self, raw_body: RawBody) -> WebhookEvent:
        return self._webhooks.parse(raw_body)

    async def aclose(self) -> None:
        for transport in (
            self._acquiring_transport,
            self._public_key_transport,
            self._stars_transport,
            self._steam_transport,
            self._topup_transport,
            self._vouchers_transport,
        ):
            if transport is not None:
                await transport.aclose()

    async def __aenter__(self) -> "AsyncWataClient":
        return self

    async def __aexit__(self, *exc: object) -> None:
        await self.aclose()
