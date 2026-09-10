"""HTTP-транспорт: заголовки, ретраи, таймауты, разбор ошибок.

Общая логика (заголовки, решение о повторе, разбор ошибок) вынесена в чистые
функции и используется как синхронным, так и асинхронным транспортом, чтобы
поведение двух клиентов не расходилось.
"""

from __future__ import annotations

import random
import time
from typing import Any

import httpx

from .errors import (
    WataApiError,
    WataAuthError,
    WataNetworkError,
    WataRateLimitError,
    WataServerError,
)
from .types import clean

DEFAULT_TIMEOUT = 60.0
DEFAULT_MAX_RETRIES = 3
_RETRYABLE_STATUS = {500, 502, 503, 504}


def build_headers(token: str | None) -> dict[str, str]:
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers


def _compute_backoff(attempt: int) -> float:
    """Экспоненциальная задержка с джиттером: ``2**attempt`` секунд ± 50%."""

    base = 2**attempt
    return base * (0.5 + random.random())


def _parse_retry_after(response: httpx.Response) -> float | None:
    header = response.headers.get("Retry-After")
    if header is None:
        return None
    try:
        return float(header)
    except ValueError:
        return None


def _extract_error_body(response: httpx.Response) -> dict[str, Any]:
    try:
        body = response.json()
    except ValueError:
        return {}
    if isinstance(body, dict):
        return body
    return {}


def raise_for_response(response: httpx.Response, *, path: str) -> None:
    """Превращает ответ с ошибкой в исключение из иерархии :mod:`wata.errors`.

    Ничего не делает, если ответ успешен (``< 400``).
    """

    status = response.status_code
    if status < 400:
        return

    body = _extract_error_body(response)
    error_obj = body.get("error") if isinstance(body, dict) else None
    if not isinstance(error_obj, dict):
        error_obj = {}
    wata_code = error_obj.get("code")
    message = error_obj.get("message") or f"WATA API вернул HTTP {status}"
    details = error_obj.get("details")
    validation_errors = error_obj.get("validationErrors")

    if status in (401, 403):
        raise WataAuthError(message, http_status=status, path=path, wata_code=wata_code)
    if status == 429:
        raise WataRateLimitError(
            message,
            http_status=status,
            path=path,
            wata_code=wata_code,
            retry_after=_parse_retry_after(response),
        )
    if 400 <= status < 500:
        raise WataApiError(
            message,
            http_status=status,
            path=path,
            wata_code=wata_code,
            details=details,
            validation_errors=validation_errors,
        )
    # 5xx после исчерпания попыток повтора
    raise WataServerError(message, http_status=status, path=path, wata_code=wata_code)


def should_retry(*, attempt: int, max_retries: int, mutating: bool, status: int | None, network_error: bool) -> bool:
    if mutating:
        # Изменяющие состояние запросы по умолчанию никогда не повторяются:
        # повтор мог бы создать второй платёж/заказ/возврат.
        return False
    if attempt >= max_retries:
        return False
    if network_error:
        return True
    return status is not None and status in _RETRYABLE_STATUS


