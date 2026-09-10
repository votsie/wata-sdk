"""Проверка подписи и разбор вебхуков WATA.

- Заголовок подписи: `X-Signature`, значение в base64.
- Алгоритм: **SHA512withRSA** (RSA PKCS#1 v1.5 + SHA-512) — не SHA-256.
- Ключ: `GET /api/h2h/public-key`, поле `value`, PEM. У боевого контура и
  песочницы разные ключи — кэш привязан к клиенту (а значит, к окружению).
- Проверяется **сырое тело запроса**, до разбора JSON: тело обязано быть
  `bytes` или `str`, а не разобранным объектом — пересборка JSON меняет
  порядок ключей и ломает подпись.

Обработчик вебхука обязан вернуть HTTP 200 и быть идемпотентным — это
ответственность приложения, а не SDK.
"""

from __future__ import annotations

import base64
import json
from typing import Any, Awaitable, Callable

from .errors import WataConfigError, WataWebhookError
from .types import WebhookEvent

RawBody = bytes | str


def _ensure_raw(raw_body: Any) -> bytes:
    """Проверяет, что тело — байты/строка (не разобранный JSON), и кодирует в bytes."""

    if isinstance(raw_body, bytes):
        return raw_body
    if isinstance(raw_body, str):
        return raw_body.encode("utf-8")
    raise TypeError(
        "raw_body должен быть bytes или str (сырое тело запроса), а не "
        f"{type(raw_body).__name__}. Разобранный JSON меняет порядок ключей и ломает подпись."
    )


def _load_cryptography() -> tuple[Any, Any, Any, type[Exception]]:
    try:
        from cryptography.exceptions import InvalidSignature
        from cryptography.hazmat.primitives import hashes, serialization
        from cryptography.hazmat.primitives.asymmetric import padding
    except ImportError as exc:  # pragma: no cover - зависит от окружения установки
        raise WataConfigError(
            "Проверка подписи вебхука требует пакет 'cryptography'. "
            "Установите его через extra: pip install wata[webhooks]"
        ) from exc
    return hashes, serialization, padding, InvalidSignature


def verify_signature(raw_body: RawBody, signature: str, key_pem: str) -> bool:
    """Проверяет подпись SHA512withRSA по сырому телу и явно заданному PEM-ключу.

    Не обращается к сети. Для автоматической загрузки и кэширования ключа по
    окружению используйте :meth:`WataClient.verify_webhook` без `key_pem`.
    """

    body_bytes = _ensure_raw(raw_body)
    hashes_mod, serialization, padding, InvalidSignature = _load_cryptography()

    try:
        public_key = serialization.load_pem_public_key(key_pem.encode("utf-8"))
    except Exception as exc:
        raise WataWebhookError(f"Не удалось разобрать публичный ключ вебхука: {exc}") from exc

    try:
        signature_bytes = base64.b64decode(signature, validate=True)
    except Exception:
        # Некорректный base64 — такая подпись заведомо не может совпасть с
        # ожидаемой, это не сходится с точки зрения проверки, а не сбой
        # инфраструктуры. Возвращаем False, как и при несовпадении подписи.
        return False

    try:
        public_key.verify(signature_bytes, body_bytes, padding.PKCS1v15(), hashes_mod.SHA512())
        return True
    except InvalidSignature:
        return False


def parse_webhook(raw_body: RawBody) -> WebhookEvent:
    """Разбирает событие вебхука. Подпись нужно проверить отдельно, до вызова
    этой функции — сама по себе она не является защитой от подделки."""

    body_bytes = _ensure_raw(raw_body)
    try:
        data = json.loads(body_bytes.decode("utf-8"))
    except (ValueError, UnicodeDecodeError) as exc:
        raise WataWebhookError(f"Тело вебхука не является корректным JSON: {exc}") from exc
    if not isinstance(data, dict):
        raise WataWebhookError("Тело вебхука должно быть JSON-объектом")
    return WebhookEvent.from_api(data)


class WebhookVerifier:
    """Синхронная проверка подписи с кэшированием публичного ключа."""

    def __init__(self, fetch_public_key: Callable[[], str]) -> None:
        self._fetch_public_key = fetch_public_key
        self._cached_key: str | None = None

    def verify(self, raw_body: RawBody, signature: str, key_pem: str | None = None) -> bool:
        if key_pem is not None:
            return verify_signature(raw_body, signature, key_pem)
        if self._cached_key is None:
            self._cached_key = self._fetch_public_key()
        try:
            return verify_signature(raw_body, signature, self._cached_key)
        except WataWebhookError:
            # Ключ мог быть отозван/обновлён — обновляем кэш один раз и пробуем снова.
            self._cached_key = self._fetch_public_key()
            return verify_signature(raw_body, signature, self._cached_key)

    @staticmethod
    def parse(raw_body: RawBody) -> WebhookEvent:
        return parse_webhook(raw_body)


class AsyncWebhookVerifier:
    """Асинхронная проверка подписи с кэшированием публичного ключа."""

    def __init__(self, fetch_public_key: Callable[[], Awaitable[str]]) -> None:
        self._fetch_public_key = fetch_public_key
        self._cached_key: str | None = None

    async def verify(self, raw_body: RawBody, signature: str, key_pem: str | None = None) -> bool:
        if key_pem is not None:
            return verify_signature(raw_body, signature, key_pem)
        if self._cached_key is None:
            self._cached_key = await self._fetch_public_key()
        try:
            return verify_signature(raw_body, signature, self._cached_key)
        except WataWebhookError:
            self._cached_key = await self._fetch_public_key()
            return verify_signature(raw_body, signature, self._cached_key)

    @staticmethod
    def parse(raw_body: RawBody) -> WebhookEvent:
        return parse_webhook(raw_body)
