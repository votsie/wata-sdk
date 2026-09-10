"""Структурная проверка асинхронного клиента.

Потерянный `await` перед `transport.request(...)` не ломает ни импорт, ни
типизацию: метод молча вернёт корутину вместо ответа, и ошибка всплывёт уже
у интегратора. Такое однажды случилось при массовой правке файлов, поэтому
проверка вынесена в тест.
"""

from __future__ import annotations

import ast
import pathlib

SRC = pathlib.Path(__file__).resolve().parents[1] / "src" / "wata"


def _async_request_calls_without_await() -> list[str]:
    problems: list[str] = []

    for path in SRC.rglob("*.py"):
        source = path.read_text(encoding="utf-8")
        tree = ast.parse(source)

        for node in ast.walk(tree):
            if not isinstance(node, ast.AsyncFunctionDef):
                continue

            segment = ast.get_source_segment(source, node) or ""
            for line in segment.splitlines():
                stripped = line.strip()
                if ".request(" not in stripped:
                    continue
                if stripped.startswith(("def ", "async def ")):
                    continue
                if "await" in stripped:
                    continue
                problems.append(f"{path.name}:{node.name}: {stripped[:80]}")

    return problems


def test_async_methods_always_await_transport() -> None:
    problems = _async_request_calls_without_await()
    assert not problems, "вызовы transport.request без await в async-методах:\n" + "\n".join(problems)


def test_sync_and_async_apis_expose_same_methods() -> None:
    """Асинхронный клиент не должен отставать от синхронного по составу методов."""

    from wata.digital_goods.stars import AsyncStarsAPI, StarsAPI
    from wata.digital_goods.steam import AsyncSteamAPI, SteamAPI
    from wata.digital_goods.topup import AsyncTopupAPI, TopupAPI
    from wata.digital_goods.vouchers import AsyncVouchersAPI, VouchersAPI

    pairs = [
        (StarsAPI, AsyncStarsAPI),
        (SteamAPI, AsyncSteamAPI),
        (TopupAPI, AsyncTopupAPI),
        (VouchersAPI, AsyncVouchersAPI),
    ]

    for sync_cls, async_cls in pairs:
        sync_methods = {n for n in vars(sync_cls) if not n.startswith("_")}
        async_methods = {n for n in vars(async_cls) if not n.startswith("_")}
        missing = sync_methods - async_methods
        assert not missing, f"{async_cls.__name__} не хватает методов: {sorted(missing)}"