class _BaseTransport:
    def __init__(
        self,
        *,
        base_url: str,
        token: str | None,
        timeout: float = DEFAULT_TIMEOUT,
        max_retries: int = DEFAULT_MAX_RETRIES,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.token = token
        self.timeout = timeout
        self.max_retries = max_retries

    def _url(self, path: str) -> str:
        return f"{self.base_url}{path}"

    def _prepare(self, params: dict[str, Any] | None, json_body: dict[str, Any] | None) -> tuple[dict[str, Any] | None, dict[str, Any] | None]:
        clean_params = clean(params) if params else None
        clean_body = clean(json_body) if json_body else None
        return clean_params, clean_body


class SyncTransport(_BaseTransport):
    """Синхронный транспорт поверх :class:`httpx.Client`, с ретраями."""

    def __init__(self, **kwargs: Any) -> None:
        super().__init__(**kwargs)
        self._client = httpx.Client(timeout=self.timeout)

    def close(self) -> None:
        self._client.close()

    def __enter__(self) -> "SyncTransport":
        return self

    def __exit__(self, *exc: Any) -> None:
        self.close()

    def request(
        self,
        method: str,
        path: str,
        *,
        params: dict[str, Any] | None = None,
        json_body: dict[str, Any] | None = None,
        mutating: bool = False,
        auth: bool = True,
    ) -> httpx.Response:
        params, json_body = self._prepare(params, json_body)
        headers = build_headers(self.token) if auth else {"Accept": "application/json"}

        attempt = 0
        while True:
            try:
                response = self._client.request(
                    method,
                    self._url(path),
                    params=params,
                    json=json_body,
                    headers=headers,
                )
            except httpx.TimeoutException as exc:
                if should_retry(attempt=attempt, max_retries=self.max_retries, mutating=mutating, status=None, network_error=True):
                    time.sleep(_compute_backoff(attempt))
                    attempt += 1
                    continue
                raise WataNetworkError(f"Таймаут запроса: {exc}", path=path) from exc
            except httpx.TransportError as exc:
                if should_retry(attempt=attempt, max_retries=self.max_retries, mutating=mutating, status=None, network_error=True):
                    time.sleep(_compute_backoff(attempt))
                    attempt += 1
                    continue
                raise WataNetworkError(f"Сетевая ошибка: {exc}", path=path) from exc

            if response.status_code in _RETRYABLE_STATUS and should_retry(
                attempt=attempt, max_retries=self.max_retries, mutating=mutating, status=response.status_code, network_error=False
            ):
                time.sleep(_compute_backoff(attempt))
                attempt += 1
                continue

            raise_for_response(response, path=path)
            return response


class AsyncTransport(_BaseTransport):
    """Асинхронный транспорт поверх :class:`httpx.AsyncClient`, с ретраями."""

    def __init__(self, **kwargs: Any) -> None:
        super().__init__(**kwargs)
        self._client = httpx.AsyncClient(timeout=self.timeout)

    async def aclose(self) -> None:
        await self._client.aclose()

    async def __aenter__(self) -> "AsyncTransport":
        return self

    async def __aexit__(self, *exc: Any) -> None:
        await self.aclose()

    async def request(
        self,
        method: str,
        path: str,
        *,
        params: dict[str, Any] | None = None,
        json_body: dict[str, Any] | None = None,
        mutating: bool = False,
        auth: bool = True,
    ) -> httpx.Response:
        params, json_body = self._prepare(params, json_body)
        headers = build_headers(self.token) if auth else {"Accept": "application/json"}

        attempt = 0
        while True:
            try:
                response = await self._client.request(
                    method,
                    self._url(path),
                    params=params,
                    json=json_body,
                    headers=headers,
                )
            except httpx.TimeoutException as exc:
                if should_retry(attempt=attempt, max_retries=self.max_retries, mutating=mutating, status=None, network_error=True):
                    await self._async_sleep(_compute_backoff(attempt))
                    attempt += 1
                    continue
                raise WataNetworkError(f"Таймаут запроса: {exc}", path=path) from exc
            except httpx.TransportError as exc:
                if should_retry(attempt=attempt, max_retries=self.max_retries, mutating=mutating, status=None, network_error=True):
                    await self._async_sleep(_compute_backoff(attempt))
                    attempt += 1
                    continue
                raise WataNetworkError(f"Сетевая ошибка: {exc}", path=path) from exc

            if response.status_code in _RETRYABLE_STATUS and should_retry(
                attempt=attempt, max_retries=self.max_retries, mutating=mutating, status=response.status_code, network_error=False
            ):
                await self._async_sleep(_compute_backoff(attempt))
                attempt += 1
                continue

            raise_for_response(response, path=path)
            return response

    @staticmethod
    async def _async_sleep(seconds: float) -> None:
        import asyncio

        await asyncio.sleep(seconds)
